package com.example.sprint_2_kotlin.viewmodel

import android.app.Application
import android.content.ContentValues
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sprint_2_kotlin.model.data.AppDatabase
import com.example.sprint_2_kotlin.model.data.Category
import com.example.sprint_2_kotlin.model.data.NewsItem
import com.example.sprint_2_kotlin.model.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import utils.NetworkMonitor

/**
 * NewsFeedViewModel with Cache Support, Category Filtering, and Network Detection
 *
 * ✅ ENHANCED VERSION - Connection restored notification
 */
class NewsFeedViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).CommentDao()

    private val daonews = AppDatabase.getDatabase(application).newsItemDao()
    private val repository = Repository(application.applicationContext, dao, daonews)

    // ✅ NEW: Network monitor to detect connection changes
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
    // ✅ NEW: Network connection states
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

        // ✅ Start monitoring network changes
        observeNetworkChanges()

        // Start observing the Flow continuously FIRST
        observeNewsFeedFlow()

        // Load categories and initial news
        loadCategories()
        loadNewsItems()
    }

    // ============================================
    // ✅ NEW: NETWORK MONITORING
    // ============================================

    /**
     * ✅ NEW METHOD: Observes network connectivity changes
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
                    // ✅ Connection was restored!
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
    private fun observeNewsFeedFlow() {
        viewModelScope.launch {
            Log.d(TAG, "📡 Starting continuous Flow observation...")

            repository.getNewsFeedCached()
                .catch { exception ->
                    Log.e(TAG, "❌ Error in Flow observation", exception)
                }
                .collect { cachedItems ->
                    // Apply category filter if selected
                    val categoryId = _selectedCategory.value?.category_id
                    val filteredItems = if (categoryId != null) {
                        cachedItems.filter { it.category_id == categoryId }
                    } else {
                        cachedItems
                    }

                    _newsItems.value = filteredItems
                    Log.d(TAG, "✅ Flow emitted: ${filteredItems.size} items (category: ${categoryId ?: "all"})")

                    updateCacheStatus()

                    // Clear error message if we have data and are online
                    if (filteredItems.isNotEmpty() && _isOnline.value && _errorMessage.value != null) {
                        _errorMessage.value = null
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
                _cacheStatus.value = "Cache cleared"
                loadNewsItems(forceRefresh = true)
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing cache", e)
            }
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

    fun AddNews(title:String, Url: String, Author_type: String, Author_institution: String, Description: String, Category_id: Int, onSuccess: () -> Unit, onError: (Throwable) -> Unit,onWait: ()-> Unit ) {
        viewModelScope.launch {

            try {
            val response = repository.addNews(title = title, url = Url, author_type = Author_type, author_institution = Author_institution, description = Description, category_id = Category_id)

            if (response == 0){
                withContext(Dispatchers.Main){
                    onSuccess()
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


}















