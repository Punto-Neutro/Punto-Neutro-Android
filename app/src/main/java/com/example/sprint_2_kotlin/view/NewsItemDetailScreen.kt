package com.example.sprint_2_kotlin.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.sprint_2_kotlin.viewmodel.NewsItemDetailViewModel
import com.example.sprint_2_kotlin.viewmodel.BookmarkViewModel  // bookmark
import utils.NetworkMonitor
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalUriHandler // Add this import
import androidx.compose.foundation.clickable // Ensure this is imported
import androidx.compose.ui.res.stringResource
import com.example.sprint_2_kotlin.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsItemDetailScreen(
    isDarkMode: Boolean = false,
    userProfileId: Int,
    newsItemId: Int,
    onBackClick: () -> Unit = {},
    onShareClick: (String, String) -> Unit = { _, _ -> }, // Add this parameter
    viewModel: NewsItemDetailViewModel = viewModel(),
    bookmarkViewModel: BookmarkViewModel = viewModel()  // bookmark
) {

    // Inside NewsItemDetailScreen function
    val uriHandler = LocalUriHandler.current // 1. Get the URI handler

    val context = LocalContext.current
    val networkMonitor = remember { NetworkMonitor(context) }

    LaunchedEffect(newsItemId) {
        viewModel.loadNewsItemById(newsItemId)
    }

    LaunchedEffect(Unit) {
        viewModel.startNetworkObserver(networkMonitor, newsItemId)
    }

    // Register read history when news item is loaded
    val currentItem by viewModel.newsItem.collectAsState()
    LaunchedEffect(currentItem) {
        currentItem?.let { newsItem ->
            viewModel.registerReadHistory(newsItem)
        }
    }

    val ratings by viewModel.ratings.collectAsState()

    //  Estado del bookmark
    var isBookmarked by remember { mutableStateOf(false) }

    LaunchedEffect(newsItemId) {
        currentItem?.let {
            isBookmarked = bookmarkViewModel.isBookmarked(newsItemId)
        }
    }

    // Colores dinámicos según el tema
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
    val primaryColor = if (isDarkMode) Color(0xFF9C27B0) else Color(0xFF1976D2)


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.News_Details),
                        color = textColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to News Feed",
                            tint = textColor
                        )
                    }
                },
                actions = {
                    // ✅ NUEVO: Botón de Bookmark
                    IconButton(
                        onClick = {
                            currentItem?.let { newsItem ->
                                bookmarkViewModel.toggleBookmark(newsItem)
                                isBookmarked = !isBookmarked
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                            tint = if (isBookmarked) Color(0xFFFFA726) else textColor
                        )
                    }

                    IconButton(
                        onClick = {
                            currentItem?.let { newsItem ->
                                val url = newsItem.original_source_url
                                onShareClick(url, newsItem.title)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
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
        currentItem?.let { item ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Image
                    Image(
                        painter = rememberAsyncImagePainter(item.image_url),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = item.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category and reliability
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReliabilityIndicator(item.average_reliability_score)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Meta info
                    Text(
                        text = stringResource(R.string.By) + " ${item.author_type} "+ stringResource(R.string.at) + " ${item.author_institution}",
                        fontSize = 14.sp,
                        color = secondaryTextColor
                    )
                    Text(
                        text =stringResource(R.string.Published) + " ${item.days_since} ${stringResource(R.string.days_ago)} • ${item.total_ratings} ${stringResource(R.string.total_ratings)}",
                        fontSize = 12.sp,
                        color = secondaryTextColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Long description
                    Text(
                        text = item.long_description.ifEmpty { item.short_description },
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (item.original_source_url.isNotEmpty()) {
                        Text(
                            text = "${stringResource(R.string.Source)}: ${item.original_source_url}",
                            fontSize = 12.sp,
                            color = primaryColor,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .clickable {
                                    // 2. Open the URL when clicked
                                    try {
                                        uriHandler.openUri(item.original_source_url)
                                    } catch (e: Exception) {
                                        // Handle cases where the URL might be malformed
                                    }
                                }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Box {
                        CommentSection(
                            isDarkMode = isDarkMode,
                            userProfileId = userProfileId,
                            newsItemId = newsItemId
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.Ratings_and_comments),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Ratings list
                items(ratings) { rating ->
                    RatingItemCard(
                        rating = rating,
                        isDarkMode = isDarkMode
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }

        } ?: run {
            // Loading state while fetching news item
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = if (isDarkMode) Color(0xFF9C27B0) else Color(0xFF1A1A1A)
                )
            }
        }
    }
}

@Composable
fun CommentSection(
    isDarkMode: Boolean = false,
    viewModel: NewsItemDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    userProfileId: Int,
    newsItemId: Int
) {
    var isExpanded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var rating by remember { mutableStateOf<Float>(value = 0.5f) }

    // Colores dinámicos
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
    val buttonColor = if (isDarkMode) Color(0xFF9C27B0) else Color(0xFF1A1A1A)

    Column(Modifier.padding(16.dp)) {

        Button(
            onClick = { isExpanded = !isExpanded },
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor
            )
        ) {
            Text(if (isExpanded) "Cancelar" else "Agregar comentario")
        }

        AnimatedVisibility(isExpanded) {
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor)
            ) {
                Column(Modifier.padding(16.dp)) {

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text(stringResource(R.string.Comment), color = secondaryTextColor) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = if (isDarkMode) Color(0xFF9C27B0) else Color(0xFF1A1A1A),
                            unfocusedBorderColor = if (isDarkMode) Color(0xFF404040) else Color(0xFFDDDDDD)
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.Value) + ": ${(rating * 100).toInt()}",

                        fontSize = 14.sp,
                        color = textColor
                    )
                    Slider(
                        value = rating,
                        onValueChange = { rating = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = if (isDarkMode) Color(0xFF9C27B0) else Color(0xFF1A1A1A),
                            activeTrackColor = if (isDarkMode) Color(0xFF9C27B0) else Color(0xFF1A1A1A),
                            inactiveTrackColor = if (isDarkMode) Color(0xFF404040) else Color(0xFFE0E0E0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.addComment(
                                userProfileId = userProfileId,
                                newsItemId = newsItemId,
                                comment = comment,
                                onSuccess = {
                                    message = "Comentario enviado ✅"
                                    isExpanded = false
                                    name = ""
                                    comment = ""
                                },
                                onError = { message = "Error al enviar: ${it.message}" },
                                onWait = {
                                    message = "Comentario encolado posterior envio"
                                    isExpanded = false
                                    name = ""
                                    comment = ""
                                },
                                rating = rating.toDouble()
                            )
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor
                        )
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