package com.example.sprint_2_kotlin.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sprint_2_kotlin.model.data.AppDatabase
import com.example.sprint_2_kotlin.model.data.Category
import com.example.sprint_2_kotlin.model.data.NewsItem
import com.example.sprint_2_kotlin.model.repository.Repository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import utils.NetworkMonitor

/**
 * NewsFeedViewModel with Cache Support, Category Filtering, and Network Detection
 *
 *  ENHANCED VERSION - Connection restored notification
 */
class NewsFeedViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).CommentDao()
    private val repository = Repository(application.applicationContext, dao)

    //  NEW: Network monitor to detect connection changes
    private val networkMonitor = NetworkMonitor(application.applicationContext)

    // EXISTING: News items state
    private val _newsItems = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsItems: StateFlow<List<NewsItem>> = _newsItems

    // ============================================
    // Category filtering states
    // ============================================

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory

    // ============================================
    // Search states
    // ============================================

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // UPDATE :3 : Estado para detectar búsquedas sin resultados
    private val _noSearchResults = MutableStateFlow(false)
    val noSearchResults: StateFlow<Boolean> = _noSearchResults

    // ============================================
    // Cache-related states
    // ============================================

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _cacheStatus = MutableStateFlow<String>("")
    val cacheStatus: StateFlow<String> = _cacheStatus

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // ============================================
    //  NEW: Network connection states
    // ============================================

    /**
     * Tracks if connection was just restored
     * Shows green banner when true
     */
    private val _connectionRestored = MutableStateFlow(false)
    val connectionRestored: StateFlow<Boolean> = _connectionRestored

    /**
     * Current network status
     */
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline

    /**
     * Track previous connection state to detect restoration
     */
    private var wasOffline = false

    companion object {
        private const val TAG = "NewsFeedViewModel"
    }

    init {
        Log.d(TAG, "🚀 NewsFeedViewModel initialized")

        //  Start monitoring network changes
        observeNetworkChanges()

        // Start observing the Flow continuously FIRST
        observeNewsFeedFlow()

        // Load categories and initial news
        loadCategories()
        loadNewsItems()
    }

    // ============================================
    //  NEW: NETWORK MONITORING
    // ============================================

    /**
     *  NEW METHOD: Observes network connectivity changes
     *
     * Detects when connection is restored and triggers:
     * 1. Green banner notification
     * 2. Automatic refresh of news
     * 3. Auto-hide banner after 3 seconds
     */
    private fun observeNetworkChanges() {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { isConnected ->
                _isOnline.value = isConnected

                Log.d(TAG, "📡 Network status changed: ${if (isConnected) "ONLINE" else "OFFLINE"}")

                if (isConnected && wasOffline) {
                    //  Connection was restored!
                    Log.d(TAG, "🌐 Connection RESTORED - triggering auto-refresh")

                    // Show green banner
                    _connectionRestored.value = true

                    // Clear error message
                    _errorMessage.value = null

                    // Auto-refresh news feed
                    refreshNewsFeed()

                    // Auto-hide banner after 3 seconds
                    viewModelScope.launch {
                        delay(3000)
                        _connectionRestored.value = false
                        Log.d(TAG, "✅ Connection restored banner auto-hidden")
                    }
                }

                // Update offline tracking
                wasOffline = !isConnected

                // Update error message when going offline
                if (!isConnected && _newsItems.value.isNotEmpty()) {
                    _errorMessage.value = "No internet connection. Showing cached news."
                }
            }
        }
    }

    // ============================================
    // CONTINUOUS FLOW OBSERVATION
    // ============================================

    /**
     * Observes cache Flow continuously
     */
    /**
     * Observes cache Flow continuously
     * Now supports search queries combined with category filtering
     */
    /**
     * Observes cache Flow continuously
     * Now supports search queries combined with category filtering
     */
    private fun observeNewsFeedFlow() {
        viewModelScope.launch {
            Log.d(TAG, "📡 Starting continuous Flow observation with search support...")

            // Combinar searchQuery y selectedCategory para reaccionar a cambios en ambos
            combine(
                _searchQuery,
                _selectedCategory
            ) { query, category ->
                Pair(query, category)
            }.collectLatest { (query, category) ->

                Log.d(TAG, "🔍 Search/Filter changed - query: '$query', category: ${category?.name ?: "all"}")

                // Obtener el Flow apropiado según si hay búsqueda o no
                val newsFlow = if (query.isNotBlank()) {
                    repository.searchNewsItemsCached(query)
                } else {
                    repository.getNewsFeedCached()
                }

                newsFlow.catch { exception ->
                    Log.e(TAG, " Error in Flow observation", exception)
                }.collect { items ->
                    // Aplicar filtro de categoría si está seleccionado
                    val filteredItems = if (category != null) {
                        items.filter { it.category_id == category.category_id }
                    } else {
                        items
                    }

                    _newsItems.value = filteredItems
                    Log.d(TAG, " Flow emitted: ${filteredItems.size} items (query: '$query', category: ${category?.name ?: "all"})")

                    // UPDATE: Detectar si hay búsqueda activa sin resultados
                    _noSearchResults.value = query.isNotBlank() && filteredItems.isEmpty()

                    updateCacheStatus()

                    // Clear error message if we have data and are online
                    if (filteredItems.isNotEmpty() && _isOnline.value && _errorMessage.value != null) {
                        _errorMessage.value = null
                    }
                }
            }
        }
    }

    // ============================================
    // CATEGORY FUNCTIONS
    // ============================================

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading categories...")
                val categoriesList = repository.getCategories()
                _categories.value = categoriesList
                Log.d(TAG, "Categories loaded: ${categoriesList.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading categories", e)
                _categories.value = emptyList()
            }
        }
    }

    fun selectCategory(category: Category?) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Selecting category: ${category?.name ?: "All"}")
                _selectedCategory.value = category
                loadNewsItems(forceRefresh = true)
            } catch (e: Exception) {
                Log.e(TAG, "Error selecting category", e)
            }
        }
    }

    fun clearCategoryFilter() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Clearing category filter...")
                _selectedCategory.value = null
                loadNewsItems(forceRefresh = true)
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing category filter", e)
            }
        }
    }
// ============================================
    // SEARCH FUNCTIONS
    // ============================================

    /**
     * Update search query
     * Automatically triggers Flow to refresh results
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        Log.d(TAG, "🔍 Search query updated: '$query'")
    }

    /**
     * Clear search query
     */
    fun clearSearch() {
        _searchQuery.value = ""
        Log.d(TAG, "🔍 Search cleared")
    }

    // ============================================
    // LOAD NEWS ITEMS
    // ============================================

    private fun loadNewsItems(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                if (forceRefresh) {
                    _isRefreshing.value = true
                    Log.d(TAG, "🔄 Force refresh triggered")
                } else {
                    _isLoading.value = true
                    Log.d(TAG, "🔄 Loading news items")
                }

                _errorMessage.value = null

                val categoryId = _selectedCategory.value?.category_id
                Log.d(TAG, "Loading with categoryId: $categoryId, forceRefresh: $forceRefresh")

                repository.loadNewsFeedWithFilter(
                    categoryId = categoryId,
                    forceRefresh = forceRefresh
                )

                Log.d(TAG, "✅ News feed loaded successfully")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading news items", e)
                e.printStackTrace()

                if (_newsItems.value.isEmpty()) {
                    _errorMessage.value = "No internet connection. Unable to load news."
                    Log.w(TAG, "⚠️ No cached data available while offline")
                } else {
                    _errorMessage.value = "Could not refresh. Showing cached news."
                    Log.i(TAG, "ℹ️ Using ${_newsItems.value.size} cached items while offline")
                }

            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
                Log.d(TAG, "🏁 Loading operation completed")
            }
        }
    }

    fun refreshNewsFeed() {
        Log.d(TAG, "🔄 Manual refresh requested")

        // Limpiar búsqueda al hacer refresh (UX más clara)
        if (_searchQuery.value.isNotBlank()) {
            Log.d(TAG, "🔍 Clearing search on refresh")
            _searchQuery.value = ""
        }

        loadNewsItems(forceRefresh = true)
    }

    private suspend fun updateCacheStatus() {
        try {
            val cachedCount = repository.getCachedItemsCount()
            _cacheStatus.value = if (cachedCount > 0) {
                "📦 Cached: $cachedCount items"
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating cache status", e)
            _cacheStatus.value = ""
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                repository.clearCache()
                // Limpiar search cache solo si no hay búsqueda activa
                if (_searchQuery.value.isBlank()) {
                    repository.clearSearchCache()
                }
                _cacheStatus.value = "Cache cleared"
                loadNewsItems(forceRefresh = true)
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing cache", e)
            }
        }
    }

    /**
     * Clear search query cache
     * Useful for testing or when search results seem outdated
     */
    fun clearSearchCache() {
        viewModelScope.launch {
            try {
                repository.clearSearchCache()
                Log.d(TAG, "Search query cache cleared")

                // Re-trigger search if there's an active query
                if (_searchQuery.value.isNotBlank()) {
                    val currentQuery = _searchQuery.value
                    _searchQuery.value = ""
                    delay(100)  // Small delay to trigger Flow
                    _searchQuery.value = currentQuery
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing search cache", e)
            }
        }
    }

    /**
     * Get search cache statistics for debugging/analytics
     */
    suspend fun getSearchCacheStats(): String {
        return try {
            val stats = repository.getSearchCacheStats()
            "Search Cache: ${stats.totalQueries}/${stats.maxSize} queries (TTL: ${stats.ttlHours}h)"
        } catch (e: Exception) {
            "Search Cache: unavailable"
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Manually dismiss connection restored banner gg
     */
    fun dismissConnectionRestored() {
        _connectionRestored.value = false
        Log.d(TAG, "Connection restored banner dismissed manually")
    }

    fun getCategoryLabel(categoryId: Int): String = when (categoryId) {
        1 -> "Politics"
        2 -> "Sports"
        3 -> "Science"
        4 -> "Economics"
        5 -> "Business"
        6 -> "Climate"
        7 -> "Conflict"
        8 -> "Local"
        else -> "General"
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 NewsFeedViewModel cleared")
    }
}












