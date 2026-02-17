package com.example.sprint_2_kotlin.model.repository

import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.colorspace.connect
import com.example.sprint_2_kotlin.model.data.*
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import utils.NetworkMonitor
import org.jsoup.Jsoup
import java.io.IOException




/**
 * Repository with Cache-First Strategy for News Feed
 * Maintains all existing Supabase functionality
 */
class Repository(private val context: Context,private val daocomment: CommentDao, private val daonewsitem: NewsItemDao ) {

    // ============================================
    // SUPABASE CLIENT (EXISTING CODE - NO CHANGES)
    // ============================================

    private val client = createSupabaseClient(
        supabaseUrl = "https://oikdnxujjmkbewdhpyor.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9pa2RueHVqam1rYmV3ZGhweW9yIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTk0MDU0MjksImV4cCI6MjA3NDk4MTQyOX0.htw3cdc-wFcBjKKPP4aEC9K9xBEnvPULMToP_PIuaLI"
    ) {
        install(Postgrest)
        install(Auth)
    }

    private val auth = client.auth

   // Monitor to verify internet connection
    private val networkMonitor = NetworkMonitor(context)


    // ============================================
    // ROOM DATABASE (NEW - FOR CACHING)
    // ============================================

    private val database = AppDatabase.getDatabase(context)
    private val newsItemDao = database.newsItemDao()

    private val categoryDao = database.categoryDao()

    private val countryDao = database.countryDao()

    // Cache expiration time: 30 minutes in milliseconds
    private val CACHE_EXPIRATION_TIME = 30 * 60 * 1000L
    // ============================================
    // SEARCH QUERY CACHE (LRU + TTL)
    // ============================================

    /**
     * LRU Cache for search queries with 24h TTL
     * Stores query → list of news item IDs
     */
    private val searchQueryCache = SearchQueryCache(
        maxSize = 50,  // Máximo 50 búsquedas en cache
        ttlMillis = 24 * 60 * 60 * 1000  // 24 horas
    )

    companion object {
        private const val TAG = "Repository"
    }

    // ============================================
    // AUTH FUNCTIONS (EXISTING CODE - NO CHANGES)
    // ============================================

    suspend fun signIn(email: String, password: String): Boolean {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun signUp(email: String, password: String, country: Int): Boolean {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val uid = auth.currentUserOrNull()?.id
            val data = UserProfile(
                uid,email,country
            )
            client.postgrest.from("user_profiles").insert(listOf(data))

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentSessionOrNull() != null
    }

    suspend fun signOut() {
        auth.signOut()
    }

    // ============================================
    // FETCH FUNCTIONS (EXISTING CODE - NO CHANGES)
    // ============================================

    suspend fun getNewsItems(pageSize: Int = 20, startRow: Int = 0): List<NewsItem> {
        val response = client.postgrest["news_items"].select {
            order("added_to_app_date", order = Order.DESCENDING)
            range(startRow.toLong(), (startRow + pageSize - 1).toLong())
        }
        return response.decodeList()
    }

    suspend fun getRatingsForNewsItem(newsItemId: Int): List<RatingItem> {
        return try {
            val response = client.postgrest["rating_items"].select {
                filter {
                    eq("news_item_id", newsItemId)
                }
            }
            response.decodeList<RatingItem>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getNewsItemById(newsItemId: Int): NewsItem {
        return try {
            val response = client.postgrest["news_items"].select {
                filter {
                    eq("news_item_id", newsItemId)
                }
            }
            response.decodeSingle<NewsItem>()
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to load news item with id: $newsItemId")
        }
    }
// Add Comments function with connectivity resistance
    suspend fun addNewComments(
        userProfileId: Int,
        newsItemId: Int,
        comment: String,
        rating: Double,
        completed: Boolean
    ): Int {
         if (networkMonitor.isConnected.value){
             try {

            val user = client.auth.currentUserOrNull()!!.id
            val response = client
                .from("user_profiles").select() { filter { eq("user_auth_id", user) } }
            val profiles = response.decodeList<UserProfile>()

            val profile = profiles.first()
            val userProfileIdActual = profile.user_profile_id

            val scaledValue = rating * 100
            val truncatedValue = kotlin.math.floor(scaledValue)
            val ratingf = truncatedValue / 100

            val datos = RatingItem(
                newsItemId,
                userProfileIdActual,
                ratingf,
                comment,
                true
            )

            val answer = client.from("rating_items").insert(listOf(datos)) {}
                 updateReliabilityScore(newsItemId,rating)
             return 0
            } catch (e: Exception) {

                 Log.w(TAG,"Error en la espera")
            e.printStackTrace()
             return 1  }

         }else{
         daocomment.insert(PendingComment(newsItemId = newsItemId, userProfileId = 0, commentText = comment, reliabilityScore = rating))
             Log.w(TAG,"Se activo el encolamiento")
             return 2

        }
    }
    // Function that uploads the comments that have been uploaded without internet connection
    suspend fun syncPendingComments(): Int  {

    try {
        Log.w(TAG,"sincronizando")

        val user = client.auth.currentUserOrNull()!!.id
        val response = client
            .from("user_profiles").select() { filter { eq("user_auth_id", user) } }
        val profiles = response.decodeList<UserProfile>()

        val profile = profiles.first()
        val userProfileIdActual = profile.user_profile_id




        val pending = daocomment.getAll()


        for (comment in pending) {
            try {

                val scaledValue = comment.reliabilityScore * 100
                val truncatedValue = kotlin.math.floor(scaledValue)
                val ratingf = truncatedValue / 100
                val newsItemId = comment.newsItemId

                val datos = RatingItem(
                    newsItemId,
                    userProfileIdActual,
                    ratingf,
                    comment.commentText,
                    true
                )

                val answer = client.from("rating_items").insert(listOf(datos)) {}

                val newsItem = getNewsItemById(newsItemId)
                val totalRatings = newsItem.total_ratings
                val averagereliabilityscore = newsItem.average_reliability_score
                val newtotalRatings = totalRatings + 1
                val newAverage = (totalRatings*averagereliabilityscore + ratingf)/newtotalRatings
                val scaledValue1 = newAverage * 100
                val truncatedValue1 = kotlin.math.floor(scaledValue1)
                val newAveragerounded = truncatedValue1 / 100
                val response = client.from("news_items")
                    .update({set("total_ratings", newtotalRatings);set("average_reliability_score", newAveragerounded)})
                    {filter { eq("news_item_id",newsItemId) }}





                daocomment.delete(comment)

            } catch (_: Exception) {
                // Si falla, sigue pendiente
                daocomment.delete(comment)
                return 0
            }
        }

    } catch (_: Exception) {
        return 2

    }
     return 1

    }

    //========================================================
    // Add News Function with connectivity resistance
    //========================================================

    suspend fun addNews(url: String, category_id: Int): Int{

        if (networkMonitor.isConnected.value){
            try {

                val user = client.auth.currentUserOrNull()!!.id
                val response = client
                    .from("user_profiles").select() { filter { eq("user_auth_id", user) } }
                val profiles = response.decodeList<UserProfile>()

                val profile = profiles.first()
                val userProfileIdActual = profile.user_profile_id

                val imageUrl = extractImageUrlFromArticle(url) ?: ""

                val title = extractTitle(url) ?: ""

                val description = extractDescription(url) ?: ""

                val author_type = extractAuthor(url) ?: ""

                val author_institution = extractAuthorInstitution(url) ?: ""




                val datos = NewsItem(
                    userProfileIdActual,
                    title = title,
                    short_description = description,
                    long_description = description,
                    image_url = imageUrl,
                    original_source_url = url,
                    category_id = category_id,
                    author_type = author_type,
                    author_institution = author_institution,
                    average_reliability_score = 0.0,
                    total_ratings = 0,
                    days_since = 0,
                    is_fake = false,
                    is_verifiedData = false,
                    is_verifiedSource = false,









                )

                val answer = client.from("news_items").insert(listOf(datos)) {}

                return 0
            } catch (e: Exception) {

                Log.w(TAG,"Error en la espera")
                e.printStackTrace()
                return 1  }

        }else{
            daonewsitem.insertNewsItem(NewsItemEntity(user_profile_id = 0, title = "", short_description = "", image_url = "", category_id = category_id, author_type = "", author_institution = "", average_reliability_score = 0.0, total_ratings = 0, days_since = 0, news_item_id = 0, cachedTimestamp = System.currentTimeMillis(), is_fake = false, is_verifiedData = false, is_verifiedSource = false, is_recognizedAuthor = false, is_manipulated = false, long_description = "", original_source_url = url, publication_date = "", added_to_appDate = ""))
            Log.w(TAG,"Se activo el encolamiento")
            return 2

        }


    }

    suspend fun syncPendingNews():Int {

        if (!networkMonitor.isConnected.value) {
            Log.d(TAG, "Cannot sync pending news, no internet connection.")
            return 2 // No connection
        }

        try {
            Log.d(TAG, "Syncing pending news...")

            // 1. Get the current user's profile to assign ownership
            val user = client.auth.currentUserOrNull()?.id ?: return 2 // Not logged in
            val profileResponse = client.from("user_profiles").select { filter { eq("user_auth_id", user) } }
            val profile = profileResponse.decodeList<UserProfile>().firstOrNull() ?: return 2 // Profile not found

            // 2. Get all pending news items from the local database
            val pendingNews = daonewsitem.getAllPendingNews() // Assuming you create this DAO function
            if (pendingNews.isEmpty()) {
                Log.d(TAG, "No pending news to sync.")
                return 1 // Nothing to sync
            }

            Log.d(TAG, "Found ${pendingNews.size} pending news items to upload.")

            // 3. Iterate through each pending item and upload it
            for (pendingItem in pendingNews) {
                try {
                    // Fetch the image URL online, as it wasn't available offline
                    val imageUrl = extractImageUrlFromArticle(pendingItem.original_source_url) ?: ""

                    val title = extractTitle(pendingItem.original_source_url) ?: ""

                    val description = extractDescription(pendingItem.original_source_url) ?: ""

                    val author_type = extractAuthor(pendingItem.original_source_url) ?: ""

                    val author_institution = extractAuthorInstitution(pendingItem.original_source_url) ?: ""

                    // Create a NewsItem object for Supabase, mapping fields from NewsItemEntity
                    val newsItemToUpload = NewsItem(
                        title = title,
                        short_description = description, // Assuming long_description holds the full text
                        long_description = description,
                        image_url = imageUrl,
                        original_source_url = pendingItem.original_source_url,
                        category_id = pendingItem.category_id,
                        author_type = author_type,
                        author_institution = author_institution,
                        user_profile_id = profile.user_profile_id,
                        total_ratings = 0, // Starts with no ratings
                        average_reliability_score = 0.0 // Starts with no score
                    )

                    // Insert the item into the remote 'news_items' table
                    client.from("news_items").insert(newsItemToUpload)

                    // 4. If upload is successful, delete the pending item from the local DB
                    daonewsitem.delete(pendingItem)
                    Log.d(TAG, "Successfully synced and deleted pending news item: ${pendingItem.title}")

                } catch (uploadError: Exception) {
                    Log.e(TAG, "Failed to sync pending news item: ${pendingItem.title}", uploadError)
                    // If a single item fails, we can choose to continue with the next one
                    // or stop. Continuing is generally better for robustness.
                }
            }
            // After syncing, clear the main news cache to ensure data is fresh on next load
            clearCache()
            return 1 // Sync process completed

        } catch (e: Exception) {
            Log.e(TAG, "An error occurred during the news sync process", e)
            return 0 // General failure
        }
    }




   //===================================================
    // Function to update average reliability score
    //===============================================
    suspend fun updateReliabilityScore(NewsItemId: Int, rating: Double): Any {
        return try {
            val newsItem = getNewsItemById(NewsItemId)
            val totalRatings = newsItem.total_ratings
            val averagereliabilityscore = newsItem.average_reliability_score
            val newtotalRatings = totalRatings + 1
            val newAverage = (totalRatings*averagereliabilityscore + rating)/newtotalRatings
            val scaledValue = newAverage * 100
            val truncatedValue = kotlin.math.floor(scaledValue)
            val newAveragerounded = truncatedValue / 100
            val response = client.from("news_items")
                .update({set("total_ratings", newtotalRatings);set("average_reliability_score", newAveragerounded)})
                {filter { eq("news_item_id",NewsItemId) }}
            clearCache()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ============================================
    // NEW: CACHE-FIRST FUNCTIONS FOR NEWS FEED
    // ============================================

    fun getNewsFeedCached(): Flow<List<NewsItem>> {
        return newsItemDao.getAllNewsItems().map { cachedEntities ->
            cachedEntities.map { it.toNewsItem() }
        }
    }

    suspend fun loadNewsFeedCached(
        forceRefresh: Boolean = false,
        pageSize: Int = 20,
        startRow: Int = 0
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "loadNewsFeedCached - forceRefresh: $forceRefresh")

            if (!forceRefresh && shouldUseCachedData()) {
                Log.d(TAG, "Using cached data (cache is fresh)")
                return@withContext
            }

            Log.d(TAG, "Fetching fresh data from Supabase...")

            val freshNewsItems = getNewsItems(pageSize, startRow)

            if (freshNewsItems.isEmpty()) {
                Log.w(TAG, "No data received from Supabase")
                return@withContext
            }

            if (forceRefresh) {
                newsItemDao.deleteAllNewsItems()
                Log.d(TAG, "Cache cleared due to force refresh")

                // Limpiar search cache
                clearSearchCache()
                Log.d(TAG, "🧹 Search cache cleared")
            }
            val entities = freshNewsItems.map { it.toEntity() }
            newsItemDao.insertAllNewsItems(entities)

            Log.d(TAG, "Successfully cached ${entities.size} news items from Supabase")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading news feed", e)
        }
    }

    suspend fun getNewsItemByIdCached(newsItemId: Int): NewsItem? = withContext(Dispatchers.IO) {
        try {
            val cachedItem = newsItemDao.getNewsItemById(newsItemId)
            if (cachedItem != null) {
                Log.d(TAG, "News item $newsItemId found in cache")
                return@withContext cachedItem.toNewsItem()
            }

            Log.d(TAG, "News item $newsItemId not in cache, fetching from Supabase...")
            val item = getNewsItemById(newsItemId)

            newsItemDao.insertNewsItem(item.toEntity())

            item
        } catch (e: Exception) {
            Log.e(TAG, "Error getting news item $newsItemId", e)
            null
        }
    }

    private suspend fun shouldUseCachedData(): Boolean {
        val hasCachedData = newsItemDao.hasCachedData()
        if (!hasCachedData) {
            Log.d(TAG, "No cached data available")
            return false
        }

        val cachedItems = newsItemDao.getAllNewsItemsList()
        if (cachedItems.isEmpty()) {
            return false
        }

        val newestItem = cachedItems.minByOrNull { it.cachedTimestamp }
        val currentTime = System.currentTimeMillis()
        val cacheAge = currentTime - (newestItem?.cachedTimestamp ?: 0)

        val isFresh = cacheAge < CACHE_EXPIRATION_TIME

        Log.d(TAG, "Cache age: ${cacheAge / 1000}s, Fresh: $isFresh")

        return isFresh
    }

    suspend fun getCachedItemsCount(): Int = withContext(Dispatchers.IO) {
        newsItemDao.getCachedItemsCount()
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        newsItemDao.deleteAllNewsItems()
        Log.d(TAG, "Cache cleared manually")
    }

    suspend fun deleteExpiredCache() = withContext(Dispatchers.IO) {
        val expirationTimestamp = System.currentTimeMillis() - CACHE_EXPIRATION_TIME
        newsItemDao.deleteOldCachedItems(expirationTimestamp)
        Log.d(TAG, "Expired cache items deleted")
    }

    /**
     * Search news items in cache by query
     * Searches in both title and short_description fields
     * Returns Flow for reactive updates
     *
     * @param searchQuery The text to search for (case-insensitive)
     * @return Flow of matching NewsItems, or all items if query is blank
     */
    /**
     * Search news items in cache by query with LRU caching
     *
     * Strategy:
     * 1. Check if query is in LRU cache (instant return)
     * 2. If cache miss, query Room database
     * 3. Store result in LRU cache for future use
     *
     * @param searchQuery The text to search for (case-insensitive)
     * @return Flow of matching NewsItems, or all items if query is blank
     */
    fun searchNewsItemsCached(searchQuery: String): Flow<List<NewsItem>> {
        return if (searchQuery.isBlank()) {
            // Empty query → return all news
            newsItemDao.getAllNewsItems().map { cachedEntities ->
                cachedEntities.map { it.toNewsItem() }
            }
        } else {
            // Check LRU cache first
            val cachedIds = searchQueryCache.get(searchQuery)

            if (cachedIds != null) {
                // CACHE HIT - Return cached results instantly
                Log.d(TAG, "Query cache HIT: returning ${cachedIds.size} cached results for '$searchQuery'")

                // Convert IDs to NewsItems from Room
                newsItemDao.getAllNewsItems().map { allItems ->
                    allItems
                        .filter { it.news_item_id in cachedIds }
                        .map { it.toNewsItem() }
                }
            } else {
                // CACHE MISS - Query Room and cache result
                Log.d(TAG, "Query cache MISS: searching Room for '$searchQuery'")

                newsItemDao.searchNewsItems(searchQuery).map { results ->
                    // Store result in cache
                    val newsItemIds = results.map { it.news_item_id }
                    searchQueryCache.put(searchQuery, newsItemIds)

                    Log.d(TAG, "Cached search result: '$searchQuery' → ${newsItemIds.size} items")

                    // Return NewsItems
                    results.map { it.toNewsItem() }
                }
            }
        }
    }

    /**
     * Clear search query cache
     * Useful when news data is refreshed
     */
    fun clearSearchCache() {
        searchQueryCache.clear()
        Log.d(TAG, "🧹 Search query cache cleared")
    }

    /**
     * Get search cache statistics
     */
    fun getSearchCacheStats(): SearchQueryCache.CacheStats {
        return searchQueryCache.getStats()
    }

    /**
     * Remove expired search queries from cache
     * Called periodically to clean up old entries
     */
    suspend fun cleanExpiredSearchCache() = withContext(Dispatchers.IO) {
        searchQueryCache.removeExpired()
    }

// ============================================
// BUSINESS QUESTION #4: RATING DISTRIBUTION (SUPABASE REAL)
// ============================================

    suspend fun getRatingDistributionByCategory(): Result<RatingDistributionData> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching real rating distribution from Supabase...")

                // 1. Obtener todas las categorías
                val categories = getCategories(forcedrefresh = false)
                if (categories.isEmpty()) {
                    Log.w(TAG, "No categories found in database")
                    return@withContext Result.failure(Exception("No categories found"))
                }

                // 2. Para cada categoría, calcular su distribución
                val distributions = mutableListOf<CategoryRatingDistribution>()

                for (category in categories) {
                    val distribution = calculateCategoryDistribution(category)
                    if (distribution != null) {
                        distributions.add(distribution)
                        Log.d(TAG, "Processed category: ${category.name} with ${distribution.ratingCount} ratings")
                    }
                }

                if (distributions.isEmpty()) {
                    Log.w(TAG, "No ratings found in database")
                    return@withContext Result.failure(Exception("No ratings data available"))
                }

                // 3. Calcular estadísticas globales
                val statistics = calculateGlobalStatistics(distributions)

                val result = RatingDistributionData(distributions, statistics)
                Log.d(TAG, "Rating distribution loaded successfully: ${distributions.size} categories, ${statistics.totalRatings} total ratings")

                Result.success(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading rating distribution from Supabase", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Calcula la distribución de ratings para una categoría específica
     */
    private suspend fun calculateCategoryDistribution(category: Category): CategoryRatingDistribution? {
        return try {
            // 1. Obtener todas las noticias de esta categoría
            val newsItems = client.postgrest["news_items"].select {
                filter {
                    eq("category_id", category.category_id)
                }
            }.decodeList<NewsItem>()

            if (newsItems.isEmpty()) {
                Log.d(TAG, "No news items found for category: ${category.name}")
                return null
            }

            // 2. Obtener todos los ratings de estas noticias
            val newsItemIds = newsItems.map { it.news_item_id }
            val allRatings = mutableListOf<RatingItem>()

            // Supabase tiene límite en queries IN, así que hacemos por lotes si es necesario
            newsItemIds.chunked(100).forEach { batch ->
                val ratings = client.postgrest["rating_items"].select {
                    filter {
                        isIn("news_item_id", batch)
                    }
                }.decodeList<RatingItem>()
                allRatings.addAll(ratings)
            }

            if (allRatings.isEmpty()) {
                Log.d(TAG, "No ratings found for category: ${category.name}")
                return null
            }

            // 3. Calcular estadísticas
            val avgReliability = allRatings.map { it.assigned_reliability_score }.average()
            val ratingCount = allRatings.size

            // 4. Calcular distribución por rangos
            val range0_20 = allRatings.count { it.assigned_reliability_score <= 0.20 }
            val range21_40 = allRatings.count { it.assigned_reliability_score in 0.21..0.40 }
            val range41_60 = allRatings.count { it.assigned_reliability_score in 0.41..0.60 }
            val range61_80 = allRatings.count { it.assigned_reliability_score in 0.61..0.80 }
            val range81_100 = allRatings.count { it.assigned_reliability_score in 0.81..1.0 }

            CategoryRatingDistribution(
                categoryId = category.category_id,
                category = category.name,
                avgReliabilityScore = avgReliability,
                ratingCount = ratingCount,
                range0_20 = range0_20,
                range21_40 = range21_40,
                range41_60 = range41_60,
                range61_80 = range61_80,
                range81_100 = range81_100
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error calculating distribution for category: ${category.name}", e)
            null
        }
    }

    /**
     * Calcula estadísticas globales a partir de las distribuciones por categoría
     */
    private fun calculateGlobalStatistics(distributions: List<CategoryRatingDistribution>): RatingStatistics {
        val totalRatings = distributions.sumOf { it.ratingCount }
        val avgReliability = if (distributions.isNotEmpty()) {
            // Promedio ponderado por número de ratings
            val weightedSum = distributions.sumOf { it.avgReliabilityScore * it.ratingCount }
            weightedSum / totalRatings
        } else {
            0.0
        }

        val mostRatedCategory = distributions.maxByOrNull { it.ratingCount }?.category ?: "N/A"
        val mostReliableCategory = distributions.maxByOrNull { it.avgReliabilityScore }?.category ?: "N/A"
        val leastReliableCategory = distributions.minByOrNull { it.avgReliabilityScore }?.category ?: "N/A"

        return RatingStatistics(
            totalRatings = totalRatings,
            avgReliability = avgReliability,
            mostRatedCategory = mostRatedCategory,
            mostReliableCategory = mostReliableCategory,
            leastReliableCategory = leastReliableCategory
        )
    }
    // ============================================
// CATEGORY FILTERING FUNCTIONS
// ============================================

    /**
     * Fetch all categories from Supabase
     */
    suspend fun getCategories(forcedrefresh: Boolean): List<Category> = withContext(Dispatchers.IO) {
        try {
            if (forcedrefresh){
                categoryDao.deleteAll()
                Log.d(TAG, "Cache cleared due to force refresh")
            }
            // First, try to get categories from the local cache
            val cachedCategories = categoryDao.getAllCategories()
            if (cachedCategories.isNotEmpty()) {
                Log.d(TAG, "Categories loaded from cache: ${cachedCategories.size}")
                return@withContext cachedCategories
            }

            // If cache is empty, fetch from Supabase
            Log.d(TAG, "Fetching categories from Supabase...")
            val response = client.postgrest["categories"].select()
            val categories = response.decodeList<Category>()

            // Save the fetched categories into the cache
            if (categories.isNotEmpty()) {
                categoryDao.insertAll(categories)
                Log.d(TAG, "Categories loaded from Supabase and cached: ${categories.size}")
            } else {
                Log.d(TAG, "No categories found on Supabase.")
            }

            categories
        } catch (e: Exception) {
            Log.e(TAG, "Error loading categories, attempting to use cache", e)
            // In case of a network error, still try to return from cache as a fallback
            try {
                categoryDao.getAllCategories()
            } catch (dbError: Exception) {
                Log.e(TAG, "Error reading categories from cache as fallback", dbError)
                emptyList()
            }
        }
    }

    /**
     * Fetch news items filtered by category
     */
    suspend fun getNewsItemsByCategory(
        categoryId: Int,
        pageSize: Int = 20,
        startRow: Int = 0
    ): List<NewsItem> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching news items for category: $categoryId")
            val response = client.postgrest["news_items"].select {
                filter {
                    eq("category_id", categoryId)
                }
                range(startRow.toLong(), (startRow + pageSize - 1).toLong())
            }
            val items = response.decodeList<NewsItem>()
            Log.d(TAG, "News items loaded for category $categoryId: ${items.size}")
            items
        } catch (e: Exception) {
            Log.e(TAG, "Error loading news items for category $categoryId", e)
            emptyList()
        }
    }

    /**
     * Load news feed with optional category filter (cached version)
     */
    suspend fun loadNewsFeedWithFilter(
        categoryId: Int? = null,
        forceRefresh: Boolean = false,
        pageSize: Int = 20,
        startRow: Int = 0
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "loadNewsFeedWithFilter - categoryId: $categoryId, forceRefresh: $forceRefresh")

            if (!forceRefresh && shouldUseCachedData()) {
                Log.d(TAG, "Using cached data (cache is fresh)")
                return@withContext
            }

            Log.d(TAG, "Fetching fresh data from Supabase...")

            val freshNewsItems = if (categoryId != null) {
                getNewsItemsByCategory(categoryId, pageSize, startRow)
            } else {
                getNewsItems(pageSize, startRow)
            }

            if (freshNewsItems.isEmpty()) {
                Log.w(TAG, "No data received from Supabase")
                return@withContext
            }

            if (forceRefresh) {
                newsItemDao.deleteAllNewsItems()
                Log.d(TAG, "Cache cleared due to force refresh")

                // Limpiar search cache para que las búsquedas se re-ejecuten con datos frescos
                clearSearchCache()
                Log.d(TAG, "🧹 Search cache cleared - queries will re-execute with fresh data")
            }

            val entities = freshNewsItems.map { it.toEntity() }
            newsItemDao.insertAllNewsItems(entities)

            Log.d(TAG, "Successfully cached ${entities.size} news items from Supabase")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading news feed with filter", e)
        }
}
    // COUNTRIES FUNCTIONS//

    suspend fun getCountries(forcedrefresh: Boolean): List<Country> = withContext(Dispatchers.IO) {
        try {
            if (forcedrefresh){
                countryDao.deleteAll()
                Log.d(TAG, "Cache cleared due to force refresh")
            }
            // First, try to get categories from the local cache
            val cachedCountries = countryDao.getAllCountries()
            if (cachedCountries.isNotEmpty()) {
                Log.d(TAG, "Countries loaded from cache: ${cachedCountries.size}")
                return@withContext cachedCountries
            }

            // If cache is empty, fetch from Supabase
            Log.d(TAG, "Fetching Countries from Supabase...")
            val response = client.postgrest["Countries"].select()
            val countries = response.decodeList<Country>()

            // Save the fetched categories into the cache
            if (countries.isNotEmpty()) {
                countryDao.insertAll(countries)
                Log.d(TAG, "Countries loaded from Supabase and cached: ${countries.size}")
            } else {
                Log.d(TAG, "No countries found on Supabase.")
            }

            countries
        } catch (e: Exception) {
            Log.e(TAG, "Error loading categories, attempting to use cache", e)
            // In case of a network error, still try to return from cache as a fallback
            try {
                countryDao.getAllCountries()
            } catch (dbError: Exception) {
                Log.e(TAG, "Error reading categories from cache as fallback", dbError)
                emptyList()
            }
        }
    }


suspend fun extractImageUrlFromArticle(url: String): String? {
    return withContext(Dispatchers.IO) { // Perform network operation on the IO thread
        try {
            // 1. Fetch and parse the HTML document
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36") // Be a good citizen
                .get()

            // 2. Look for the 'og:image' meta tag (most reliable)
            val ogImage = doc.select("meta[property=og:image]").attr("content")
            if (ogImage.isNotEmpty()) {
                Log.d("ImageExtractor", "Found image via og:image: $ogImage")
                return@withContext ogImage
            }

            // 3. Look for the 'twitter:image' meta tag
            val twitterImage = doc.select("meta[name=twitter:image]").attr("content")
            if (twitterImage.isNotEmpty()) {
                Log.d("ImageExtractor", "Found image via twitter:image: $twitterImage")
                return@withContext twitterImage
            }

            // 4. Look for the 'image_src' link tag
            val imageSrc = doc.select("link[rel=image_src]").attr("href")
            if (imageSrc.isNotEmpty()) {
                Log.d("ImageExtractor", "Found image via image_src: $imageSrc")
                return@withContext imageSrc
            }

            // 5. Fallback: Find the first large image inside the <article> or <main> tag
            val mainContent = doc.select("article, main").first()
            val firstImage = mainContent?.select("img[src]")?.firstOrNull()?.attr("abs:src")
            if (firstImage != null && firstImage.isNotEmpty()) {
                Log.d("ImageExtractor", "Found image via fallback (first img in article): $firstImage")
                return@withContext firstImage
            }

            // If no image is found
            Log.w("ImageExtractor", "Could not find a main image for URL: $url")
            null

        } catch (e: IOException) {
            Log.e("ImageExtractor", "Error fetching URL: $url", e)
            null
        } catch (e: Exception) {
            Log.e("ImageExtractor", "An unexpected error occurred", e)
            null
        }
    }
}


suspend fun extractTitle(url: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                .get()

            // 1. Try Open Graph title
            val ogTitle = doc.select("meta[property=og:title]").attr("content")
            if (ogTitle.isNotBlank()) return@withContext ogTitle

            // 2. Try Twitter title
            val twitterTitle = doc.select("meta[name=twitter:title]").attr("content")
            if (twitterTitle.isNotBlank()) return@withContext twitterTitle

            // 3. Fallback to standard <title> tag
            val docTitle = doc.title()
            if (docTitle.isNotBlank()) return@withContext docTitle

            null
        } catch (e: Exception) {
            Log.e("Error", "Error extracting title from $url", e)
            null
        }
    }
}

suspend fun extractAuthor(url: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                .get()

            // 1. Try standard author meta tags
            val author = doc.select("meta[name=author]").attr("content")
            if (author.isNotBlank()) return@withContext author

            // 2. Try Open Graph article author
            val ogAuthor = doc.select("meta[property=article:author]").attr("content")
            if (ogAuthor.isNotBlank()) return@withContext ogAuthor

            // 3. Common HTML patterns (e.g., classes named "author" or "byline")
            val htmlAuthor = doc.select("[class*=author], [id*=author], [class*=byline]").first()?.text()
            if (htmlAuthor != null && htmlAuthor.isNotBlank()) return@withContext htmlAuthor

            return@withContext "Anonimo"
        } catch (e: Exception) {
            Log.e("Error", "Error extracting author from $url", e)
            return@withContext "Anonimo"
        }
    }
}

suspend fun extractDescription(url: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                .get()

            // 1. Try Open Graph description
            val ogDesc = doc.select("meta[property=og:description]").attr("content")
            if (ogDesc.isNotBlank()) return@withContext ogDesc

            // 2. Try standard meta description
            val metaDesc = doc.select("meta[name=description]").attr("content")
            if (metaDesc.isNotBlank()) return@withContext metaDesc

            // 3. Try Twitter description
            val twitterDesc = doc.select("meta[name=twitter:description]").attr("content")
            if (twitterDesc.isNotBlank()) return@withContext twitterDesc

            // 4. Fallback: Get the first paragraph of the article body
            val firstParagraph = doc.select("article p, main p, .content p").first()?.text()
            if (firstParagraph != null && firstParagraph.isNotBlank()) {
                return@withContext if (firstParagraph.length > 200) firstParagraph.take(197) + "..." else firstParagraph
            }

            return@withContext "Anonimo"
        } catch (e: Exception) {
            Log.e("Error", "Error extracting description from $url", e)
            return@withContext "Anonimo"
        }
    }
}

suspend fun extractAuthorInstitution(url: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                .get()

            // 1. Try Open Graph site name (Very common for news outlets)
            val ogSiteName = doc.select("meta[property=og:site_name]").attr("content")
            if (ogSiteName.isNotBlank()) return@withContext ogSiteName

            // 2. Try the "publisher" meta tag
            val publisher = doc.select("meta[name=publisher]").attr("content")
            if (publisher.isNotBlank()) return@withContext publisher

            // 3. Try article:publisher (often a link to a FB page, but can be text)
            val articlePublisher = doc.select("meta[property=article:publisher]").attr("content")
            if (articlePublisher.isNotBlank()) {
                // If it's a URL, we return the site name part, otherwise the text
                return@withContext articlePublisher.substringAfterLast("/").replace("-", " ")
                    .capitalize()
            }

            // 4. Fallback: Search for common classes in the footer or header
            val brandName = doc.select(".brand, .logo-text, [class*='source']").first()?.text()
            if (brandName != null && brandName.isNotBlank()) return@withContext brandName

            return@withContext "Anonimo"
        } catch (e: Exception) {
            Log.e("Error", "Error extracting institution from $url", e)
            return@withContext "Anonimo"
        }
    }
  }
}




