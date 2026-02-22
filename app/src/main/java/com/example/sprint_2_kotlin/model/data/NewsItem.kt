package com.example.sprint_2_kotlin.model.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "news_items")
data class NewsItem(

    @SerialName("user_profile_id")
    val user_profile_id: Int = 0,

    val title: String = "",

    @SerialName("short_description")
    val short_description: String = "",

    @SerialName("image_url")
    val image_url: String = "",

    @SerialName("category_id")
    val category_id: Int = 0,

    @SerialName("author_type")
    val author_type: String = "",

    @SerialName("author_institution")
    val author_institution: String = "",

    @SerialName("days_since")
    val days_since: Int = 0,

    @SerialName("average_reliability_score")
    val average_reliability_score: Double = 0.00,

    @SerialName("is_fake")
    val is_fake: Boolean = false,

    @SerialName("is_verifiedSource")
    val is_verifiedSource: Boolean = false,

    @SerialName("is_verifiedData")
    val is_verifiedData: Boolean = false,

    @SerialName("is_recognizedAuthor")
    val is_recognizedAuthor: Boolean = false,

    @SerialName("is_manipulated")
    val is_manipulated: Boolean = false,

    @SerialName("long_description")
    val long_description: String = "",

    @SerialName("original_source_url")
    val original_source_url: String = "",

    @SerialName("publication_date")
    val publication_date: String = "",

    @SerialName("added_to_appDate")
    val added_to_appDate: String = "",

    @SerialName("total_ratings")
    val total_ratings: Int = 0,

    // THE MOST IMPORTANT ONE
    @SerialName("country_id")
    val country_id: Int = 0,

    // Define the primary key for Room
    @PrimaryKey
    @SerialName("news_item_id")
    val news_item_id: Int = 0,
)

// Your InsertNewsItem class is correct as it is.



// Add this class to the bottom of the NewsItem.kt file@Serializable
@Serializable
data class InsertNewsItem(
    @SerialName("user_profile_id")
    val user_profile_id: Int,
    val title: String,
    @SerialName("short_description")
    val short_description: String,
    @SerialName("long_description")
    val long_description: String,
    @SerialName("image_url")
    val image_url: String,
    @SerialName("original_source_url")
    val original_source_url: String,
    @SerialName("category_id")
    val category_id: Int,
    @SerialName("country_id")
    val country_id: Int, // Your country_id is here and will be sent correctly
    @SerialName("author_type")
    val author_type: String,
    @SerialName("author_institution")
    val author_institution: String
    // No IDs or default values like total_ratings, is_fake, etc.
    // The database will handle defaults for these.
)


