package com.example.sprint_2_kotlin.model.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "PQRS")

@Serializable
data class PQRS(
    val description: String = "",
    val type_id : Int = 0,
    val user_id: String = "",
    @PrimaryKey (autoGenerate = true) val id: Int = 0,
)