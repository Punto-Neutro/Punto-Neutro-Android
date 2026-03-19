package com.example.sprint_2_kotlin.model.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) for Bookmark operations
 * Provides methods for Local-First bookmark management and sync queue
 */
@Dao
interface BookmarkDao {

    // ========== BOOKMARKS ==========

    /**
     * Get all bookmarks as Flow (reactive updates)
     * Ordered by most recent first
     */
    @Query("SELECT * FROM bookmarks ORDER BY bookmarkedAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    /**
     * Check if a news item is bookmarked
     */
    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE newsItemId = :newsItemId AND userid = :userProfileId)")
    suspend fun isBookmarked(newsItemId: Int, userProfileId: Int?): Boolean

    /**
     * Get a specific bookmark
     */
    @Query("SELECT * FROM bookmarks WHERE newsItemId = :newsItemId")
    suspend fun getBookmark(newsItemId: Int): BookmarkEntity?

    /**
     * Insert bookmark (Local-First)
     * OnConflictStrategy.REPLACE updates if already exists
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookmarks: List<BookmarkEntity>)


    /**
     * Delete bookmark by ID
     */
    @Query("DELETE FROM bookmarks WHERE newsItemId = :newsItemId")
    suspend fun deleteBookmarkById(newsItemId: Int)

    /**
     * Delete bookmark by ID
     */
    @Query("DELETE FROM bookmarks ")
    suspend fun deleteAll()


    /**
     * Get total count of bookmarks
     */
    @Query("SELECT COUNT(*) FROM bookmarks")
    fun getBookmarkCount(): Flow<Int>

    /**
     * Update sync status of a bookmark
     */
    @Query("UPDATE bookmarks SET isSynced = :synced, lastSyncAttempt = :timestamp WHERE newsItemId = :newsItemId")
    suspend fun updateSyncStatus(newsItemId: Int, synced: Boolean, timestamp: Long)

    /**
     * Get bookmarks that haven't been synced yet
     */
    @Query("SELECT * FROM bookmarks WHERE isSynced = 0")
    suspend fun getUnsyncedBookmarks(): List<BookmarkEntity>

    /**
     * Clear all bookmarks
     */
    @Query("DELETE FROM bookmarks")
    suspend fun clearAllBookmarks()

    // ========== SYNC QUEUE ==========

    /**
     * Add operation to sync queue
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncOperation(operation: BookmarkSyncQueueEntity)

    /**
     * Get pending sync operations
     */
    @Query("SELECT * FROM bookmark_sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingSyncOperations(): List<BookmarkSyncQueueEntity>

    /**
     * Update sync operation status
     */
    @Update
    suspend fun updateSyncOperation(operation: BookmarkSyncQueueEntity)

    /**
     * Delete sync operation
     */
    @Delete
    suspend fun deleteSyncOperation(operation: BookmarkSyncQueueEntity)

    /**
     * Delete completed operations
     */
    @Query("DELETE FROM bookmark_sync_queue WHERE status = 'COMPLETED'")
    suspend fun deleteCompletedOperations()

    /**
     * Count pending operations
     */
    @Query("SELECT COUNT(*) FROM bookmark_sync_queue WHERE status = 'PENDING'")
    fun getPendingOperationsCount(): Flow<Int>

    // ========== TRANSACTIONS ==========

    /**
     * Transaction: Add bookmark and register in sync queue
     */
    @Transaction
    suspend fun addBookmarkWithSync(bookmark: BookmarkEntity, syncOperation: BookmarkSyncQueueEntity) {
        insertBookmark(bookmark)
        insertSyncOperation(syncOperation)
    }

    /**
     * Transaction: Remove bookmark and register in sync queue
     */
    @Transaction
    suspend fun removeBookmarkWithSync(newsItemId: Int, syncOperation: BookmarkSyncQueueEntity) {
        deleteBookmarkById(newsItemId)
        insertSyncOperation(syncOperation)
    }
}