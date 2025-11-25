package com.example.sprint_2_kotlin.model.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database class for the application
 * This is the main database configuration with all entities and version
 */
@Database(
    entities = [
        NewsItemEntity::class,
        PendingComment::class,
        ReadHistoryEntity::class  // update: Nueva entidad para historial de lectura
    ],
    version = 3,  // cambiao-> Incrementado de 2 a 3
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides access to NewsItemDao for database operations
     */
    abstract fun newsItemDao(): NewsItemDao
    abstract fun CommentDao(): CommentDao
    abstract fun readHistoryDao(): ReadHistoryDao  // update: DAO para historial de lectura

    companion object {
        // Singleton prevents multiple instances of database opening at the same time
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Gets the singleton instance of AppDatabase
         * Uses double-checked locking pattern for thread safety
         */
        fun getDatabase(context: Context): AppDatabase {
            // If INSTANCE is not null, return it
            // If it is null, create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sprint2_news_database"
                )
                    // Strategy for destructive migration (will delete and recreate tables)
                    // In production, you should use proper migrations
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * For testing purposes - allows clearing the singleton instance
         */
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}