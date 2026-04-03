package com.example.sprint_2_kotlin.model.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "Countries")

@Serializable
data class Country(
    @PrimaryKey val id: Int = 0,
    val country_name: String = ""
)