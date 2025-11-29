package com.example.sprint_2_kotlin.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sprint_2_kotlin.model.data.*
import utils.NetworkMonitor
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class BookmarkViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val bookmarkDao = AppDatabase.getDatabase(application).bookmarkDao()
    private val networkMonitor = NetworkMonitor(application)

    // Supabase client (same configuration as Repository)
    private val supabaseClient = createSupabaseClient(
        supabaseUrl = "https://oikdnxujjmkbewdhpyor.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9pa2RueHVqam1rYmV3ZGhweW9yIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTk0MDU0MjksImV4cCI6MjA3NDk4MTQyOX0.htw3cdc-wFcBjKKPP4aEC9K9xBEnvPULMToP_PIuaLI"
    ) {
        install(Postgrest)
        install(Auth)
    }

    // UI State
    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkDao
        .getAllBookmarks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val bookmarkCount: StateFlow<Int> = bookmarkDao
        .getBookmarkCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val pendingSyncCount: StateFlow<Int> = bookmarkDao
        .getPendingOperationsCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Network connectivity
    val isConnected: StateFlow<Boolean> = networkMonitor.isConnected
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        // Auto-sync when connection is restored
        viewModelScope.launch {
            isConnected
                .filter { it } // Only when connected
                .collect {
                    syncPendingOperations()
                }
        }
    }

    // ========== USER AUTHENTICATION ==========

    /**
     * Get current user's profile ID from Supabase
     * Same logic as Repository.kt
     */
    private suspend fun getCurrentUserProfileId(): Int? {
        return try {
            val user = supabaseClient.auth.currentUserOrNull()?.id
            if (user == null) {
                Log.w(TAG, "No authenticated user found")
                return null
            }

            val response = supabaseClient.from("user_profiles").select {
                filter {
                    eq("user_auth_id", user)
                }
            }
            val profiles = response.decodeList<UserProfile>()

            if (profiles.isEmpty()) {
                Log.w(TAG, "No user profile found for auth_id: $user")
                return null
            }

            val userProfileId = profiles.first().user_profile_id
            Log.d(TAG, "Current user profile ID: $userProfileId")
            userProfileId

        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile ID", e)
            null
        }
    }

    // ========== LOCAL-FIRST OPERATIONS ==========

    /**
     * Check if a news item is bookmarked
     */
    suspend fun isBookmarked(newsItemId: Int): Boolean = withContext(Dispatchers.IO) {
        bookmarkDao.isBookmarked(newsItemId)
    }

    /**
     * Toggle bookmark (Local-First + Eventual Sync)
     */
    fun toggleBookmark(newsItem: NewsItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isCurrentlyBookmarked = bookmarkDao.isBookmarked(newsItem.news_item_id)

                if (isCurrentlyBookmarked) {
                    removeBookmark(newsItem.news_item_id)
                } else {
                    addBookmark(newsItem)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling bookmark", e)
            }
        }
    }

    /**
     * Add bookmark (Local-First)
     */
    private suspend fun addBookmark(newsItem: NewsItem) {
        val bookmark = newsItem.toBookmarkEntity()

        val syncOperation = BookmarkSyncQueueEntity(
            newsItemId = newsItem.news_item_id,
            operationType = BookmarkSyncQueueEntity.OperationType.ADD
        )

        // Save locally FIRST (Local-First principle)
        bookmarkDao.addBookmarkWithSync(bookmark, syncOperation)

        // Try to sync immediately if connected
        if (isConnected.value) {
            syncSingleOperation(syncOperation)
        }
    }

    /**
     * Remove bookmark (Local-First)
     */
    private suspend fun removeBookmark(newsItemId: Int) {
        val syncOperation = BookmarkSyncQueueEntity(
            newsItemId = newsItemId,
            operationType = BookmarkSyncQueueEntity.OperationType.REMOVE
        )

        // Remove locally FIRST (Local-First principle)
        bookmarkDao.removeBookmarkWithSync(newsItemId, syncOperation)

        // Try to sync immediately if connected
        if (isConnected.value) {
            syncSingleOperation(syncOperation)
        }
    }

    /**
     * Clear all bookmarks
     */
    fun clearAllBookmarks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get all bookmarks
                val allBookmarks = bookmarks.value

                // Create remove operations for each
                allBookmarks.forEach { bookmark ->
                    val syncOperation = BookmarkSyncQueueEntity(
                        newsItemId = bookmark.newsItemId,
                        operationType = BookmarkSyncQueueEntity.OperationType.REMOVE
                    )
                    bookmarkDao.insertSyncOperation(syncOperation)
                }

                // Clear locally
                bookmarkDao.clearAllBookmarks()

                // Sync if connected
                if (isConnected.value) {
                    syncPendingOperations()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing bookmarks", e)
            }
        }
    }

    // ========== EVENTUAL CONNECTIVITY ==========

    /**
     * Sync all pending operations
     */
    fun syncPendingOperations() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!isConnected.value) {
                Log.d(TAG, "No connection, skipping sync")
                return@launch
            }

            _syncStatus.value = SyncStatus.Syncing

            try {
                val pendingOps = bookmarkDao.getPendingSyncOperations()
                Log.d(TAG, "Syncing ${pendingOps.size} pending operations")

                pendingOps.forEach { operation ->
                    syncSingleOperation(operation)
                }

                // Clean up completed operations
                bookmarkDao.deleteCompletedOperations()

                _syncStatus.value = SyncStatus.Success
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing operations", e)
                _syncStatus.value = SyncStatus.Error(e.message ?: "Sync failed")
            }
        }
    }

    /**
     * Sync a single operation
     */
    private suspend fun syncSingleOperation(operation: BookmarkSyncQueueEntity) {
        try {
            // Update to IN_PROGRESS
            bookmarkDao.updateSyncOperation(
                operation.copy(
                    status = BookmarkSyncQueueEntity.SyncStatus.IN_PROGRESS,
                    lastAttempt = System.currentTimeMillis()
                )
            )

            when (operation.operationType) {
                BookmarkSyncQueueEntity.OperationType.ADD -> {
                    syncAddToSupabase(operation.newsItemId)
                }
                BookmarkSyncQueueEntity.OperationType.REMOVE -> {
                    syncRemoveFromSupabase(operation.newsItemId)
                }
            }

            // Mark as completed
            bookmarkDao.updateSyncOperation(
                operation.copy(status = BookmarkSyncQueueEntity.SyncStatus.COMPLETED)
            )

            // Update bookmark sync status
            bookmarkDao.updateSyncStatus(
                operation.newsItemId,
                synced = true,
                timestamp = System.currentTimeMillis()
            )

            Log.d(TAG, "Synced operation: ${operation.operationType} for ${operation.newsItemId}")

        } catch (e: Exception) {
            Log.e(TAG, "Error syncing operation", e)

            // Mark as failed with retry logic
            val maxRetries = 5
            if (operation.retryCount >= maxRetries) {
                Log.e(TAG, "Max retries reached for operation ${operation.id}")
                bookmarkDao.deleteSyncOperation(operation)
            } else {
                bookmarkDao.updateSyncOperation(
                    operation.copy(
                        status = BookmarkSyncQueueEntity.SyncStatus.FAILED,
                        retryCount = operation.retryCount + 1,
                        lastAttempt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /**
     * Force manual sync
     */
    fun forceSyncNow() {
        viewModelScope.launch {
            if (isConnected.value) {
                syncPendingOperations()
            } else {
                _syncStatus.value = SyncStatus.Error("No internet connection")
            }
        }
    }

    /**
     * Sync ADD to Supabase
     */
    private suspend fun syncAddToSupabase(newsItemId: Int) {
        val userProfileId = getCurrentUserProfileId()
        if (userProfileId == null) {
            Log.e(TAG, "Cannot sync: user not authenticated")
            throw Exception("User not authenticated")
        }

        // ✅ CORREGIDO: Usar JSON String en lugar de Map
        val jsonBody = """
        {
            "user_profile_id": $userProfileId,
            "news_item_id": $newsItemId,
            "is_deleted": false
        }
    """.trimIndent()

        supabaseClient.from("bookmarks")
            .insert(jsonBody)

        Log.d(TAG, "Bookmark added to Supabase: newsItemId=$newsItemId, userId=$userProfileId")
    }

    /**
     * Sync REMOVE from Supabase
     */
    private suspend fun syncRemoveFromSupabase(newsItemId: Int) {
        val userProfileId = getCurrentUserProfileId()
        if (userProfileId == null) {
            Log.e(TAG, "Cannot sync: user not authenticated")
            throw Exception("User not authenticated")
        }

        // Usar JSON String
        val jsonBody = """
        {
            "is_deleted": true
        }
    """.trimIndent()

        supabaseClient.from("bookmarks")
            .update(jsonBody) {
                filter {
                    eq("user_profile_id", userProfileId)
                    eq("news_item_id", newsItemId)
                    eq("is_deleted", false)
                }
            }

        Log.d(TAG, "Bookmark removed from Supabase: newsItemId=$newsItemId, userId=$userProfileId")
    }

    sealed class SyncStatus {
        object Idle : SyncStatus()
        object Syncing : SyncStatus()
        object Success : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }

    companion object {
        private const val TAG = "BookmarkViewModel"
    }
}