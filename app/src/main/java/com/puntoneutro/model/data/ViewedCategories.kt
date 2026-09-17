package com.puntoneutro.model.data

import kotlinx.serialization.Serializable

@Serializable
data class ViewedCategories(
    val category_id: Int = 0,
    val userSession_id: Int = 0
)
