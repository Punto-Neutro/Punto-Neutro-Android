package com.example.sprint_2_kotlin.model.data

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(

    val user_auth_id: String? = "",
    val user_auth_email: String = "",
    val user_profile_id: Int = 0,
)
