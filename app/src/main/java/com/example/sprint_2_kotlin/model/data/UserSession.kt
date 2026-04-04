package com.example.sprint_2_kotlin.model.data

import kotlinx.serialization.Serializable


@Serializable
data class UserSession(
    val user_session_id: Int = 0,
    val user_profile_id: String = "",
    val start_time: String = "",
    val end_time: String = "",
    val duration_seconds: Int = 0,
    val device_type: String = "",
    val operating_system: String = "",
    val usedCategory_filter: Boolean = false,
    val articles_viewed: Int = 0
)
