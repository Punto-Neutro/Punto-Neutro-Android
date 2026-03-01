package com.example.sprint_2_kotlin.model.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PQRSDao {
    @Insert
    suspend fun insert(comment: PQRS)

    @Delete
    suspend fun delete(comment: PQRS)

    @Query("SELECT * FROM PQRS ")
    suspend fun getAllPendingPQRS(): List<PQRS>

}