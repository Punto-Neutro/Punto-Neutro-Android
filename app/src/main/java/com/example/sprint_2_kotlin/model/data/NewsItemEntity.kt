package com.example.sprint_2_kotlin.model.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class for Room Database to cache NewsItem data
 * This represents the news_items table in the local SQLite database
 * Adapted to match the Supabase NewsItem structure
 */
@Entity(tableName = "news_items")
data class NewsItemEntity(
    @PrimaryKey
    val news_item_id: Int,
    val user_profile_id: Int,
    val title: String,
    val short_description: String,
    val image_url: String,
    val category_id: Int,
    val country_id: Int,
    val author_type: String,
    val author_institution: String,
    val days_since: Int,
    val average_reliability_score: Double,
    val is_fake: Boolean,
    val is_verifiedSource: Boolean,
    val is_verifiedData: Boolean,
    val is_recognizedAuthor: Boolean,
    val is_manipulated: Boolean,
    val long_description: String,
    val original_source_url: String,
    val publication_date: String,
    val added_to_appDate: String,
    val total_ratings: Int,
    val cachedTimestamp: Long = System.currentTimeMillis() // When it was cached
)

/**
 * Extension function to convert NewsItemEntity to NewsItem (domain model)
 */
fun NewsItemEntity.toNewsItem(): NewsItem {
    return NewsItem(
        news_item_id = this.news_item_id,
        user_profile_id = this.user_profile_id,
        title = this.title,
        short_description = this.short_description,
        image_url = this.image_url,
        category_id = this.category_id,
        country_id = this.country_id,
        author_type = this.author_type,
        author_institution = this.author_institution,
        days_since = this.days_since,
        average_reliability_score = this.average_reliability_score,
        is_fake = this.is_fake,
        is_verifiedSource = this.is_verifiedSource,
        is_verifiedData = this.is_verifiedData,
        is_recognizedAuthor = this.is_recognizedAuthor,
        is_manipulated = this.is_manipulated,
        long_description = this.long_description,
        original_source_url = this.original_source_url,
        publication_date = this.publication_date,
        added_to_appDate = this.added_to_appDate,
        total_ratings = this.total_ratings
    )
}

/**
 * Extension function to convert NewsItem to NewsItemEntity (for caching)
 */
fun NewsItem.toEntity(): NewsItemEntity {
    return NewsItemEntity(
        news_item_id = this.news_item_id,
        user_profile_id = this.user_profile_id,
        title = this.title,
        short_description = this.short_description,
        image_url = this.image_url,
        category_id = this.category_id,
        country_id = this.country_id,
        author_type = this.author_type,
        author_institution = this.author_institution,
        days_since = this.days_since,
        average_reliability_score = this.average_reliability_score,
        is_fake = this.is_fake,
        is_verifiedSource = this.is_verifiedSource,
        is_verifiedData = this.is_verifiedData,
        is_recognizedAuthor = this.is_recognizedAuthor,
        is_manipulated = this.is_manipulated,
        long_description = this.long_description,
        original_source_url = this.original_source_url,
        publication_date = this.publication_date,
        added_to_appDate = this.added_to_appDate,
        total_ratings = this.total_ratings
    )
}






