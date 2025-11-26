package com.example.sprint_2_kotlin.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import coil.compose.AsyncImage
import com.example.sprint_2_kotlin.model.data.BookmarkEntity
import com.example.sprint_2_kotlin.viewmodel.BookmarkViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Bookmarks tab content for ProfileScreen
 * Shows list of saved articles with sync status
 */
@Composable
fun BookmarksTabContent(
    viewModel: BookmarkViewModel,
    isDarkMode: Boolean = false,
    onNavigateToDetail: (Int) -> Unit = {}
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val bookmarkCount by viewModel.bookmarkCount.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }

    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    // fix: Usar Box en lugar de Column
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Bookmarks list
        if (bookmarks.isEmpty()) {
            EmptyBookmarksState(isDarkMode = isDarkMode)
        } else {
            // fix: Remover fillMaxSize() del LazyColumn
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),  // Solo fillMaxWidth, NO fillMaxSize
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (pendingSyncCount > 0 || !isConnected) 56.dp else 16.dp,  // Espacio para banner
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with count and clear button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$bookmarkCount Saved Article${if (bookmarkCount != 1) "s" else ""}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        TextButton(onClick = { showClearDialog = true }) {
                            Text(
                                "Clear All",
                                color = Color(0xFFE53935)
                            )
                        }
                    }
                }

                // Bookmarks
                items(bookmarks) { bookmark ->
                    BookmarkCard(
                        bookmark = bookmark,
                        isDarkMode = isDarkMode,
                        onClick = { onNavigateToDetail(bookmark.newsItemId) }
                    )
                }
            }
        }

        // fix: Banner flotante en la parte superior
        if (pendingSyncCount > 0 || !isConnected) {
            SyncStatusBanner(
                isConnected = isConnected,
                pendingSyncCount = pendingSyncCount,
                syncStatus = syncStatus,
                isDarkMode = isDarkMode,
                onSyncClick = { viewModel.forceSyncNow() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    // Clear confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE53935)
                )
            },
            title = { Text("Clear All Bookmarks?") },
            text = { Text("This will remove all $bookmarkCount saved articles. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllBookmarks()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
@Composable
fun SyncStatusBanner(
    isConnected: Boolean,
    pendingSyncCount: Int,
    syncStatus: BookmarkViewModel.SyncStatus,
    isDarkMode: Boolean,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier  //fix
) {
    val bannerColor = when {
        !isConnected -> Color(0xFFE53935)
        pendingSyncCount > 0 -> Color(0xFFFFA726)
        else -> Color(0xFF4CAF50)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),  // fix
        color = bannerColor.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.Cloud else Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = bannerColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = when {
                            !isConnected -> "Offline Mode"
                            pendingSyncCount > 0 -> "$pendingSyncCount pending sync"
                            else -> "All synced"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = bannerColor
                    )
                    if (syncStatus is BookmarkViewModel.SyncStatus.Syncing) {
                        Text(
                            text = "Syncing...",
                            fontSize = 12.sp,
                            color = bannerColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            if (isConnected && pendingSyncCount > 0) {
                IconButton(onClick = onSyncClick) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = "Sync now",
                        tint = bannerColor
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarkCard(
    bookmark: BookmarkEntity,
    isDarkMode: Boolean,
    onClick: () -> Unit
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
            // Image
            AsyncImage(
                model = bookmark.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = bookmark.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = bookmark.shortDescription,
                        fontSize = 12.sp,
                        color = secondaryTextColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = getCategoryName(bookmark.categoryId),
                        fontSize = 11.sp,
                        color = getCategoryColor(bookmark.categoryId),
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = getRelativeTime(bookmark.bookmarkedAt),
                        fontSize = 11.sp,
                        color = secondaryTextColor
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyBookmarksState(isDarkMode: Boolean) {
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = secondaryTextColor.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No Bookmarks Yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Save articles to read them later",
                fontSize = 14.sp,
                color = secondaryTextColor
            )
        }
    }
}

// Helper functions
private fun getCategoryName(categoryId: Int): String {
    return when (categoryId) {
        1 -> "Politics"
        2 -> "Sports"
        3 -> "Technology"
        4 -> "Entertainment"
        5 -> "Science"
        6 -> "Health"
        7 -> "Business"
        8 -> "Local"
        else -> "Other"
    }
}

private fun getCategoryColor(categoryId: Int): Color {
    return when (categoryId) {
        1 -> Color(0xFF1976D2) // Politics - Blue
        2 -> Color(0xFF388E3C) // Sports - Green
        3 -> Color(0xFF7B1FA2) // Technology - Purple
        4 -> Color(0xFFE91E63) // Entertainment - Pink
        5 -> Color(0xFF00897B) // Science - Teal
        6 -> Color(0xFFF57C00) // Health - Orange
        7 -> Color(0xFF5D4037) // Business - Brown
        8 -> Color(0xFF455A64) // Local - Blue Grey
        else -> Color(0xFF757575) // Other - Grey
    }
}

private fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            try {
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
            } catch (e: Exception) {
                "Recently"
            }
        }
    }
}