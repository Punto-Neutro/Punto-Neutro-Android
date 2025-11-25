package com.example.sprint_2_kotlin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sprint_2_kotlin.model.data.AppDatabase
import com.example.sprint_2_kotlin.model.data.ReadHistoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Read History Screen
 * Manages the user's reading history data
 */
class ReadHistoryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val readHistoryDao = AppDatabase.getDatabase(application).readHistoryDao()

    // StateFlow for read history list
    private val _readHistory = MutableStateFlow<List<ReadHistoryEntity>>(emptyList())
    val readHistory: StateFlow<List<ReadHistoryEntity>> = _readHistory.asStateFlow()

    // StateFlow for loading state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // StateFlow for total count of articles read
    private val _readCount = MutableStateFlow(0)
    val readCount: StateFlow<Int> = _readCount.asStateFlow()

    init {
        loadReadHistory()
        loadReadCount()
    }

    /**
     * Load all read history from database
     * Observes changes in real-time using Flow
     */
    private fun loadReadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                readHistoryDao.getAllReadHistory().collect { historyList ->
                    _readHistory.value = historyList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _readHistory.value = emptyList()
                _isLoading.value = false
            }
        }
    }

    /**
     * Load total count of articles read
     * Used for ProfileScreen counter
     */
    private fun loadReadCount() {
        viewModelScope.launch {
            try {
                readHistoryDao.getReadCountFlow().collect { count ->
                    _readCount.value = count
                }
            } catch (e: Exception) {
                _readCount.value = 0
            }
        }
    }

    /**
     * Refresh the read history
     * Useful for pull-to-refresh functionality
     */
    fun refreshHistory() {
        loadReadHistory()
        loadReadCount()
    }

    /**
     * Delete a specific history entry
     */
    fun deleteHistoryEntry(historyId: Int) {
        viewModelScope.launch {
            try {
                readHistoryDao.deleteHistoryEntry(historyId)
                // History will auto-update via Flow
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    /**
     * Clear all read history
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                readHistoryDao.deleteAllHistory()
                // History will auto-update via Flow
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    /**
     * Get read count synchronously (for ProfileScreen)
     */
    suspend fun getReadCountSync(): Int {
        return try {
            readHistoryDao.getReadCount()
        } catch (e: Exception) {
            0
        }
    }
}