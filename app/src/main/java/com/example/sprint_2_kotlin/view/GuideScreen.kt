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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sprint_2_kotlin.R

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
                    text = stringResource(R.string.Guide_to_Identify_Fake_News),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.Learn_to_detect_misinformation_and_manipulated_content),
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
                    title = stringResource(R.string.Examine_the_source),
                    level = stringResource(R.string.Basic),
                    description = stringResource(R.string.Verify_if_the_website_is_known_and_reliable__Check_the_About_section_to_learn_more_about_the_organization_)
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                GuideTipCard(
                    isDarkMode = isDarkMode,
                    icon = Icons.Default.Search,
                    title = stringResource(R.string.Search_multiple_sources),
                    level = stringResource(R.string.Basic),
                    description = stringResource(R.string.Real_)
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                GuideTipCard(
                    isDarkMode = isDarkMode,
                    icon = Icons.Default.Person,
                    title = stringResource(R.string.Verify_the_author),
                    level = stringResource(R.string.Intermediate),
                    description = stringResource(R.string.Look_up_info),
                    levelColor = Color(0xFFFFA726)
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                GuideTipCard(
                    isDarkMode = isDarkMode,
                    icon = Icons.Default.Warning,
                    title = stringResource(R.string.Beware_of_sensationalist_headlines),
                    level = stringResource(R.string.Basic),
                    description = stringResource(R.string.Overly_emotional_titles_or_those_with_ALL_CAPS_are_usually_indicators_of_questionable_content_)
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                GuideTipCard(
                    isDarkMode = isDarkMode,
                    icon = Icons.Default.CheckCircle,
                    title = stringResource(R.string.Check_the_dates),
                    level = stringResource(R.string.Basic),
                    description = stringResource(R.string.Make_sure_the_information_)
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                GuideTipCard(
                    isDarkMode = isDarkMode,
                    icon = Icons.Default.Shield,
                    title = stringResource(R.string.Use_verification_tools),
                    level = stringResource(R.string.Advanced),
                    description = stringResource(R.string.Use_fact_checking_sites_),
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
                    text = stringResource(R.string.Warning_Signs),
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
                    text = stringResource(R.string.Verification_Process),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(Modifier.height(16.dp))

            ProcessStep(
                isDarkMode = isDarkMode,
                number = "1",
                title = stringResource(R.string.First_reading),
                description = stringResource(R.string.Read_the_title_and_first_paragraph_)
            )
            Spacer(Modifier.height(12.dp))

            ProcessStep(
                isDarkMode = isDarkMode,
                number = "2",
                title = stringResource(R.string.Verify_the_source),
                description = stringResource(R.string.Research_who_published_the_information_and_their_reputation_)
            )
            Spacer(Modifier.height(12.dp))

            ProcessStep(
                isDarkMode = isDarkMode,
                number = "3",
                title = stringResource(R.string.Look_for_corroboration),
                description = stringResource(R.string.Search_for_the_same_news_in_other_reliable_media_outlets_)
            )
            Spacer(Modifier.height(12.dp))

            ProcessStep(
                isDarkMode = isDarkMode,
                number = "4",
                title = stringResource(R.string.Analyze_the_images),
                description = stringResource(R.string.Use_reverse_image_search_to_verify_their_origin_)
            )
            Spacer(Modifier.height(12.dp))

            ProcessStep(
                isDarkMode = isDarkMode,
                number = "5",
                title = stringResource(R.string.Consult_experts),
                description = stringResource(R.string.Look_for_opinions_from_specialists_on_the_topic_covered_)
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
                    text = stringResource(R.string.Recommended_Tools),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(Modifier.height(16.dp))

            ToolCard(
                isDarkMode = isDarkMode,
                title = "Punto Neutro AI",
                description = stringResource(R.string.Punto_Neutro_AI),
                icon = Icons.Default.SmartToy
            )

            Spacer(Modifier.height(12.dp))

            ToolCard(
                isDarkMode = isDarkMode,
                title = stringResource(R.string.Reverse_Search),
                description = stringResource(R.string.Verify_the_origin_and_authenticity_of_images),
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