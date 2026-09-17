package com.puntoneutro.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.puntoneutro.model.data.NewsItem
import com.puntoneutro.viewmodel.NewsFeedViewModel
import androidx.compose.ui.graphics.Color

@Composable
fun NewsCard(
    item: NewsItem,
    viewModel: NewsFeedViewModel,
    onClick: () -> Unit = {} // 👈 new param
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // 👈 make card clickable
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Image(
                painter = rememberAsyncImagePainter(item.image_url),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = viewModel.getCategoryLabel(item.category_id),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                ReliabilityIndicator(item.average_reliability_score)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Text(
                text = item.short_description,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${item.author_type} at ${item.author_institution}",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "${item.days_since} days ago • ${item.total_ratings} ratings",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}



@Composable
fun ReliabilityIndicator(score: Double) {
    val percentage = (score * 100).toInt()

    val color = when {
        percentage >= 75 -> Color(0xFF4CAF50) // Green
        percentage >= 50 -> Color(0xFFFFC107) // Yellow
        else -> Color(0xFFF44336)             // Red
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$percentage%",
            color = color,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
