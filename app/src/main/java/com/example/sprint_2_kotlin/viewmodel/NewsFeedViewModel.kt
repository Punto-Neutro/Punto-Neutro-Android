package com.example.sprint_2_kotlin.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sprint_2_kotlin.model.data.AppDatabase
import com.example.sprint_2_kotlin.model.data.Category
import com.example.sprint_2_kotlin.model.data.NewsItem
import com.example.sprint_2_kotlin.model.repository.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * NewsFeedViewModel with Cache Support and Category Filtering
 */
class NewsFeedViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).CommentDao()
    private val repository = Repository(application.applicationContext, dao)

    // EXISTING: News items state
    private val _newsItems = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsItems: StateFlow<List<NewsItem>> = _newsItems

    // ============================================
    // NEW: Category filtering states
    // ============================================

    // Available categories
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    // Selected category (null = show all)
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

    companion object {
        private const val TAG = "NewsFeedViewModel"
    }

    init {
        loadCategories()
        loadNewsItems()
    }

    /**
     * NEW: Load available categories from Supabase
     */
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

    /**
     * NEW: Select a category to filter news items
     * Pass null to show all news items
     */
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

    /**
     * NEW: Clear category filter (show all news)
     */
    fun clearCategoryFilter() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Clearing category filter...")
                _selectedCategory.value = null
                // Recargar todas las noticias inmediatamente
                loadNewsItems(forceRefresh = true)
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing category filter", e)
            }
        }
    }

    /**
     * UPDATED: Load news items with optional category filter
     */
    private fun loadNewsItems(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                if (forceRefresh) {
                    _isRefreshing.value = true
                } else {
                    _isLoading.value = true
                }

                val categoryId = _selectedCategory.value?.category_id

                Log.d(TAG, "Loading news items - categoryId: $categoryId, forceRefresh: $forceRefresh")

                // Si estamos forzando refresh o el filtro cambió, cargar desde red
                if (forceRefresh) {
                    // Cargar desde la red según el filtro
                    val freshItems = if (categoryId != null) {
                        repository.getNewsItemsByCategory(categoryId)
                    } else {
                        repository.getNewsItems()
                    }
                    _newsItems.value = freshItems
                    Log.d(TAG, "Fresh news items loaded: ${freshItems.size} items")
                } else {
                    // Usar caché y filtrar localmente
                    val cachedItems = repository.getNewsFeedCached().first()

                    val filteredItems = if (categoryId != null) {
                        cachedItems.filter { it.category_id == categoryId }
                    } else {
                        cachedItems
                    }

                    _newsItems.value = filteredItems
                    Log.d(TAG, "Cached news items filtered: ${filteredItems.size} items")
                }

                updateCacheStatus()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading news items", e)
                e.printStackTrace()
                _newsItems.value = emptyList()
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }

        // Cargar datos en caché de manera paralela
        viewModelScope.launch {
            try {
                val categoryId = _selectedCategory.value?.category_id
                repository.loadNewsFeedWithFilter(
                    categoryId = categoryId,
                    forceRefresh = forceRefresh
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading cached news feed", e)
            }
        }
    }

    /**
     * Refresh news feed (for pull-to-refresh)
     */
    fun refreshNewsFeed() {
        Log.d(TAG, "Refreshing news feed...")
        loadNewsItems(forceRefresh = true)
    }

    /**
     * Update cache status for UI display
     */
    private suspend fun updateCacheStatus() {
        try {
            val cachedCount = repository.getCachedItemsCount()
            _cacheStatus.value = if (cachedCount > 0) {
                " Cached: $cachedCount items"
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating cache status", e)
            _cacheStatus.value = ""
        }
    }

    /**
     * Clear cache manually
     */
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

    /**
     * Get category label by ID
     */
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
}