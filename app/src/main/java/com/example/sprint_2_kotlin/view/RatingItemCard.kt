package com.example.sprint_2_kotlin.view

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.format.FormatStyle
import com.example.sprint_2_kotlin.R

@RequiresApi(Build.VERSION_CODES.O)
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
                    text = "${stringResource(R.string.Reliability)}: $percentage%",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = formatSupabaseTimestamp(
                        context = LocalContext.current,
                        timestamp = rating.rating_date
                    ),
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

@RequiresApi(Build.VERSION_CODES.O)
fun formatSupabaseTimestamp(
    context: Context,
    timestamp: String
): String {
    val locale = context.resources.configuration.locales[0]

    val dateTime = OffsetDateTime.parse(timestamp)
    val local = dateTime.atZoneSameInstant(ZoneId.systemDefault())

    val formatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(locale)

    return local.format(formatter)
}
