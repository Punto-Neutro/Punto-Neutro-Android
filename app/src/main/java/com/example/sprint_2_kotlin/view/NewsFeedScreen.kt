package com.example.sprint_2_kotlin.view

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.forEach
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.res.stringResource
import com.example.sprint_2_kotlin.R
import com.example.sprint_2_kotlin.model.data.Country
import com.example.sprint_2_kotlin.model.data.PQRS_types
import utils.getTranslatedCategoryName
import utils.getTranslatedCountryName
import utils.getTranslatedPQRStypeame


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
    val newsItems: List<NewsItem> by viewModel.newsItems.collectAsState()
    val lazyListState = rememberLazyListState() // Add this state

    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val cacheStatus by viewModel.cacheStatus.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val countries by viewModel.countries.collectAsState()
    val pqrstypes by viewModel.pqrstypes.collectAsState()
    val connectionRestored by viewModel.connectionRestored.collectAsState()
    val noSearchResults by viewModel.noSearchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    val selectedCountryIds by viewModel.selectedCountryIds.collectAsState()
    val newsScope by viewModel.newsScope.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) } // State to control the dialog


    var showDialogPQRS by remember { mutableStateOf(false) } // State to control the dialog
// Add this check to display the dialog
    if (showFilterDialog) {
        FilterDialog(
            isDarkMode = isDarkMode,
            countries = countries,
            selectedCountryIds = selectedCountryIds,
            selectedScope = newsScope,
            onCountrySelected = viewModel::onCountrySelected,
            onScopeSelected = viewModel::onNewsScopeSelected,
            onApply = viewModel::applyFilters,
            onClear = viewModel::clearAllFilters,
            onDismiss = { showFilterDialog = false }
        )
    }


    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf false

            // Load more when user is 5 items away from the bottom
            lastVisibleItem.index >= lazyListState.layoutInfo.totalItemsCount - 5
        }
    }



    if (showDialog) {
        FeedbackDialog(
            isDarkMode = isDarkMode,
            categories = categories,
            countries = countries,
            onDismiss = { showDialog = false }

        )
    }

    if (showDialogPQRS) {
        PQRsDialog(
            isDarkMode = isDarkMode,
            categories = categories,
            pqrstypes = pqrstypes ,
            onDismiss = { showDialogPQRS = false }

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

    LaunchedEffect(newsItems.isEmpty() && !isLoading && !isRefreshing) {
        if (newsItems.isEmpty() && !isLoading && !isRefreshing) {
            viewModel.loadNewsItems(false)
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !newsItems.isEmpty()  ) {
            viewModel.loadNextPage()
        }
    }










    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            // Use a Box to position multiple FABs
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp) // Offset for the system navigation bar
            ) {
                // Left FAB (PQRS)
                FloatingActionButton(
                    onClick = { showDialogPQRS = true },
                    containerColor = if (isDarkMode) Color(0xFF9C27B0) else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomStart) // Align to Bottom Left
                ) {
                    Icon(Icons.Filled.AddReaction, "Add PQRS")
                }

                // Right FAB (Feedback/Article)
                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = if (isDarkMode) Color(0xFF9C27B0) else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd) // Align to Bottom Right
                ) {
                    Icon(Icons.Filled.PostAdd, "Add Article")
                }
            }
        },

        bottomBar = {
            BottomNavigationBar(
                isDarkMode = isDarkMode,
                onNavigateToGuide = onNavigateToGuide,
                onNavigateToProfile = onNavigateToProfile
            )
        }
    ) // NewsFeedScreen.kt - Update your paddingValues block

 { paddingValues ->
    Box(modifier = Modifier.padding(paddingValues)) {
        SwipeRefresh(state = rememberSwipeRefreshState(isRefreshing),
            onRefresh = { viewModel.refreshNewsFeed() }
        ) {
            // REMOVE: The if (isLoading && newsItems.isEmpty()) check from here

            LazyColumn(
                state = lazyListState,
                modifier = modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            ) {
                // 1. Header items (Header, Search, Tabs)
                item {
                    Column(modifier = Modifier.fillMaxWidth().background(surfaceColor)) {
                        FeedHeader(isDarkMode = isDarkMode, cacheStatus = cacheStatus)
                        SearchBar(isDarkMode = isDarkMode, query = searchQuery, onQueryChange = { viewModel.updateSearchQuery(it) }, onFilterClick = { showFilterDialog = true })
                        CategoryTabsFromSupabase(
                            isDarkMode = isDarkMode,
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { viewModel.selectCategory(it) },
                            onClearFilter = { viewModel.clearCategoryFilter() }
                        )
                    }
                }

                // 2. INITIAL LOADING STATE (Now inside the list)
                // This fills the screen but KEEPS the LazyColumn alive in the UI tree
                // NewsFeedScreen.kt inside LazyColumn

                when {
                    // ONLY show the fullscreen loader if we have NO items AND we are loading for the first time.
                    // If newsItems has data, we NEVER show this Box, so the list stays in the UI tree.
                    isLoading && newsItems.isEmpty() -> {
                        item {
                            Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = if (isDarkMode) Color(0xFF9C27B0) else Color(0xFF1A1A1A))
                            }
                        }
                    }

                    // Only show "No News" if we are NOT loading and we actually have nothing.


                    else -> {
                        if (noSearchResults) {
                            item { NoSearchResultsBanner(isDarkMode, searchQuery) { viewModel.clearSearch() } }
                        }


                        // This is the core content. As long as this is rendered, scroll position is safe.
                        items(
                            items = newsItems,
                            key = { it.news_item_id },
                            contentType = { "NewsCard" }
                        ) { item ->
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                NewsCard(isDarkMode, item, categories) { onNewsItemClick(item.news_item_id) }
                            }
                        }

                        // Pagination loader at the bottom
                        if (isLoading && newsItems.isNotEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDialog(isDarkMode: Boolean, categories: List<Category>, countries: List<Country>, viewModel: NewsFeedViewModel = viewModel(), onDismiss: () -> Unit) {
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


    var expandedcategory by remember { mutableStateOf(false) }

    var expandedcountry by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedCountry by remember { mutableStateOf<Country?>(null) }




    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.Add_an_article), style = MaterialTheme.typography.titleLarge, color = textColor)
                Spacer(modifier = Modifier.height(16.dp))

                // Dropdown menu for categories
                ExposedDropdownMenuBox(
                    expanded = expandedcategory,
                    onExpandedChange = { expandedcategory = !expandedcategory }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.let { getTranslatedCategoryName(it.name) } ?: stringResource(R.string.Select_Category),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.Category)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedcategory)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedcategory,
                        onDismissRequest = { expandedcategory = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(getTranslatedCategoryName(category.name)) },
                                onClick = {
                                    selectedCategory = category
                                    expandedcategory = false
                                }
                            )
                        }
                    }
                }



                //Dropdown menu for Countries

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedcountry,
                    onExpandedChange = { expandedcountry = !expandedcountry }
                ) {
                    OutlinedTextField(
                        value =  selectedCountry?.let { getTranslatedCountryName(it.country_name) } ?: stringResource(R.string.Select_Your_Country),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.Country)) },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedcountry)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedcountry,
                        onDismissRequest = { expandedcountry = false }
                    ) {
                        countries.forEach { country ->
                            DropdownMenuItem(
                                text = { Text(getTranslatedCountryName(country.country_name)) },
                                onClick = {
                                    selectedCountry = country
                                    expandedcountry = false
                                    Log.d(TAG, "Selected country = ${selectedCountry?.country_name}")
                                    Log.d(TAG, "Selected country id= ${selectedCountry?.id}")

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
                        Text(stringResource(R.string.Cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {


                            // Handle submission logic here
                            viewModel.AddNews(

                                URL,



                                selectedCategory!!.category_id,
                                selectedCountry!!.id,

                                onSuccess = {
                                    showSuccessSnackbar = true
                                    onDismiss()
                                },
                                onWait = {message = ":D"},
                                onError = {},
                            )
                            onDismiss()
                        },
                    ) {
                        Text(stringResource(R.string.Submit))
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PQRsDialog(isDarkMode: Boolean, categories: List<Category>, pqrstypes: List<PQRS_types>, viewModel: NewsFeedViewModel = viewModel(), onDismiss: () -> Unit) {
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


    var expandedpqrstype by remember { mutableStateOf(false) }
    var selectedpqrstype by remember { mutableStateOf<PQRS_types?>(null) }




    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.Submit_a_request), style = MaterialTheme.typography.titleLarge, color = textColor)
                Spacer(modifier = Modifier.height(16.dp))

                // Dropdown menu for categories
                ExposedDropdownMenuBox(
                    expanded = expandedpqrstype,
                    onExpandedChange = { expandedpqrstype = !expandedpqrstype }
                ) {
                    OutlinedTextField(
                        value =  selectedpqrstype?.let { getTranslatedPQRStypeame(it.name) } ?: stringResource(R.string.Select_a_type),
                        onValueChange = {},
                        readOnly = true,
                        label = {Text(stringResource(R.string.Type)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedpqrstype)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedpqrstype,
                        onDismissRequest = { expandedpqrstype = false }
                    ) {
                        pqrstypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(getTranslatedPQRStypeame(type.name)) },
                                onClick = {
                                    selectedpqrstype = type
                                    expandedpqrstype = false
                                }
                            )
                        }
                    }
                }




                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = Description,
                    onValueChange = { Description = it },
                    label = { Text(stringResource(R.string.Description)) },
                    modifier = Modifier
                        .fillMaxWidth()

                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.Cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {


                            // Handle submission logic here
                            viewModel.AddPQRS(

                                description = Description  ,



                                selectedpqrstype!!.id,

                                onSuccess = {
                                    showSuccessSnackbar = true
                                    onDismiss()
                                },
                                onWait = {message = ":D"},
                                onError = {},
                            )
                            onDismiss()
                        },
                    ) {
                        Text(stringResource(R.string.Submit))
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
                    contentDescription = stringResource(R.string.Connection_restored),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.Connection_restored),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.Refreshing_news),
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
                    contentDescription = stringResource(R.string.Submit),
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
                text = stringResource(R.string.All),
                selected = selectedCategory == null,
                onClick = onClearFilter
            )
        }

        items(categories) { category ->
            CategoryChip(
                isDarkMode = isDarkMode,
                text = getTranslatedCategoryName(category.name),
                selected = selectedCategory?.category_id == category.category_id,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

// Add this new composable at the end of the file

@Composable
fun FilterDialog(
    isDarkMode: Boolean,
    countries: List<Country>,
    selectedCountryIds: Set<Int>,
    selectedScope: String,
    onCountrySelected: (Int, Boolean) -> Unit,
    onScopeSelected: (String) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
    val primaryColor = if (isDarkMode) Color(0xFF9C27B0) else MaterialTheme.colorScheme.primary

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            modifier = Modifier.heightIn(max = 600.dp) // Limit height
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.Filter), style = MaterialTheme.typography.titleLarge, color = textColor)
                Spacer(Modifier.height(16.dp))

                // Scope Selection (Local / International)
                Text(stringResource(R.string.By_scope), fontWeight = FontWeight.SemiBold, color = textColor)
                Spacer(modifier = Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScopeChip(stringResource(R.string.All), selectedScope == "All", isDarkMode) { onScopeSelected("All") }
                    ScopeChip(stringResource(R.string.Local), selectedScope == "Local", isDarkMode) { onScopeSelected("Local") }
                    ScopeChip(stringResource(R.string.International), selectedScope == "International", isDarkMode) { onScopeSelected("International") }
                }

                Divider(Modifier.padding(vertical = 16.dp))

                // Country Selection
                Text(stringResource(R.string.Filter_by_country), fontWeight = FontWeight.SemiBold, color = textColor)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(countries.sortedBy { it.country_name }) { country ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onCountrySelected(country.id, country.id !in selectedCountryIds) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = country.id in selectedCountryIds,
                                onCheckedChange = { isChecked -> onCountrySelected(country.id, isChecked) },
                                colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                            )
                            Text(
                                text = getTranslatedCountryName(country.country_name),
                                color = textColor,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Action Buttons
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        onClear()
                        onDismiss()
                    }) {
                        Text(stringResource(R.string.Clear_all), color = secondaryTextColor)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        onApply()
                        onDismiss()
                    }) {
                        Text(stringResource(R.string.Submit))
                    }
                }
            }
        }
    }
}

@Composable
fun ScopeChip(text: String, isSelected: Boolean, isDarkMode: Boolean, onClick: () -> Unit) {
    val selectedBgColor = if (isDarkMode) Color(0xFF9C27B0) else Color(0xFF1A1A1A)
    val unselectedTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) selectedBgColor else Color.Transparent,
        border = if (!isSelected) ButtonDefaults.outlinedButtonBorder else null,
        modifier = Modifier.height(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = text,
                color = if (isSelected) Color.White else unselectedTextColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
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
                    contentDescription = stringResource(R.string.notifications),
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
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit

) {
    val containerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF8F8F8)
    val borderColor = if (isDarkMode) Color(0xFF404040) else Color(0xFFDDDDDD)
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val placeholderColor = if (isDarkMode) Color(0xFF808080) else Color(0xFFAAAAAA)
    val iconColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
    val filterBgColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF0F0F0)

    var showFilters by remember { mutableStateOf(false) }
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
            placeholder = { Text(stringResource(R.string.Search_news), color = placeholderColor) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.Search),
                    tint = iconColor
                )
            },
            trailingIcon = {
                // fixed : Botón para limpiar búsqueda
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.Clear_search),
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
            onClick = { onFilterClick()},
            modifier = Modifier
                .size(48.dp)
                .background(filterBgColor, RoundedCornerShape(12.dp))
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = stringResource(R.string.Filter),
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
                            text = getTranslatedCategoryName(categoryName),
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
                                contentDescription = stringResource(R.string.Reliability),
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
                        contentDescription = stringResource(R.string.Author),
                        tint = if (item.is_verifiedSource) Color(0xFF4CAF50) else tertiaryTextColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.Verified_author),
                        fontSize = 13.sp,
                        color = secondaryTextColor
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.author_institution.ifEmpty { stringResource(R.string.Unknown_source) },
                        fontSize = 12.sp,
                        color = tertiaryTextColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " • ${item.days_since} ${stringResource(R.string.days_ago)}",
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
                            text = stringResource(R.string.Read_more),
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
    icon: ImageVector,
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
        tonalElevation = 8.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text(stringResource(R.string.Home), fontSize = 12.sp) },
            selected = true,
            onClick = { }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.MenuBook, contentDescription = "Guide") },
            label = { Text(stringResource(R.string.Guide), fontSize = 12.sp) },
            selected = false,
            onClick = onNavigateToGuide
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile") },
            label = { Text(stringResource(R.string.Profile), fontSize = 12.sp) },
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
                contentDescription = stringResource(R.string.No_results),
                tint = iconColor,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.No_matches_found),
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${stringResource(R.string.No_news_found_for)} \"$searchQuery\"",
                    fontSize = 14.sp,
                    color = secondaryTextColor,
                    lineHeight = 18.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onClearSearch) {
                Text(
                    stringResource(R.string.Clear),
                    color = iconColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

