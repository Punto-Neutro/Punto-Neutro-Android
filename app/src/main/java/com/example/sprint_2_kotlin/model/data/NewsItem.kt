package com.example.sprint_2_kotlin.model.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "news_items")
data class NewsItem(

    val user_profile_id: Int = 0,
    val title: String = "",
    val short_description: String = "",
    val image_url: String = "",
    val category_id: Int = 0,
    val country_id: Int = 0,
    val author_type: String = "",
    val author_institution: String = "",
    val days_since: Int = 0,
    val average_reliability_score: Double = 0.00,
    val is_fake: Boolean = false,
    val is_verifiedSource: Boolean = false,
    val is_verifiedData: Boolean = false,
    val is_recognizedAuthor: Boolean = false,
    val is_manipulated: Boolean = false,
    val long_description: String = "",
    val original_source_url: String = "",
    val publication_date: String = "",
    val added_to_appDate: String = "",
    val total_ratings: Int = 0,
    val news_item_id: Int = 0,
)

