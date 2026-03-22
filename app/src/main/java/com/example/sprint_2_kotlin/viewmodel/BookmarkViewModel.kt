package com.example.sprint_2_kotlin.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sprint_2_kotlin.model.data.*
import com.example.sprint_2_kotlin.model.repository.Repository
import utils.NetworkMonitor
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.take

class BookmarkViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val bookmarkDao = AppDatabase.getDatabase(application).bookmarkDao()
    private val networkMonitor = NetworkMonitor(application)

    // Database DAOs
    private val dao = AppDatabase.getDatabase(application).CommentDao()
    private val daonews = AppDatabase.getDatabase(application).newsItemDao()

    private val daopqrs = AppDatabase.getDatabase(application).PQRSDao()
    private val daopqrstypes = AppDatabase.getDatabase(application).PQRS_typesDao()


    private val repository = Repository(application.applicationContext, dao, daonews, daopqrs, daopqrstypes)

    private var currentOffset = 0
    private val PAGE_SIZE = 20
    private var isLastPage = false


    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading





    // Supabase client (same configuration as Repository)
    private val supabaseClient = createSupabaseClient(
        supabaseUrl = "https://fyotaxqfpgbkyefapzya.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZ5b3RheHFmcGdia3llZmFwenlhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzE4MDMxNDIsImV4cCI6MjA4NzM3OTE0Mn0.Hvb--I3VLCkkhXAbMUaUC-O2SKbV9JyyUjFc3dmmxjU"
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


    // 1. Add a state for the visible paginated items
    private val _paginatedBookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    val paginatedBookmarks: StateFlow<List<BookmarkEntity>> = _paginatedBookmarks.asStateFlow()

    // 2. We need to know the full list from the DB to "paginate" locally first
    private var allLocalBookmarks = listOf<BookmarkEntity>()


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

            loadBookmarks(false)
            isConnected
                .filter { it } // Only when connected
                .collect {
                    syncPendingOperations()
                }
        }

        viewModelScope.launch {
            bookmarkDao.getAllBookmarks().collect {
                allLocalBookmarks = it
                // When the DB changes, refresh the first page of the visible list
                refreshPaginatedList()
            }
        }
    }

    // ========== USER AUTHENTICATION ==========

    /**
     * Get current user's profile ID from Supabase
     * Same logic as Repository.kt
     */

    // ========== LOCAL-FIRST OPERATIONS ==========

    /**
     * Check if a news item is bookmarked
     */
    suspend fun isBookmarked(newsItemId: Int): Boolean = withContext(Dispatchers.IO) {
        val userid = repository.getCurrentUserProfileId()
        val boolean = bookmarkDao.isBookmarked(newsItemId,userid)
        Log.d(TAG, "isBookmarked: $boolean")
        return@withContext boolean
    }

    /**
     * Toggle bookmark (Local-First + Eventual Sync)
     */
    fun toggleBookmark(newsItem: NewsItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userid = repository.getCurrentUserProfileId()

                val isCurrentlyBookmarked = bookmarkDao.isBookmarked(newsItem.news_item_id,userid)

                if (isCurrentlyBookmarked) {
                    removeBookmark(newsItem.news_item_id, newsItem.category_id, userid, newsItem.short_description, newsItem.image_url, newsItem.title)
                } else {
                    addBookmark(newsItem)
                    Log.d(TAG, "Bookmark added: ${newsItem.news_item_id}")
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
            operationType = BookmarkSyncQueueEntity.OperationType.ADD,
            categoryId = newsItem.category_id,
            userId = newsItem.user_profile_id,
            shortDescription = newsItem.short_description,
            imageUrl = newsItem.image_url,
            title = newsItem.title
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
    private suspend fun removeBookmark(newsItemId: Int, categoryId: Int, userId: Int?, shortDescription: String, imageUrl: String, title: String) {
        val syncOperation = BookmarkSyncQueueEntity(
            newsItemId = newsItemId,
            operationType = BookmarkSyncQueueEntity.OperationType.REMOVE,
            categoryId = categoryId,
            userId = userId,
            shortDescription = shortDescription,
            imageUrl = imageUrl,
            title = title

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
                        operationType = BookmarkSyncQueueEntity.OperationType.REMOVE,
                        categoryId = bookmark.categoryId,
                        userId = bookmark.userid,
                        shortDescription = bookmark.shortDescription,
                        imageUrl = bookmark.imageUrl,
                        title = bookmark.title

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
                    syncAddToSupabase(operation.newsItemId,operation.categoryId,operation.imageUrl,operation.shortDescription,operation.title)
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
    private suspend fun syncAddToSupabase(newsItemId: Int,categoryId: Int,imageUrl: String,shortDescription: String,title: String) {
        val userProfileId = repository.getCurrentUserProfileId()
        if (userProfileId == null) {
            Log.e(TAG, "Cannot sync: user not authenticated")
            throw Exception("User not authenticated")
        }



        repository.addBookmarks(userProfileId, newsItemId,imageUrl, title,categoryId,shortDescription)





        Log.d(TAG, "Bookmark added to Supabase: newsItemId=$newsItemId, userId=$userProfileId")
    }

    /**
     * Sync REMOVE from Supabase
     */
    private suspend fun syncRemoveFromSupabase(newsItemId: Int) {
        val userProfileId = repository.getCurrentUserProfileId()
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

        repository.deleteBookmark( newsItemId)



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

    //===============================================================
    // Pagination
    // ===============================================================

    private fun refreshPaginatedList() {
        currentOffset = 0
        isLastPage = false
        // Take the first chunk from the local DB
        _paginatedBookmarks.value = allLocalBookmarks.take(PAGE_SIZE)
        currentOffset = _paginatedBookmarks.value.size
    }





    fun loadNextPage() {
        if (_isLoading.value || isLastPage) return

        viewModelScope.launch {
            _isLoading.value = true

            // Strategy: First try to load more from the local Room list
            if (currentOffset < allLocalBookmarks.size) {
                val nextChunk = allLocalBookmarks.drop(currentOffset).take(PAGE_SIZE)
                _paginatedBookmarks.value = _paginatedBookmarks.value + nextChunk
                currentOffset += nextChunk.size
                _isLoading.value = false
            }
            // If local is exhausted, fetch from Supabase
            else if (isConnected.value) {
                try {
                    // You'll need to implement getBookmarks in your Repository
                    // similar to getNewsItems but for the 'bookmarks' table
                    val remoteBookmarks = repository.getBookmarks(
                        forcedrefresh = false,
                        pageSize = PAGE_SIZE,
                        startRow = currentOffset
                    )

                    if (remoteBookmarks.isNotEmpty()) {
                        // Update currentOffset and check if it's the last page
                        currentOffset += remoteBookmarks.size
                        isLastPage = remoteBookmarks.size < PAGE_SIZE

                        // Note: Usually, remote bookmarks should be saved to Room,
                        // which triggers the collector above and updates the UI.
                    } else {
                        isLastPage = true
                    }
                } catch (e: Exception) {
                    Log.e("BookmarkVM", "Error fetching remote bookmarks", e)
                } finally {
                    _isLoading.value = false
                }
            } else {
                _isLoading.value = false
                isLastPage = true // No internet and no more local items
            }
        }
    }

    private fun loadBookmarks(forcedRefresh: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading Bookmarks...")
                val BookmarksList = repository.getBookmarks(forcedRefresh )

                Log.d(TAG, "Bookmarks loaded: ${BookmarksList.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Bookmarks", e)

            }
        }
    }



}