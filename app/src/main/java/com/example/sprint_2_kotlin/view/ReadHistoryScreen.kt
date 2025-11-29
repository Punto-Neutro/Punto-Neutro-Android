package com.example.sprint_2_kotlin.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.sprint_2_kotlin.model.data.ReadHistoryEntity
import com.example.sprint_2_kotlin.viewmodel.ReadHistoryViewModel
import com.example.sprint_2_kotlin.viewmodel.NewsFeedViewModel  // update
import com.example.sprint_2_kotlin.viewmodel.RatingDistributionViewModel  // update
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadHistoryScreen(
    isDarkMode: Boolean = false,
    onBackClick: () -> Unit = {},
    onNewsItemClick: (Int) -> Unit = {},
    viewModel: ReadHistoryViewModel = viewModel(),
    newsFeedViewModel: NewsFeedViewModel = viewModel(),  // update: Para acceder a getCategoryLabel
    ratingViewModel: RatingDistributionViewModel = viewModel()  // update: Para acceder a getCategoryColor
) {
    val readHistory by viewModel.readHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val readCount by viewModel.readCount.collectAsState()

    // Colores dinámicos según el tema
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    // Dialog state for confirming clear all
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Read History",
                            color = textColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$readCount articles read",
                            color = secondaryTextColor,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                },
                actions = {
                    if (readHistory.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear History",
                                tint = Color(0xFFE53935)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    // Loading state
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = if (isDarkMode) Color(0xFF9C27B0) else Color(0xFF1976D2)
                    )
                }
                readHistory.isEmpty() -> {
                    // Empty state
                    EmptyHistoryState(
                        isDarkMode = isDarkMode,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    // History list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = readHistory,
                            key = { it.id }
                        ) { historyItem ->
                            ReadHistoryCard(
                                historyItem = historyItem,
                                isDarkMode = isDarkMode,
                                categoryLabel = newsFeedViewModel.getCategoryLabel(historyItem.categoryId),  // modified
                                categoryColor = ratingViewModel.getCategoryColor(historyItem.categoryId),  // update
                                onClick = { onNewsItemClick(historyItem.newsItemId) }
                            )
                        }

                        // Bottom spacer
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Clear All History Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = surfaceColor,
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Clear History?",
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all your read history. This action cannot be undone.",
                    color = secondaryTextColor
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false }
                ) {
                    Text("Cancel", color = textColor)
                }
            }
        )
    }
}

@Composable
fun ReadHistoryCard(
    historyItem: ReadHistoryEntity,
    isDarkMode: Boolean = false,
    categoryLabel: String = "General",  //update: Parámetro para nombre de categoría
    categoryColor: Color = Color.Gray,  // update: Parámetro para color de categoría
    onClick: () -> Unit = {}
) {
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Image thumbnail
            if (historyItem.imageUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(historyItem.imageUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Title
                Text(
                    text = historyItem.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                // Short description
                if (historyItem.shortDescription.isNotEmpty()) {
                    Text(
                        text = historyItem.shortDescription,
                        fontSize = 13.sp,
                        color = secondaryTextColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                }

                // Meta info (category, author, time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category badge - update: Ahora usa el nombre y color real
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = categoryColor.copy(alpha = 0.15f)  //Color con transparencia
                    ) {
                        Text(
                            text = categoryLabel,  // Update: Muestra el nombre en lugar del ID
                            fontSize = 11.sp,
                            color = categoryColor,  //Update: Usa el color de la categoría
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Time ago
                    Text(
                        text = formatTimeAgo(historyItem.readTimestamp),
                        fontSize = 11.sp,
                        color = secondaryTextColor
                    )
                }

                // Author info
                if (historyItem.authorType.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "By ${historyItem.authorType}${
                            if (historyItem.authorInstitution.isNotEmpty())
                                " at ${historyItem.authorInstitution}"
                            else ""
                        }",
                        fontSize = 11.sp,
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyHistoryState(
    isDarkMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = "No history",
            modifier = Modifier.size(80.dp),
            tint = secondaryTextColor.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No Reading History",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Articles you read will appear here",
            fontSize = 14.sp,
            color = secondaryTextColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * Format timestamp to relative time (e.g., "2 hours ago", "Yesterday")
 */
private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
        days == 1L -> "Yesterday"
        days < 7 -> "$days days ago"
        else -> {
            // Format as date for older entries
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}