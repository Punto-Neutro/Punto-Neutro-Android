package com.example.sprint_2_kotlin

// Define navigation routes
sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object NewsFeed : Screen("news_feed")
    object NewsItemDetail : Screen("news_item_detail/{newsItemId}") {
        fun createRoute(newsItemId: Int) = "news_item_detail/$newsItemId"
    }
    object Guide : Screen("guide")
    object Profile : Screen("profile")

    object ReadHistory : Screen("read_history")  // update: Ruta para historial de lectura
}












