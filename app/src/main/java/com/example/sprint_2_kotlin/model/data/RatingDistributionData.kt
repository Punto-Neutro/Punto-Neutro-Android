package com.example.sprint_2_kotlin.model.data

/**
 * Data class para representar la distribución de reliability ratings por categoría
 * Usado para Business Question #4
 *
 * Los reliability scores están en escala 0.0 - 1.0 (0% - 100%)
 */
data class CategoryRatingDistribution(
    val categoryId: Int,             // ID de la categoría
    val category: String,            // Nombre de la categoría
    val avgReliabilityScore: Double, // Promedio de reliability (0.0 - 1.0)
    val ratingCount: Int,            // Total de ratings en esta categoría

    // Distribución por rangos de porcentaje
    val range0_20: Int = 0,      // Ratings entre 0% - 20%
    val range21_40: Int = 0,     // Ratings entre 21% - 40%
    val range41_60: Int = 0,     // Ratings entre 41% - 60%
    val range61_80: Int = 0,     // Ratings entre 61% - 80%
    val range81_100: Int = 0     // Ratings entre 81% - 100%
)

/**
 * Clase para estadísticas generales de reliability ratings
 */
data class RatingStatistics(
    val totalRatings: Int,               // Total de ratings en todas las categorías
    val avgReliability: Double,          // Promedio global de reliability (0.0 - 1.0)
    val mostRatedCategory: String,       // Categoría con más ratings
    val mostReliableCategory: String,    // Categoría con mayor promedio de reliability
    val leastReliableCategory: String    // Categoría con menor promedio de reliability
)

/**
 * Wrapper para todos los datos de distribución de ratings
 */
data class RatingDistributionData(
    val distributions: List<CategoryRatingDistribution>,
    val statistics: RatingStatistics
)