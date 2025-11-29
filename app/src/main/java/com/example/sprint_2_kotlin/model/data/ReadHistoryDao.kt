package com.example.sprint_2_kotlin.model.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) for Read History operations
 * Provides methods to track and retrieve user's reading history
 */
@Dao
interface ReadHistoryDao {

    /**
     * Insert a new read history entry
     * OnConflictStrategy.IGNORE prevents duplicate entries for the same news item
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReadHistory(readHistory: ReadHistoryEntity)

    /**
     * Get all read history entries as Flow (reactive updates)
     * Ordered by most recent first
     */
    @Query("SELECT * FROM read_history ORDER BY readTimestamp DESC")
    fun getAllReadHistory(): Flow<List<ReadHistoryEntity>>

    /**
     * Get all read history entries as a single list (non-reactive)
     * Useful for one-time queries
     */
    @Query("SELECT * FROM read_history ORDER BY readTimestamp DESC")
    suspend fun getAllReadHistoryList(): List<ReadHistoryEntity>

    /**
     * Get the total count of articles read by the user
     * Used for the "Articles read" counter in ProfileScreen
     */
    @Query("SELECT COUNT(*) FROM read_history")
    suspend fun getReadCount(): Int

    /**
     * Get the total count of articles read as Flow (reactive)
     * Updates automatically when new articles are read
     */
    @Query("SELECT COUNT(*) FROM read_history")
    fun getReadCountFlow(): Flow<Int>

    /**
     * Check if a specific news item has already been read
     * Returns true if exists, false otherwise
     * Used to prevent duplicate entries
     */
    @Query("SELECT EXISTS(SELECT 1 FROM read_history WHERE newsItemId = :newsItemId LIMIT 1)")
    suspend fun isNewsItemRead(newsItemId: Int): Boolean

    /**
     * Get read history filtered by category
     */
    @Query("SELECT * FROM read_history WHERE categoryId = :categoryId ORDER BY readTimestamp DESC")
    fun getReadHistoryByCategory(categoryId: Int): Flow<List<ReadHistoryEntity>>

    /**
     * Delete a specific history entry
     */
    @Query("DELETE FROM read_history WHERE id = :historyId")
    suspend fun deleteHistoryEntry(historyId: Int)

    /**
     * Delete all read history (clear history)
     */
    @Query("DELETE FROM read_history")
    suspend fun deleteAllHistory()

    /**
     * Get the most recent N articles read
     * Useful for showing "Recently read" section
     */
    @Query("SELECT * FROM read_history ORDER BY readTimestamp DESC LIMIT :limit")
    suspend fun getRecentReadHistory(limit: Int = 10): List<ReadHistoryEntity>

    /**
     * Delete old history entries (older than specified timestamp)
     * Useful for implementing automatic cleanup
     */
    @Query("DELETE FROM read_history WHERE readTimestamp < :expirationTimestamp")
    suspend fun deleteOldHistory(expirationTimestamp: Long)
}