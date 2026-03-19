package com.example.sprint_2_kotlin.model.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sync Queue for eventual connectivity
 * Stores pending operations (ADD/REMOVE bookmarks) when offline
 * Operations are processed when connection is restored
 */
@Entity(tableName = "bookmark_sync_queue")
data class BookmarkSyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val newsItemId: Int,              // ID of the news item
    val categoryId: Int,              // Category ID
    val userId: Int?,                  // User ID
    val shortDescription: String,     // Short description
    val imageUrl: String,             // Image URL
    val title: String,                // Title
    val operationType: OperationType, // ADD or REMOVE

    // Timestamp and retry control
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttempt: Long? = null,
    val retryCount: Int = 0,

    // Status
    val status: SyncStatus = SyncStatus.PENDING
) {
    enum class OperationType {
        ADD,    // Add bookmark to Supabase
        REMOVE  // Remove bookmark from Supabase
    }

    enum class SyncStatus {
        PENDING,     // Waiting to be synced
        IN_PROGRESS, // Currently syncing
        FAILED,      // Failed (will retry)
        COMPLETED    // Successfully synced
    }
}