package com.example.sprint_2_kotlin.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sprint_2_kotlin.viewmodel.BookmarkViewModel
import com.example.sprint_2_kotlin.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    isDarkMode: Boolean = false,
    onBackClick: () -> Unit = {},
    onNewsItemClick: (Int) -> Unit = {},
    viewModel: BookmarkViewModel = viewModel()
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val bookmarkCount by viewModel.bookmarkCount.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }

    // Colores dinámicos
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.Bookmarks),
                        color = textColor
                    )
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
                    if (bookmarkCount > 0) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear all",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
        ) {
            // Sync status banner
            if (pendingSyncCount > 0 || !isConnected) {
                SyncStatusBanner(
                    isConnected = isConnected,
                    pendingSyncCount = pendingSyncCount,
                    syncStatus = syncStatus,
                    isDarkMode = isDarkMode,
                    onSyncClick = { viewModel.forceSyncNow() }
                )
            }

            // Bookmarks list
            if (bookmarks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyBookmarksState(isDarkMode = isDarkMode)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    item {
                        Text(
                            text = "$bookmarkCount ${stringResource(R.string.Saved_articles)}${if (bookmarkCount != 1) "s" else ""}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    // Bookmarks
                    items(bookmarks) { bookmark ->
                        BookmarkCard(
                            bookmark = bookmark,
                            isDarkMode = isDarkMode,
                            onClick = { onNewsItemClick(bookmark.newsItemId) }
                        )
                    }
                }
            }
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
            title = { Text(stringResource(R.string.Clear_all_bookmarks)) },
            text = { Text("${stringResource(R.string.This_will_remove_all)} $bookmarkCount ${stringResource(R.string.This_action_cannot_be_undone)}") },
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
                    Text(stringResource(R.string.Clear_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.Cancel))
                }
            }
        )
    }
}