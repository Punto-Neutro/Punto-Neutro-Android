package com.example.sprint_2_kotlin.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(
    isDarkMode: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    // Colores dinámicos según el tema
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
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
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { /* TODO: Notifications */ }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    text = "Guide to Identify Fake News",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Learn to detect misinformation and manipulated content",
                    fontSize = 14.sp,
                    color = secondaryTextColor,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(24.dp))
            }

            item {
                GuideTipCard(
                    isDarkMode = isDarkMode,
                    icon = Icons.Default.RemoveRedEye,
                    title = "Examine the source",
                    level = "Basic",
                    description = "Verify if the website is known and reliable. Check the 'About' section to learn more about the organization."
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                GuideTipCard(
                    isDarkMode = isDarkMode,
                    icon = Icons.Default.Search,
                    title = "Search multiple sources",
                    level = "Basic",
                    description = "Real news is usually reported by multiple reliable media outlets. If it only appears on one site, be suspicious."
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                GuideTipCard(
                    isDarkMode = isDarkMode,
                    icon = Icons.Default.Person,
                    title = "Verify the author",
                    level = "Intermediate",
                    description = "Look up information about the journalist or author. Professionals usually have verifiable online presence.",
                    levelColor = Color(0xFFFFA726)
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                GuideTipCard(
                    isDarkMode = isDarkMode,
                    icon = Icons.Default.Warning,
                    title = "Beware of sensationalist headlines",
                    level = "Basic",
                    description = "Overly emotional titles or those with ALL CAPS are usually indicators of questionable content."
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                GuideTipCard(
                    isDarkMode = isDarkMode,
                    icon = Icons.Default.CheckCircle,
                    title = "Check the dates",
                    level = "Basic",
                    description = "Make sure the information is current. Sometimes old news is recycled as if it were recent."
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                GuideTipCard(
                    isDarkMode = isDarkMode,
                    icon = Icons.Default.Shield,
                    title = "Use verification tools",
                    level = "Advanced",
                    description = "Use fact-checking sites like Snopes, PolitiFact, or Punto Neutro's integrated tools.",
                    levelColor = Color(0xFF1976D2)
                )
                Spacer(Modifier.height(24.dp))
            }

            item {
                WarningSigns(isDarkMode = isDarkMode)
                Spacer(Modifier.height(24.dp))
            }

            item {
                VerificationProcess(isDarkMode = isDarkMode)
                Spacer(Modifier.height(24.dp))
            }

            item {
                RecommendedTools(isDarkMode = isDarkMode)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun GuideTipCard(
    isDarkMode: Boolean = false,
    icon: ImageVector,
    title: String,
    level: String,
    description: String,
    levelColor: Color = Color(0xFF757575)
) {
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
    val iconBgColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = iconBgColor,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = textColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = levelColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = level,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = levelColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = description,
                fontSize = 14.sp,
                color = secondaryTextColor,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun WarningSigns(isDarkMode: Boolean = false) {
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val itemTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF333333)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Warning Signs",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(Modifier.height(16.dp))

            WarningItem(isDarkMode = isDarkMode, text = "Strange URLs or many spelling errors")
            Spacer(Modifier.height(8.dp))
            WarningItem(isDarkMode = isDarkMode, text = "Images that don't match the text")
            Spacer(Modifier.height(8.dp))
            WarningItem(isDarkMode = isDarkMode, text = "Missing date or clearly identified author")
        }
    }
}

@Composable
fun WarningItem(isDarkMode: Boolean = false, text: String) {
    val textColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF333333)

    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .offset(y = 7.dp)
                .background(Color(0xFFE53935), CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = textColor,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun VerificationProcess(isDarkMode: Boolean = false) {
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Process",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Verification Process",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(Modifier.height(16.dp))

            ProcessStep(
                isDarkMode = isDarkMode,
                number = "1",
                title = "First reading",
                description = "Read the title and first paragraph. Does it seem credible to you?"
            )
            Spacer(Modifier.height(12.dp))

            ProcessStep(
                isDarkMode = isDarkMode,
                number = "2",
                title = "Verify the source",
                description = "Research who published the information and their reputation."
            )
            Spacer(Modifier.height(12.dp))

            ProcessStep(
                isDarkMode = isDarkMode,
                number = "3",
                title = "Look for corroboration",
                description = "Search for the same news in other reliable media outlets."
            )
            Spacer(Modifier.height(12.dp))

            ProcessStep(
                isDarkMode = isDarkMode,
                number = "4",
                title = "Analyze the images",
                description = "Use reverse image search to verify their origin."
            )
            Spacer(Modifier.height(12.dp))

            ProcessStep(
                isDarkMode = isDarkMode,
                number = "5",
                title = "Consult experts",
                description = "Look for opinions from specialists on the topic covered."
            )
        }
    }
}

@Composable
fun ProcessStep(
    isDarkMode: Boolean = false,
    number: String,
    title: String,
    description: String
) {
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
    val numberBgColor = if (isDarkMode) Color(0xFF9C27B0) else Color(0xFF1A1A1A)

    Row(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = CircleShape,
            color = numberBgColor,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = secondaryTextColor,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun RecommendedTools(isDarkMode: Boolean = false) {
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Tools",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Recommended Tools",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(Modifier.height(16.dp))

            ToolCard(
                isDarkMode = isDarkMode,
                title = "Punto Neutro AI",
                description = "Our integrated automatic verification system.",
                icon = Icons.Default.SmartToy
            )

            Spacer(Modifier.height(12.dp))

            ToolCard(
                isDarkMode = isDarkMode,
                title = "Reverse Search",
                description = "Verify the origin and authenticity of images.",
                icon = Icons.Default.ImageSearch
            )
        }
    }
}

@Composable
fun ToolCard(
    isDarkMode: Boolean = false,
    title: String,
    description: String,
    icon: ImageVector
) {
    val bgColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF8F8F8)
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = secondaryTextColor
                )
            }
        }
    }
}