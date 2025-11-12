package com.example.sprint_2_kotlin.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sprint_2_kotlin.model.data.AppDatabase
import com.example.sprint_2_kotlin.model.data.RatingDistributionData
import com.example.sprint_2_kotlin.model.network.NetworkStatusTracker
import com.example.sprint_2_kotlin.model.repository.Repository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel para Business Question #4
 * Maneja el estado y lógica de distribución de ratings por categoría
 */
class RatingDistributionViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).CommentDao()
    private val repository = Repository(application.applicationContext, dao)

    // Estados
    var distributionData by mutableStateOf<RatingDistributionData?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadDistributionData()
    }

    /**
     * Cargar datos de distribución desde el repository
     */
    fun loadDistributionData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val result = repository.getRatingDistributionByCategory()

                result.onSuccess { data ->
                    distributionData = data
                    isLoading = false
                }.onFailure { error ->
                    errorMessage = "Error loading data: ${error.message}"
                    isLoading = false
                }
            } catch (e: Exception) {
                errorMessage = "Unexpected error: ${e.message}"
                isLoading = false
            }
        }
    }

    /**
     * Refrescar datos
     */
    fun refresh() {
        loadDistributionData()
    }

    /**
     * Obtener color para categoría usando category_id (mismo esquema que NewsFeedScreen)
     */
    fun getCategoryColor(categoryId: Int): androidx.compose.ui.graphics.Color {
        return when (categoryId) {
            1 -> androidx.compose.ui.graphics.Color(0xFF2196F3)  // Politics
            2 -> androidx.compose.ui.graphics.Color(0xFFE91E63)  // Sports
            3 -> androidx.compose.ui.graphics.Color(0xFF4CAF50)  // Science
            4 -> androidx.compose.ui.graphics.Color(0xFFFF5722)  // Economics
            5 -> androidx.compose.ui.graphics.Color(0xFF9C27B0)  // Business
            6 -> androidx.compose.ui.graphics.Color(0xFF00BCD4)  // Climate
            7 -> androidx.compose.ui.graphics.Color(0xFFFF9800)  // Conflict
            8 -> androidx.compose.ui.graphics.Color(0xFF607D8B)  // Local
            else -> androidx.compose.ui.graphics.Color(0xFF757575)
        }
    }

    /**
     * Formatear valor de reliability (0.0 - 1.0) como porcentaje
     */
    fun formatPercentage(value: Double): String {
        val percentage = (value * 100).toInt()
        return "$percentage%"
    }

    /**
     * Obtener emoji para nivel de reliability (escala 0.0 - 1.0)
     */
    fun getReliabilityEmoji(avgReliability: Double): String {
        return when {
            avgReliability >= 0.80 -> "🌟" // Excellent (80%+)
            avgReliability >= 0.60 -> "✅" // Good (60-79%)
            avgReliability >= 0.40 -> "⚠️" // Moderate (40-59%)
            else -> "❌" // Poor (below 40%)
        }
    }
}