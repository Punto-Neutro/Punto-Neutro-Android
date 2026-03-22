@file:OptIn(FlowPreview::class)

package com.example.sprint_2_kotlin.view

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    preferences: SharedPreferences,
    isDarkMode: Boolean = false,
    onBackClick: () -> Unit = {},
    onNewsItemClick: (Int) -> Unit = {},
    viewModel: BookmarkViewModel = viewModel()
) {
    val bookmarkCount by viewModel.bookmarkCount.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()


    // Colores dinámicos
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    // 1. Use the new paginated state instead of the full list
    val bookmarks by viewModel.paginatedBookmarks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 2. Setup scroll state and "should load more" detector
    val scrollposition = remember{ preferences.getInt("scroll_bookmarks", 0)}

    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = scrollposition
    )
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf false
            lastVisibleItem.index >= lazyListState.layoutInfo.totalItemsCount - 5
        }
    }

    // 3. Trigger load more when needed
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && bookmarks.isNotEmpty()) {
            viewModel.loadNextPage()
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .debounce(500) // Debounce for 500ms
            .collectLatest { index ->
                preferences.edit().putInt("scroll_bookmarks", index).apply()
            }
    }



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
                    state = lazyListState,
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
                    if (isLoading) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }


}