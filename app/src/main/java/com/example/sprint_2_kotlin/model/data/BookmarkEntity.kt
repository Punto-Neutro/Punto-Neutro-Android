package com.example.sprint_2_kotlin.model.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Entity for storing bookmarks locally (Local-First)
 * Tracks which news items a user has bookmarked
 * Includes sync status for eventual connectivity to Supabase
 */
@Entity(tableName = "bookmarks")
@Serializable
data class BookmarkEntity(
    val newsItemId: Int,              // ID of the bookmarked news item

    // Denormalized data for offline display
    val title: String,                // Title of the news
    val shortDescription: String,     // Short description
    val imageUrl: String,             // Image URL for thumbnail
    val categoryId: Int,              // Category ID
    val userid: String,                  // User ID

    // Timestamps

    val bookmarkedAt: Long? = null,           // When it was bookmarked (milliseconds since epoch)

    // Sync status for eventual connectivity
    val isSynced: Boolean = false,    // false = pending sync to Supabase
    val lastSyncAttempt: Long = 0, // Last time sync was attempted
    val syncRetries: Int = 0,

    @PrimaryKey (autoGenerate = true) val bookmark_id: Int = 0,// Number of failed sync attempts
)

/**
 * Extension function to create BookmarkEntity from NewsItem
 */
fun NewsItem.toBookmarkEntity(): BookmarkEntity {
    return BookmarkEntity(

        newsItemId = this.news_item_id,
        title = this.title,
        shortDescription = this.short_description,
        imageUrl = this.image_url,
        categoryId = this.category_id,
        userid = this.user_profile_id,
        bookmarkedAt = System.currentTimeMillis(),
        isSynced = false,
        bookmark_id = 0

    )
}