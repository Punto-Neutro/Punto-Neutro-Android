package com.example.sprint_2_kotlin.model.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
@Dao
interface CountryDao {
    @Query("SELECT * FROM Countries")
    suspend fun getAllCountries(): List<Country>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(countries: List<Country>)

    @Query("DELETE FROM Countries")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM Countries")
    suspend fun getCountryCount(): Int
}