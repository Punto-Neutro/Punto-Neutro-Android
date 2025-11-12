package com.example.sprint_2_kotlin.model.repository

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.example.sprint_2_kotlin.model.data.*
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.w3c.dom.Comment
import utils.NetworkMonitor

/**
 * Repository with Cache-First Strategy for News Feed
 * Maintains all existing Supabase functionality
 */
class Repository(private val context: Context,private val daocomment: CommentDao ) {

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

    // Cache expiration time: 30 minutes in milliseconds
    private val CACHE_EXPIRATION_TIME = 30 * 60 * 1000L

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

    suspend fun signUp(email: String, password: String): Boolean {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
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

// ============================================
// BUSINESS QUESTION #4: RATING DISTRIBUTION (SUPABASE REAL)
// ============================================

    suspend fun getRatingDistributionByCategory(): Result<RatingDistributionData> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching real rating distribution from Supabase...")

                // 1. Obtener todas las categorías
                val categories = getCategories()
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
    suspend fun getCategories(): List<Category> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching categories from Supabase...")
            val response = client.postgrest["categories"].select()
            val categories = response.decodeList<Category>()
            Log.d(TAG, "Categories loaded: ${categories.size}")
            categories
        } catch (e: Exception) {
            Log.e(TAG, "Error loading categories", e)
            emptyList()
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
            }

            val entities = freshNewsItems.map { it.toEntity() }
            newsItemDao.insertAllNewsItems(entities)

            Log.d(TAG, "Successfully cached ${entities.size} news items from Supabase")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading news feed with filter", e)
        }
}

}