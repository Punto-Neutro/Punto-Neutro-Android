package com.example.sprint_2_kotlin.model.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * SessionManager - Manages user session persistence using DataStore
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

class SessionManager(private val context: Context) {

    companion object {
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_PASSWORD = stringPreferencesKey("user_password")
        private val USER_ID = intPreferencesKey("user_id")
        private val LOGIN_TIMESTAMP = longPreferencesKey("login_timestamp")
    }

    /**
     * Save user session after successful login
     */
    suspend fun saveSession(email: String, password: String, userId: Int) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = true
            preferences[USER_EMAIL] = email
            preferences[USER_PASSWORD] = password
            preferences[USER_ID] = userId
            preferences[LOGIN_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    /**
     * Clear user session on logout
     */
    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Check if user is logged in
     */
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN] ?: false
    }

    /**
     * Get stored user email
     */
    val userEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL]
    }

    /**
     * Get stored user password
     */
    val userPassword: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_PASSWORD]
    }

    /**
     * Get stored user ID
     */
    val userId: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID]
    }

    /**
     * Get login timestamp
     */
    val loginTimestamp: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[LOGIN_TIMESTAMP]
    }

    /**
     * Get stored credentials
     */
    suspend fun getStoredCredentials(): Pair<String, String>? {
        val preferences = context.dataStore.data.first()
        val email = preferences[USER_EMAIL]
        val password = preferences[USER_PASSWORD]

        return if (email != null && password != null) {
            Pair(email, password)
        } else {
            null
        }
    }

    /**
     * Check if session is valid (not expired)
     */
    suspend fun isSessionValid(): Boolean {
        val preferences = context.dataStore.data.first()
        val isLoggedIn = preferences[IS_LOGGED_IN] ?: false
        val timestamp = preferences[LOGIN_TIMESTAMP] ?: 0L

        // Check if session is older than 30 days
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
        val isNotExpired = (System.currentTimeMillis() - timestamp) < thirtyDaysInMillis

        return isLoggedIn && isNotExpired
    }
}