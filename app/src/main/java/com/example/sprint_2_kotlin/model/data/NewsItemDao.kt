package com.example.sprint_2_kotlin.model.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) for NewsItem database operations
 * Provides methods to interact with the cached news items
 */
@Dao
interface NewsItemDao {

    /**
     * Insert a single news item into the cache
     * OnConflictStrategy.REPLACE will update if the item already exists
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNewsItem(newsItem: NewsItemEntity)

    /**
     * Insert multiple news items at once
     * More efficient for bulk operations
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNewsItems(newsItems: List<NewsItemEntity>)

    /**
     * Get all cached news items as Flow (reactive updates)
     * Flow will automatically emit new values when database changes
     * Ordered by publication date (most recent first)
     */
    @Query("SELECT * FROM news_items ORDER BY publication_date DESC")
    fun getAllNewsItems(): Flow<List<NewsItemEntity>>

    /**
     * Get all cached news items as a single list (non-reactive)
     */
    @Query("SELECT * FROM news_items ORDER BY publication_date DESC")
    suspend fun getAllNewsItemsList(): List<NewsItemEntity>

    /**
     * Get news items filtered by category
     */
    @Query("SELECT * FROM news_items WHERE category_id = :categoryId ORDER BY publication_date DESC")
    fun getNewsItemsByCategory(categoryId: Int): Flow<List<NewsItemEntity>>

    /**
     * Get a single news item by ID
     */
    @Query("SELECT * FROM news_items WHERE news_item_id = :newsItemId")
    suspend fun getNewsItemById(newsItemId: Int): NewsItemEntity?

    /**
     * Delete all cached news items
     * Useful for clearing the cache
     */
    @Query("DELETE FROM news_items")
    suspend fun deleteAllNewsItems()

    /**
     * Delete a specific news item by ID
     */
    @Query("DELETE FROM news_items WHERE news_item_id = :newsItemId")
    suspend fun deleteNewsItemById(newsItemId: Int)

    /**
     * Delete old cached items (older than the specified timestamp)
     * This is useful for implementing cache expiration
     * @param expirationTimestamp Items cached before this time will be deleted
     */
    @Query("DELETE FROM news_items WHERE cachedTimestamp < :expirationTimestamp")
    suspend fun deleteOldCachedItems(expirationTimestamp: Long)

    /**
     * Get the count of cached news items
     */
    @Query("SELECT COUNT(*) FROM news_items")
    suspend fun getCachedItemsCount(): Int

    /**
     * Check if cache has data
     */
    @Query("SELECT COUNT(*) > 0 FROM news_items")
    suspend fun hasCachedData(): Boolean

    /**
     * Get fake news items only
     */
    @Query("SELECT * FROM news_items WHERE is_fake = 1 ORDER BY publication_date DESC")
    fun getFakeNewsItems(): Flow<List<NewsItemEntity>>

    /**
     * Get verified news items only
     */
    @Query("SELECT * FROM news_items WHERE is_verifiedSource = 1 ORDER BY publication_date DESC")
    fun getVerifiedNewsItems(): Flow<List<NewsItemEntity>>

    @Query("SELECT * FROM news_items WHERE news_item_id = 0")
    suspend fun getAllPendingNews(): List<NewsItemEntity>

    // Make sure you have a delete function that accepts a NewsItemEntity
    @Delete
    suspend fun delete(newsItem: NewsItemEntity)
    /**
     * Search news items by title or short description
     * Uses LIKE operator for partial matching (case-insensitive)
     * @param searchQuery The text to search for
     * @return Flow of matching news items ordered by publication date
     */
    @Query("""
    SELECT * FROM news_items 
    WHERE title LIKE '%' || :searchQuery || '%' 
       OR short_description LIKE '%' || :searchQuery || '%'
    ORDER BY publication_date DESC
""")
    fun searchNewsItems(searchQuery: String): Flow<List<NewsItemEntity>>

    @Query("UPDATE news_items SET average_reliability_score = :newAverageReliabilityScore WHERE news_item_id = :newsItemId")
    suspend fun updateReliabilityScore(newsItemId: Int, newAverageReliabilityScore: Double)

}






