package com.example.sprint_2_kotlin.model.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<Category>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
}
