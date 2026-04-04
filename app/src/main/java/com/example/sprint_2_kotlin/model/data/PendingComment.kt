package com.example.sprint_2_kotlin.model.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_comments")

data class PendingComment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val newsItemId: Int,
    val userProfileId: String,
    val reliabilityScore: Double,
    val commentText: String
)
