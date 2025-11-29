package com.example.sprint_2_kotlin.model.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing read history locally
 * Tracks which news items a user has read
 * Only stores the first time a news item is opened
 */
@Entity(tableName = "read_history")
data class ReadHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val newsItemId: Int,              // ID of the news item (references news_items table)
    val title: String,                // Title of the news
    val shortDescription: String,     // Short description
    val imageUrl: String,             // Image URL for thumbnail
    val categoryId: Int,              // Category ID
    val readTimestamp: Long,          // When it was read (milliseconds since epoch)
    val authorType: String = "",      // Author type (e.g., "Journalist", "Researcher")
    val authorInstitution: String = "" // Author institution
)

/**
 * Extension function to convert ReadHistoryEntity to NewsItem
 * Useful for navigating to news detail from history
 */
fun ReadHistoryEntity.toNewsItem(): NewsItem {
    return NewsItem(
        news_item_id = this.newsItemId,
        title = this.title,
        short_description = this.shortDescription,
        image_url = this.imageUrl,
        category_id = this.categoryId,
        author_type = this.authorType,
        author_institution = this.authorInstitution
    )
}

/**
 * Extension function to create ReadHistoryEntity from NewsItem
 * Used when registering a new read event
 */
fun NewsItem.toReadHistoryEntity(): ReadHistoryEntity {
    return ReadHistoryEntity(
        newsItemId = this.news_item_id,
        title = this.title,
        shortDescription = this.short_description,
        imageUrl = this.image_url,
        categoryId = this.category_id,
        readTimestamp = System.currentTimeMillis(),
        authorType = this.author_type,
        authorInstitution = this.author_institution
    )
}
