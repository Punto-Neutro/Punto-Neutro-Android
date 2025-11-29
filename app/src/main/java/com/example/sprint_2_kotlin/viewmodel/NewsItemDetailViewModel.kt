package com.example.sprint_2_kotlin.viewmodel

import android.app.Application
import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sprint_2_kotlin.model.data.AppDatabase
import com.example.sprint_2_kotlin.model.data.NewsItem
import com.example.sprint_2_kotlin.model.data.RatingItem
import com.example.sprint_2_kotlin.model.data.UserProfile
import com.example.sprint_2_kotlin.model.data.toReadHistoryEntity
import com.example.sprint_2_kotlin.model.network.NetworkStatusTracker
import com.example.sprint_2_kotlin.model.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import utils.NetworkMonitor

/**
 * NewsItemDetailViewModel
 *
 * CHANGE: Now extends AndroidViewModel to pass context to Repository
 */
class NewsItemDetailViewModel(
    application: Application // update: ahora recibe Application
) : AndroidViewModel(application) { //  update: extiende AndroidViewModel

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> get() = _isConnected
    // update: Repository ahora recibe context
    private val dao = AppDatabase.getDatabase(application).CommentDao()
    private val daonews = AppDatabase.getDatabase(application).newsItemDao()
    private val readHistoryDao = AppDatabase.getDatabase(application).readHistoryDao()  // updated
    // update: Repository ahora recibe context
    private val repository = Repository(application.applicationContext, daocomment = dao, daonewsitem = daonews)
    private val _newsItem = MutableStateFlow<NewsItem?>(null)
    val newsItem: StateFlow<NewsItem?> = _newsItem.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)

    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _ratings = MutableStateFlow<List<RatingItem>>(emptyList())
    val ratings: StateFlow<List<RatingItem>> = _ratings.asStateFlow()

    // Keep the old function for backward compatibility if needed
    fun loadNewsItem(newsItem: NewsItem) {
        _newsItem.value = newsItem
        loadRatings(newsItem.news_item_id)
    }

    // Load news item by ID
    fun loadNewsItemById(newsItemId: Int) {
        viewModelScope.launch {
            try {
                // Fetch the news item from repository
                val item = repository.getNewsItemById(newsItemId)
                _newsItem.value = item

                // Load ratings for this news item
                loadRatings(newsItemId)
            } catch (e: Exception) {
                // Handle error - you might want to add error state
                _newsItem.value = null
            }
        }
    }

    private fun loadRatings(newsItemId: Int) {
        viewModelScope.launch {
            try {
                val ratingsList = repository.getRatingsForNewsItem(newsItemId)
                _ratings.value = ratingsList
            } catch (e: Exception) {
                _ratings.value = emptyList()
            }
        }
    }

    /**
     *  NUEVA FUNCION: Register read history
     * Only registers if the news item hasn't been read before
     */
    fun registerReadHistory(newsItem: NewsItem) {
        viewModelScope.launch {
            try {
                // Check if already read
                val alreadyRead = readHistoryDao.isNewsItemRead(newsItem.news_item_id)

                if (!alreadyRead) {
                    // Convert NewsItem to ReadHistoryEntity and insert
                    val historyEntity = newsItem.toReadHistoryEntity()
                    readHistoryDao.insertReadHistory(historyEntity)
                    Log.d(TAG, "Read history registered for news item: ${newsItem.news_item_id}")
                } else {
                    Log.d(TAG, "News item already in read history: ${newsItem.news_item_id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registering read history: ${e.message}")
            }
        }
    }

    fun addComment(userProfileId: Int, comment:String, newsItemId: Int, rating: Double, onSuccess: () -> Unit, onError: (Throwable) -> Unit,onWait: ()-> Unit )
    {
        viewModelScope.launch {
            try {
                val response = repository.addNewComments(userProfileId,newsItemId, comment = comment, rating = rating, completed = false)

                if (response == 0){
                    withContext(Dispatchers.Main){
                        onSuccess()
                        loadNewsItemById(newsItemId)
                    }

                }else if (response == 2) {
                    Log.w(TAG,"Se activo el encolamiento")
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

    fun startSync(networkMonitor: NetworkMonitor,newsItemid:Int) {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->
                _isConnected.value = connected
                if (connected) {repository.syncPendingComments()
                    repository.syncPendingNews()
                    repository.clearCache()
                    loadNewsItemById(newsItemid)}




            }
        }
    }

    fun startNetworkObserver(networkMonitor: NetworkMonitor, newsitemId: Int) {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->
                if (connected) {
                    startSync(networkMonitor,newsitemId)

                }
            }
        }
    }
}