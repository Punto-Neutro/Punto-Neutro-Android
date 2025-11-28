package com.example.sprint_2_kotlin.model.data

import android.util.Log
import java.util.LinkedHashMap

/**
 * LRU Cache for search queries
 * Stores search results with TTL (Time To Live) policy
 *
 * Features:
 * - LRU eviction when cache is full
 * - TTL-based expiration (default 24 hours)
 * - Thread-safe operations
 */
class SearchQueryCache(
    private val maxSize: Int = 50,  // Máximo 50 queries en cache
    private val ttlMillis: Long = 24 * 60 * 60 * 1000  // 24 horas
) {
    companion object {
        private const val TAG = "SearchQueryCache"
    }

    /**
     * Cache entry with timestamp for TTL
     */
    private data class CacheEntry(
        val newsItemIds: List<Int>,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(ttl: Long): Boolean {
            return System.currentTimeMillis() - timestamp > ttl
        }
    }

    /**
     * LRU Cache implementation using LinkedHashMap
     * accessOrder = true means entries are ordered by access (LRU)
     */
    private val cache = object : LinkedHashMap<String, CacheEntry>(
        maxSize,
        0.75f,
        true  // accessOrder = true for LRU
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean {
            val shouldRemove = size > maxSize
            if (shouldRemove && eldest != null) {
                Log.d(TAG, "LRU eviction: removing query '${eldest.key}'")
            }
            return shouldRemove
        }
    }

    /**
     * Get cached results for a query
     * Returns null if cache miss or expired
     */
    @Synchronized
    fun get(query: String): List<Int>? {
        val normalizedQuery = query.trim().lowercase()

        val entry = cache[normalizedQuery]

        return when {
            entry == null -> {
                Log.d(TAG, "Cache MISS for query: '$normalizedQuery'")
                null
            }
            entry.isExpired(ttlMillis) -> {
                Log.d(TAG, "Cache EXPIRED for query: '$normalizedQuery'")
                cache.remove(normalizedQuery)
                null
            }
            else -> {
                Log.d(TAG, "Cache HIT for query: '$normalizedQuery' (${entry.newsItemIds.size} results)")
                entry.newsItemIds
            }
        }
    }

    /**
     * Put search results into cache
     */
    @Synchronized
    fun put(query: String, newsItemIds: List<Int>) {
        val normalizedQuery = query.trim().lowercase()
        cache[normalizedQuery] = CacheEntry(newsItemIds)
        Log.d(TAG, "Cached query: '$normalizedQuery' with ${newsItemIds.size} results")
    }

    /**
     * Clear all cache
     */
    @Synchronized
    fun clear() {
        val size = cache.size
        cache.clear()
        Log.d(TAG, "Cache cleared ($size queries removed)")
    }

    /**
     * Remove expired entries
     */
    @Synchronized
    fun removeExpired() {
        val iterator = cache.iterator()
        var removedCount = 0

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.isExpired(ttlMillis)) {
                iterator.remove()
                removedCount++
            }
        }

        if (removedCount > 0) {
            Log.d(TAG, "Removed $removedCount expired queries")
        }
    }

    /**
     * Get cache statistics
     */
    @Synchronized
    fun getStats(): CacheStats {
        return CacheStats(
            totalQueries = cache.size,
            maxSize = maxSize,
            ttlHours = ttlMillis / (60 * 60 * 1000)
        )
    }

    data class CacheStats(
        val totalQueries: Int,
        val maxSize: Int,
        val ttlHours: Long
    )
}