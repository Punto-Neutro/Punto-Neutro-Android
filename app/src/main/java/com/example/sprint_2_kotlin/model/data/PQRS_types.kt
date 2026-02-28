package com.example.sprint_2_kotlin.model.data

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "PQRS_types")

data class PQRS_types(
    @PrimaryKey val id: Int = 0,
    val name: String = "",
    val description: String = ""


)
