package com.example.sprint_2_kotlin.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sprint_2_kotlin.model.data.RatingItem
import androidx.compose.ui.graphics.Color

@Composable
fun RatingItemCard(
    rating: RatingItem,
    isDarkMode: Boolean = false
) {
    val percentage = (rating.assigned_reliability_score * 100).toInt()
    val color = when {
        percentage >= 75 -> Color(0xFF4CAF50)
        percentage >= 50 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    // Colores dinámicos según el tema
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    Card(
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Reliability: $percentage%",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = rating.rating_date,
                    fontSize = 12.sp,
                    color = secondaryTextColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = rating.comment_text.ifEmpty { "No comment provided." },
                fontSize = 14.sp,
                color = textColor
            )
        }
    }
}