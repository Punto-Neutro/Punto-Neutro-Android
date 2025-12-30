package com.example.sprint_2_kotlin.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.forEach
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.sprint_2_kotlin.model.data.NewsItem
import com.example.sprint_2_kotlin.model.data.Category
import com.example.sprint_2_kotlin.viewmodel.NewsFeedViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import utils.NetworkMonitor

import coil.request.ImageRequest
import coil.request.CachePolicy
import androidx.compose.ui.platform.LocalContext
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsFeedScreen(
    isDarkMode: Boolean = false,
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

    val connectionRestored by viewModel.connectionRestored.collectAsState()
    val noSearchResults by viewModel.noSearchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        FeedbackDialog(
            isDarkMode = isDarkMode,
            categories = categories,
            onDismiss = { showDialog = false }

        )
    }

    // Colores dinámicos según el tema
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    val context = LocalContext.current
    val networkMonitor = remember { NetworkMonitor(context) }

    LaunchedEffect(Unit) {
        viewModel.startNetworkObserver(networkMonitor)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = if (isDarkMode) Color(0xFF9C27B0) else MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, "Add")
            }
        },

        bottomBar = {
            BottomNavigationBar(
                isDarkMode = isDarkMode,
                onNavigateToGuide = onNavigateToGuide,
                onNavigateToProfile = onNavigateToProfile
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing),
                onRefresh = { viewModel.refreshNewsFeed() }
            ) {
                if (isLoading && newsItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = if (isDarkMode) Color(0xFF9C27B0) else Color(0xFF1A1A1A)
                            )
                            Text(
                                "Loading news...",
                                color = secondaryTextColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else if (newsItems.isEmpty() && !isLoading && searchQuery.isBlank()) {
                    // update: Solo mostrar "No news" si NO hay búsqueda activa
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor),
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
                                tint = secondaryTextColor
                            )
                            Text(
                                text = if (selectedCategory != null) {
                                    "No news in ${selectedCategory?.name} category"
                                } else {
                                    "No news available"
                                },
                                fontSize = 16.sp,
                                color = secondaryTextColor
                            )
                            Button(
                                onClick = { viewModel.refreshNewsFeed() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDarkMode) Color(0xFF9C27B0) else Color(0xFF1A1A1A)
                                )
                            ) {
                                Icon(Icons.Default.Refresh, "Refresh")
                                Spacer(Modifier.width(8.dp))
                                Text("Refresh")
                            }
                        }
                    }
                }else {
                    LazyColumn(
                        modifier = modifier
                            .fillMaxSize()
                            .background(backgroundColor)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(surfaceColor)
                            ) {
                                FeedHeader(
                                    isDarkMode = isDarkMode,
                                    cacheStatus = cacheStatus
                                )
                                SearchBar(
                                    isDarkMode = isDarkMode,
                                    query = searchQuery,
                                    onQueryChange = { viewModel.updateSearchQuery(it) }
                                )
                                CategoryTabsFromSupabase(
                                    isDarkMode = isDarkMode,
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

// Update: Banner de "No matches" cuando búsqueda no tiene resultados
                        if (noSearchResults) {
                            item {
                                NoSearchResultsBanner(
                                    isDarkMode = isDarkMode,
                                    searchQuery = searchQuery,
                                    onClearSearch = { viewModel.clearSearch() }
                                )
                            }
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                MisinformationAlert(isDarkMode = isDarkMode)
                            }
                        }

                        if (selectedCategory != null) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    FilterInfoCard(
                                        isDarkMode = isDarkMode,
                                        categoryName = selectedCategory!!.name,
                                        itemCount = newsItems.size,
                                        onClearFilter = { viewModel.clearCategoryFilter() }
                                    )
                                }
                            }
                        }

                        items(
                            items = newsItems,
                            key = { it.news_item_id },
                            contentType = { "NewsCard" }  // MicroOpti: Define el tipo de contenido
                        ) { item ->
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                NewsCard(
                                    isDarkMode = isDarkMode,
                                    item = item,
                                    categories = categories,
                                    onClick = { onNewsItemClick(item.news_item_id) }
                                )
                            }
                        }

                        if (newsItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = "📦 Using cached data for faster loading",
                                    fontSize = 12.sp,
                                    color = if (isDarkMode) Color(0xFF808080) else Color(0xFF999999),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 16.dp)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = connectionRestored,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                ConnectionRestoredBanner(
                    onDismiss = { viewModel.dismissConnectionRestored() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDialog(isDarkMode: Boolean,categories: List<Category>,viewModel: NewsFeedViewModel = androidx.lifecycle.viewmodel.compose.viewModel(), onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var URL by remember { mutableStateOf("") }
    var Author_type by remember{ mutableStateOf("") }
    var Author_institution by remember{ mutableStateOf("") }
    var Description by remember { mutableStateOf("") }
    var showSuccessSnackbar by remember {mutableStateOf(false)}
    var message by remember { mutableStateOf<String?>(null) }

    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)


    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }



    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp).verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Add an article", style = MaterialTheme.typography.titleLarge, color = textColor)
                Spacer(modifier = Modifier.height(16.dp))

                // Dropdown menu for categories
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Select a Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = URL,
                    onValueChange = { URL = it },
                    label = { Text("URL") },
                    modifier = Modifier
                        .fillMaxWidth()

                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {


                            // Handle submission logic here
                            viewModel.AddNews(

                                URL,



                                selectedCategory!!.category_id,
                                onSuccess = {
                                    showSuccessSnackbar = true
                                    onDismiss()
                                },
                                onWait = {message = "noticia encolada posterior envio"},
                                onError = {},
                            )
                            onDismiss()
                        },
                    ) {
                        Text("Submit")
                    }
                    message?.let {
                        Text(
                            it,
                            color = secondaryTextColor,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun ConnectionRestoredBanner(
    onDismiss: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF4CAF50),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Connection restored",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Connection restored",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Refreshing news...",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryTabsFromSupabase(
    isDarkMode: Boolean = false,
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
                isDarkMode = isDarkMode,
                text = "All",
                selected = selectedCategory == null,
                onClick = onClearFilter
            )
        }

        items(categories) { category ->
            CategoryChip(
                isDarkMode = isDarkMode,
                text = category.name,
                selected = selectedCategory?.category_id == category.category_id,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
fun FilterInfoCard(
    isDarkMode: Boolean = false,
    categoryName: String,
    itemCount: Int,
    onClearFilter: () -> Unit
) {
    val cardColor = if (isDarkMode) Color(0xFF1A3A5C) else Color(0xFFE3F2FD)
    val textColor = if (isDarkMode) Color(0xFF90CAF9) else Color(0xFF1976D2)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
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
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Filtered by: $categoryName",
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "$itemCount ${if (itemCount == 1) "article" else "articles"} found",
                        fontSize = 11.sp,
                        color = textColor
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
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun FeedHeader(
    isDarkMode: Boolean = false,
    cacheStatus: String = ""
) {
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
    val tertiaryTextColor = if (isDarkMode) Color(0xFF808080) else Color(0xFF999999)
    val dividerColor = if (isDarkMode) Color(0xFF333333) else Color(0xFFE0E0E0)

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
                    tint = textColor
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Punto Neutro",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            if (cacheStatus.isNotEmpty()) {
                Text(
                    text = cacheStatus,
                    fontSize = 11.sp,
                    color = tertiaryTextColor,
                    modifier = Modifier.padding(start = 32.dp, top = 2.dp)
                )
            }
        }

        Box {
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = textColor
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
        StatItem(isDarkMode = isDarkMode, value = "1,247", label = "Verified today")
        StatItem(isDarkMode = isDarkMode, value = "25", label = "Fake detected", color = Color(0xFFE53935))
        StatItem(isDarkMode = isDarkMode, value = "158", label = "Verifying")
    }

    Divider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = dividerColor
    )
}

@Composable
fun StatItem(
    isDarkMode: Boolean = false,
    value: String,
    label: String,
    color: Color? = null
) {
    val textColor = color ?: if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = secondaryTextColor
        )
    }
}

@Composable
fun SearchBar(
    isDarkMode: Boolean = false,
    query: String,
    onQueryChange: (String) -> Unit
) {
    val containerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF8F8F8)
    val borderColor = if (isDarkMode) Color(0xFF404040) else Color(0xFFDDDDDD)
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val placeholderColor = if (isDarkMode) Color(0xFF808080) else Color(0xFFAAAAAA)
    val iconColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
    val filterBgColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF0F0F0)

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
            placeholder = { Text("Search news...", color = placeholderColor) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = iconColor
                )
            },
            trailingIcon = {
                // fixed : Botón para limpiar búsqueda
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = iconColor
                        )
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor,
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            singleLine = true
        )

        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = { },
            modifier = Modifier
                .size(48.dp)
                .background(filterBgColor, RoundedCornerShape(12.dp))
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter",
                tint = textColor
            )
        }
    }
}

@Composable
fun CategoryChip(
    isDarkMode: Boolean = false,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val selectedBgColor = if (isDarkMode) Color(0xFF9C27B0) else Color(0xFF1A1A1A)
    val unselectedTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) selectedBgColor else Color.Transparent,
        border = if (!selected) ButtonDefaults.outlinedButtonBorder else null
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (selected) Color.White else unselectedTextColor,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun MisinformationAlert(isDarkMode: Boolean = false) {
    val cardColor = if (isDarkMode) Color(0xFF3E2723) else Color(0xFFFFF3E0)
    val textColor = if (isDarkMode) Color(0xFFFFCC80) else Color(0xFF8B4513)
    val iconColor = if (isDarkMode) Color(0xFFFFB74D) else Color(0xFFFF6F00)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Misinformation Alert",
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "3 fake news stories detected about health topics. Verify sources before sharing.",
                    fontSize = 12.sp,
                    color = textColor,
                    lineHeight = 16.sp
                )
            }
        }
        TextButton(
            onClick = { },
            modifier = Modifier.padding(start = 52.dp, bottom = 8.dp)
        ) {
            Text("View details", color = textColor, fontSize = 13.sp)
        }
    }
}



@Composable
fun NewsCard(
    isDarkMode: Boolean = false,
    item: NewsItem,
    categories: List<Category>,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val itemCategory = categories.find { it.category_id == item.category_id }
    val categoryName = itemCategory?.name ?: "General"
    val categoryColor = getCategoryColorDynamic(item.category_id)

    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
    val tertiaryTextColor = if (isDarkMode) Color(0xFF808080) else Color(0xFF888888)
    val dividerColor = if (isDarkMode) Color(0xFF333333) else Color(0xFFE0E0E0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.image_url)
                        .crossfade(true)  // Transición suave MicroOpti
                        .memoryCachePolicy(CachePolicy.ENABLED)  // MicroOpti: Cache en RAM
                        .diskCachePolicy(CachePolicy.ENABLED)    // MicroOpti: Cache en disco
                        .build(),
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
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = item.short_description,
                    fontSize = 14.sp,
                    color = secondaryTextColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.is_verifiedSource) Icons.Default.Verified else Icons.Default.Person,
                        contentDescription = "Author",
                        tint = if (item.is_verifiedSource) Color(0xFF4CAF50) else tertiaryTextColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Verified author",
                        fontSize = 13.sp,
                        color = secondaryTextColor
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.author_institution.ifEmpty { "Unknown Source" },
                        fontSize = 12.sp,
                        color = tertiaryTextColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " • ${item.days_since} days ago",
                        fontSize = 12.sp,
                        color = tertiaryTextColor
                    )
                }

                Spacer(Modifier.height(12.dp))

                Divider(color = dividerColor)

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconWithText(
                            isDarkMode = isDarkMode,
                            icon = Icons.Outlined.ChatBubbleOutline,
                            text = "${item.total_ratings}"
                        )
                        IconWithText(
                            isDarkMode = isDarkMode,
                            icon = Icons.Outlined.Share,
                            text = ""
                        )
                        IconWithText(
                            isDarkMode = isDarkMode,
                            icon = Icons.Outlined.BookmarkBorder,
                            text = ""
                        )
                    }

                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) Color(0xFF9C27B0) else Color(0xFF1A1A1A)
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
fun IconWithText(
    isDarkMode: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    val iconColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        if (text.isNotEmpty()) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = iconColor
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
        score >= 0.6 -> Color(0xFFC107)
        else -> Color(0xFFE53935)
    }
}

@Composable
fun BottomNavigationBar(
    isDarkMode: Boolean = false,
    onNavigateToGuide: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White

    NavigationBar(
        containerColor = containerColor,
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

@Composable
fun NoSearchResultsBanner(
    isDarkMode: Boolean = false,
    searchQuery: String,
    onClearSearch: () -> Unit
) {
    val bannerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
    val iconColor = if (isDarkMode) Color(0xFF9C27B0) else Color(0xFF1976D2)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bannerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = "No results",
                tint = iconColor,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "No matches found",
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "No news found for \"$searchQuery\"",
                    fontSize = 14.sp,
                    color = secondaryTextColor,
                    lineHeight = 18.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onClearSearch) {
                Text(
                    "Clear",
                    color = iconColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}