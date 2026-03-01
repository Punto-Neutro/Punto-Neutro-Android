package com.example.sprint_2_kotlin.model.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PQRS_typesDao {
    @Insert
    suspend fun insert(comment: PQRS_types)

    @Insert
    suspend fun insertAll(comments: List<PQRS_types>)
    @Query("SELECT * FROM PQRS_types")
    suspend fun getAllPQRS_types(): List<PQRS_types>

    @Delete
    suspend fun delete(comment: PQRS_types)

    @Query("DELETE FROM PQRS_types")
    suspend fun deleteAll()

}