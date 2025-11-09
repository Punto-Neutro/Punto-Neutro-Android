package com.example.sprint_2_kotlin.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.sprint_2_kotlin.model.data.NewsItem
import com.example.sprint_2_kotlin.model.data.Category
import com.example.sprint_2_kotlin.viewmodel.NewsFeedViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsFeedScreen(
    onNewsItemClick: (Int) -> Unit,
    onNavigateToGuide: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: NewsFeedViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val newsItems by viewModel.newsItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val cacheStatus by viewModel.cacheStatus.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    FeedHeader(cacheStatus = cacheStatus)

                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it }
                    )

                    CategoryTabsFromSupabase(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { category ->
                            viewModel.selectCategory(category)
                        },
                        onClearFilter = {
                            viewModel.clearCategoryFilter()
                        }
                    )
                }
            }
        },
        bottomBar = {
            BottomNavigationBar(
                onNavigateToGuide = onNavigateToGuide,
                onNavigateToProfile = onNavigateToProfile
            )
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing),
            onRefresh = { viewModel.refreshNewsFeed() },
            modifier = Modifier.padding(paddingValues)
        ) {
            if (isLoading && newsItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Loading news...",
                            color = Color(0xFF666666),
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (newsItems.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "No news",
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFFAAAAAA)
                        )
                        Text(
                            text = if (selectedCategory != null) {
                                "No news in ${selectedCategory?.name} category"
                            } else {
                                "No news available"
                            },
                            fontSize = 16.sp,
                            color = Color(0xFF666666)
                        )
                        Button(
                            onClick = { viewModel.refreshNewsFeed() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1A1A1A)
                            )
                        ) {
                            Icon(Icons.Default.Refresh, "Refresh")
                            Spacer(Modifier.width(8.dp))
                            Text("Refresh")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5F5)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    item {
                        MisinformationAlert()
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (selectedCategory != null) {
                        item {
                            FilterInfoCard(
                                categoryName = selectedCategory!!.name,
                                itemCount = newsItems.size,
                                onClearFilter = { viewModel.clearCategoryFilter() }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    items(newsItems, key = { it.news_item_id }) { item ->
                        NewsCard(
                            item = item,
                            categories = categories,
                            onClick = { onNewsItemClick(item.news_item_id) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (newsItems.isNotEmpty()) {
                        item {
                            Text(
                                text = "📦 Using cached data for faster loading",
                                fontSize = 12.sp,
                                color = Color(0xFF999999),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryTabsFromSupabase(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    onClearFilter: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            CategoryChip(
                text = "All",
                selected = selectedCategory == null,
                onClick = onClearFilter
            )
        }

        items(categories) { category ->
            CategoryChip(
                text = category.name,
                selected = selectedCategory?.category_id == category.category_id,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
fun FilterInfoCard(
    categoryName: String,
    itemCount: Int,
    onClearFilter: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Filtered by: $categoryName",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1976D2),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "$itemCount ${if (itemCount == 1) "article" else "articles"} found",
                        fontSize = 11.sp,
                        color = Color(0xFF1976D2)
                    )
                }
            }

            IconButton(
                onClick = onClearFilter,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear filter",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun FeedHeader(cacheStatus: String = "") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Logo",
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF1A1A1A)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Punto Neutro",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            }
            if (cacheStatus.isNotEmpty()) {
                Text(
                    text = cacheStatus,
                    fontSize = 11.sp,
                    color = Color(0xFF999999),
                    modifier = Modifier.padding(start = 32.dp, top = 2.dp)
                )
            }
        }

        Box {
            IconButton(onClick = { /* TODO */ }) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = Color(0xFF1A1A1A)
                )
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(x = 24.dp, y = 8.dp)
                    .background(Color.Red, CircleShape)
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(value = "1,247", label = "Verified today")
        StatItem(value = "25", label = "Fake detected", color = Color(0xFFE53935))
        StatItem(value = "158", label = "Verifying")
    }

    Divider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = Color(0xFFE0E0E0)
    )
}

@Composable
fun StatItem(value: String, label: String, color: Color = Color(0xFF1A1A1A)) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF666666)
        )
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search news...", color = Color(0xFFAAAAAA)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF666666)
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFDDDDDD),
                unfocusedBorderColor = Color(0xFFDDDDDD),
                focusedContainerColor = Color(0xFFF8F8F8),
                unfocusedContainerColor = Color(0xFFF8F8F8)
            ),
            singleLine = true
        )

        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = { /* TODO: Filter */ },
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp))
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter",
                tint = Color(0xFF1A1A1A)
            )
        }
    }
}

@Composable
fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Color(0xFF1A1A1A) else Color.Transparent,
        border = if (!selected) ButtonDefaults.outlinedButtonBorder else null
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (selected) Color.White else Color(0xFF666666),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun MisinformationAlert() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = Color(0xFFFF6F00),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Misinformation Alert",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B4513),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "3 fake news stories detected about health topics. Verify sources before sharing.",
                    fontSize = 12.sp,
                    color = Color(0xFF8B4513),
                    lineHeight = 16.sp
                )
            }
        }
        TextButton(
            onClick = { /* TODO */ },
            modifier = Modifier.padding(start = 52.dp, bottom = 8.dp)
        ) {
            Text("View details", color = Color(0xFF8B4513), fontSize = 13.sp)
        }
    }
}

@Composable
fun NewsCard(
    item: NewsItem,
    categories: List<Category>,
    onClick: () -> Unit
) {
    val itemCategory = categories.find { it.category_id == item.category_id }
    val categoryName = itemCategory?.name ?: "General"
    val categoryColor = getCategoryColorDynamic(item.category_id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = item.image_url,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = categoryColor
                    ) {
                        Text(
                            text = categoryName,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = getReliabilityColor(item.average_reliability_score)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Reliability",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${(item.average_reliability_score * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = item.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = item.short_description,
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.is_verifiedSource) Icons.Default.Verified else Icons.Default.Person,
                        contentDescription = "Author",
                        tint = if (item.is_verifiedSource) Color(0xFF4CAF50) else Color(0xFF999999),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Verified author",
                        fontSize = 13.sp,
                        color = Color(0xFF666666)
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.author_institution.ifEmpty { "Unknown Source" },
                        fontSize = 12.sp,
                        color = Color(0xFF888888),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " • ${item.days_since} days ago",
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Divider(color = Color(0xFFE0E0E0))

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconWithText(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            text = "${item.total_ratings}"
                        )
                        IconWithText(
                            icon = Icons.Outlined.Share,
                            text = ""
                        )
                        IconWithText(
                            icon = Icons.Outlined.BookmarkBorder,
                            text = ""
                        )
                    }

                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A1A1A)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Read more",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IconWithText(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF666666),
            modifier = Modifier.size(20.dp)
        )
        if (text.isNotEmpty()) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = Color(0xFF666666)
            )
        }
    }
}

fun getCategoryColorDynamic(categoryId: Int): Color {
    return when (categoryId) {
        1 -> Color(0xFF2196F3)
        2 -> Color(0xFFE91E63)
        3 -> Color(0xFF4CAF50)
        4 -> Color(0xFFFF5722)
        5 -> Color(0xFF9C27B0)
        6 -> Color(0xFF00BCD4)
        7 -> Color(0xFFFF9800)
        8 -> Color(0xFF607D8B)
        else -> Color(0xFF757575)
    }
}

fun getReliabilityColor(score: Double): Color {
    return when {
        score >= 0.8 -> Color(0xFF4CAF50)
        score >= 0.6 -> Color(0xFFFFC107)
        else -> Color(0xFFE53935)
    }
}

@Composable
fun BottomNavigationBar(
    onNavigateToGuide: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 12.sp) },
            selected = true,
            onClick = { }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.MenuBook, contentDescription = "Guide") },
            label = { Text("Guide", fontSize = 12.sp) },
            selected = false,
            onClick = onNavigateToGuide
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile") },
            label = { Text("Profile", fontSize = 12.sp) },
            selected = false,
            onClick = onNavigateToProfile
        )
    }
}