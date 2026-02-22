package com.example.sprint_2_kotlin.viewmodel

import android.R
import android.app.Application
import android.content.ContentValues
import android.util.Log
import androidx.compose.foundation.gestures.forEach
import androidx.compose.ui.geometry.isEmpty
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sprint_2_kotlin.model.data.AppDatabase
import com.example.sprint_2_kotlin.model.data.Category
import com.example.sprint_2_kotlin.model.data.Country
import com.example.sprint_2_kotlin.model.data.NewsItem
import com.example.sprint_2_kotlin.model.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import utils.NetworkMonitor

/**
 * NewsFeedViewModel with Cache Support, Category Filtering, and Network Detection
 *
 *  ENHANCED VERSION - Connection restored notification
 */
class NewsFeedViewModel(
    application: Application
) : AndroidViewModel(application) {

    // Pagination Process


    private var currentOffset = 0
    private val PAGE_SIZE = 20
    private var isLastPage = false

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> get() = _isConnected

    private val dao = AppDatabase.getDatabase(application).CommentDao()

    private val daonews = AppDatabase.getDatabase(application).newsItemDao()
    private val repository = Repository(application.applicationContext, dao, daonews)

    //  NEW: Network monitor to detect connection changes
    private val networkMonitor = NetworkMonitor(application.applicationContext)

    // EXISTING: News items state
    private val _newsItems = MutableStateFlow<List<NewsItem>>(emptyList())
    private val _paginatedNewsItems = MutableStateFlow<List<NewsItem>>(emptyList())

    val newsItems: StateFlow<List<NewsItem>> = _paginatedNewsItems

    // ============================================
    // Category filtering states
    // ============================================

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory

    // ============================================
    // Country filtering states
    // ============================================

    private val _countries = MutableStateFlow<List<Country>>(emptyList())
    val countries: StateFlow<List<Country>> = _countries

    // Holds the IDs of selected countries for filtering
    private val _selectedCountryIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedCountryIds: StateFlow<Set<Int>> = _selectedCountryIds

    // Holds the selected news scope: "All", "Local", or "International"
    private val _newsScope = MutableStateFlow("All")
    val newsScope: StateFlow<String> = _newsScope

    // In NewsFeedViewModel.kt, near the other state variables



    // This will hold the *complete* list of filtered items from the database
    private var _fullFilteredList = listOf<NewsItem>()

    // No change here, still needed for network fetches



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
        loadCategories(true)
        loadNewsItems()
        loadCountries(false)
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

            // Combine all flows that should trigger a re-filter
            combine(
                _searchQuery,
                _selectedCategory,
                _selectedCountryIds,
                _newsScope
            ) { query, category, countryIds, scope ->
                // This tuple holds the latest values from all filters
                Triple(query, category, Pair(countryIds, scope))
            }.collectLatest { (query, category, filterPair) ->
                val (countryIds, scope) = filterPair

                Log.d(TAG, "🔄 FILTER TRIGGER: Query='$query', Category=${category?.name ?: "All"}, Countries=${countryIds}, Scope='$scope'")

                val newsFlow = if (query.isNotBlank()) {
                    repository.searchNewsItemsCached(query)
                } else {
                    repository.getNewsFeedCached()
                }



                newsFlow.catch { exception ->
                    Log.e(TAG, "❌ Error in Flow observation", exception)
                }.collect { allItems ->
                    Log.d(TAG, "📬 Flow emitted ${allItems.size} items before filtering.")

                    // 1. Apply category filter first
                    val categoryFilteredItems = if (category != null) {
                        Log.d(TAG, "==> Applying CATEGORY filter: '${category.name}'")
                        allItems.filter { it.category_id == category.category_id }
                    } else {
                        Log.d(TAG, "==> No category filter applied.")
                        allItems
                    }

                    // For now, we'll keep the hardcoded user country ID for 'Local'.
                    val userCountryId = 183 // Example: United States

                    // 2. Apply country/scope filter to the result of the category filter
                    val finalFilteredItems = when {
                        // Priority 1: Specific countries are selected
                        countryIds.isNotEmpty() -> {
                            Log.d(TAG, "==> Applying MULTI-COUNTRY filter for IDs: $countryIds")
                            categoryFilteredItems.filter { item ->
                                val condition = item.country_id in countryIds
                                // LOG THE VALUE OF it.country_id HERE
                                Log.d(TAG, "  - Checking Item #${item.news_item_id}: its country_id is ${item.country_id}. Condition '${item.country_id} in $countryIds' is $condition")
                                condition
                            }
                        }
                        // Priority 2: Scope is "Local"
                        scope == "Local" -> {
                            Log.d(TAG, "==> Applying 'LOCAL' news filter (country_id == $userCountryId)")
                            categoryFilteredItems.filter { item ->
                                val condition = item.country_id == userCountryId
                                // LOG THE VALUE OF it.country_id HERE
                                Log.d(TAG, "  - Checking Item #${item.news_item_id}: its country_id is ${item.country_id}. Condition '${item.country_id} == $userCountryId' is $condition")
                                condition
                            }
                        }
                        // Priority 3: Scope is "International"
                        scope == "International" -> {
                            Log.d(TAG, "==> Applying 'INTERNATIONAL' news filter (country_id != $userCountryId)")
                            categoryFilteredItems.filter { item ->
                                val condition = item.country_id != userCountryId
                                // LOG THE VALUE OF it.country_id HERE
                                Log.d(TAG, "  - Checking Item #${item.news_item_id}: its country_id is ${item.country_id}. Condition '${item.country_id} != $userCountryId' is $condition")
                                condition
                            }
                        }
                        // Default: Scope is "All" and no countries selected
                        else -> {
                            Log.d(TAG, "==> No country or scope filter applied.")
                            categoryFilteredItems
                        }
                    }

                    _newsItems.value = finalFilteredItems
                    Log.d(TAG, "✅ Flow finished. Final list size: ${finalFilteredItems.size}")

                    // ✅ THE FIX: Store the complete filtered list and display only the first page
                    _fullFilteredList = finalFilteredItems
                    _paginatedNewsItems.value = _fullFilteredList

                    // Update UI states based on the final list
                    _noSearchResults.value = query.isNotBlank() && finalFilteredItems.isEmpty()
                    updateCacheStatus()
                    if (finalFilteredItems.isNotEmpty() && _isOnline.value && _errorMessage.value != null) {
                        _errorMessage.value = null
                    }
                }
            }
        }
    }


    // ============================================
    // CATEGORY FUNCTIONS
    // ============================================

    private fun loadCategories(forcedRefresh: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading categories...")
                val categoriesList = repository.getCategories(forcedRefresh )
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
                resetAndReload()
                loadNewsItems(forceRefresh = true)
            } catch (e: Exception) {
                Log.e(TAG, "Error selecting category", e)
            }
        }
    }


    fun loadCountries(forcedRefresh: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(ContentValues.TAG, "Loading countries...")
                val categoriesList = repository.getCountries(forcedRefresh )
                _countries.value = categoriesList
                Log.d(ContentValues.TAG, "Countries loaded: ${categoriesList.size}")
            } catch (e: Exception) {
                Log.e(ContentValues.TAG, "Error loading countries", e)
                _countries.value = emptyList()
            }
        }
    }


    fun clearCategoryFilter() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Clearing category filter...")
                _selectedCategory.value = null
                loadNewsItems(forceRefresh = true)
                resetAndReload()
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing category filter", e)
            }
        }
    }

    // A helper function to avoid repeating code

    private fun resetAndReload() {
        _paginatedNewsItems.value = emptyList() // Clear the UI list
        _fullFilteredList = emptyList()         // Clear the complete cached list
        currentOffset = 0                       // Reset network offset
        isLastPage = false
        // The call to loadNewsItems(forceRefresh = true) is correct as it will
        // refresh the DB and trigger observeNewsFeedFlow to repopulate everything.
        loadNewsItems(forceRefresh = true)
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

    // NewsFeedViewModel.kt

    fun loadNewsItems(forceRefresh: Boolean = false) {
        // If we are already loading, don't trigger another one
        if (_isLoading.value || _isRefreshing.value) return

        viewModelScope.launch {
            try {
                // Determine if we show the big loader or the swipe-refresh spinner
                if (forceRefresh) _isRefreshing.value = true else _isLoading.value = true

                val categoryId = _selectedCategory.value?.category_id

                // This calls Supabase and updates the Room Database
                // The Flow in observeNewsFeedFlow will pick up the changes automatically
                repository.loadNewsFeedWithFilter(
                    categoryId = categoryId,
                    forceRefresh = forceRefresh
                )

                // If we successfully fetched, reset pagination offsets
                currentOffset = 20
                isLastPage = false

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching news", e)
                _errorMessage.value = "Failed to update news feed"
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }



    // In NewsFeedViewModel.kt

    // In NewsFeedViewModel.kt

    // In NewsFeedViewModel.kt

    fun loadNextPage() {
        if (_isLoading.value || isLastPage) return

        viewModelScope.launch {
            _isLoading.value = true
            Log.d(TAG, "🔌 Loading next page...")

            val currentLoadedCount = _paginatedNewsItems.value.size

            // --- Client-Side Pagination ---
            // Check if there are more items in our cached full list
            if (currentLoadedCount < _fullFilteredList.size) {
                Log.d(TAG, "   -> Loading next page from local cache.")
                val nextPageIndex = currentLoadedCount + PAGE_SIZE
                _paginatedNewsItems.value = _fullFilteredList.take(nextPageIndex)
                _isLoading.value = false
                return@launch // We're done, no need for network call
            }

            // --- Server-Side Pagination ---
            // If we've exhausted the local list, fetch from network
            Log.d(TAG, "   -> Local cache exhausted. Fetching from network (offset: $currentOffset)...")
            try {
                // Fetch the next page *with ALL the current filters applied*
                val newItems = repository.getNewsItems(
                    pageSize = PAGE_SIZE,
                    startRow = currentOffset, // Use the network offset here
                    categoryFilter = _selectedCategory.value,
                    countryIdsFilter = _selectedCountryIds.value,
                    scopeFilter = _newsScope.value
                )

                if (newItems.isNotEmpty()) {
                    // IMPORTANT: Add to both the full list and the visible list
                    currentOffset += newItems.size
                    isLastPage = newItems.size < PAGE_SIZE
                    repository.cacheNewsItems(newItems)
                } else {
                    isLastPage = true
                    Log.d(TAG, "🏁 Reached the last page (no more items from network).")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading next page from network", e)
                _errorMessage.value = "Failed to load more news."
            } finally {
                _isLoading.value = false
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

        currentOffset = 0
        isLastPage = false
        _newsItems.value = emptyList()

        loadNewsItems(forceRefresh = true)
        loadCategories( true)
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

    fun AddNews(Url: String, Category_id: Int, country_id: Int, onSuccess: () -> Unit, onError: (Throwable) -> Unit,onWait: ()-> Unit ) {
        viewModelScope.launch {

            try {
            val response = repository.addNews( url = Url,  category_id = Category_id, country = country_id)

            if (response == 0){
                withContext(Dispatchers.Main){
                    onSuccess()
                    refreshNewsFeed()
                    loadNewsItems()
                }

            }else if (response == 2) {
                Log.w(ContentValues.TAG,"Se activo el encolamiento")
                withContext(Dispatchers.Main){
                    onWait()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main){
                onError(e)
            }

        }

        }

    }


    fun startSync(networkMonitor: NetworkMonitor) {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->
                _isConnected.value = connected
                if (connected) {
                    repository.syncPendingNews()
                    repository.clearCache()


                }
            }
        }
    }

    fun startNetworkObserver(networkMonitor: NetworkMonitor) {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->
                if (connected) {
                    startSync(networkMonitor)

                }
            }
        }
    }

    //==================================================================
    // COUNTRIES FUNCTIONS FOR FILTERING
    //==================================================================


    fun onCountrySelected(countryId: Int, isSelected: Boolean) {
        val currentSelection = _selectedCountryIds.value.toMutableSet()
        if (isSelected) {
            currentSelection.add(countryId)
        } else {
            currentSelection.remove(countryId)
        }
        _selectedCountryIds.value = currentSelection
        // When country selection changes, reset scope to "All"
        _newsScope.value = "All"
    }

    fun onNewsScopeSelected(scope: String) {
        _newsScope.value = scope
        // When scope changes, clear specific country selections
        _selectedCountryIds.value = emptySet()
    }

    fun applyFilters() {
        viewModelScope.launch {
            // This will trigger the flow observation to re-filter the news
            // We just need to trigger a change, the logic is in the collector
            _newsItems.value = _newsItems.value.map { it.copy() } // Trigger recomposition
            Log.d(TAG, "Applying filters. Scope: ${_newsScope.value}, Countries: ${_selectedCountryIds.value}")
        }
    }

    fun clearAllFilters() {
        _selectedCountryIds.value = emptySet()
        _newsScope.value = "All"
        applyFilters()
    }




}















