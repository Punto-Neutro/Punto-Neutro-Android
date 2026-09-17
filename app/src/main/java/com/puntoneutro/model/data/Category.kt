package com.puntoneutro.model.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "categories")

@Serializable
data class Category(
    @PrimaryKey val category_id: Int = 0,
    val name: String = ""
)
