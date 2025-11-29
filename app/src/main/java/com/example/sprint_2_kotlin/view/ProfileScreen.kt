package com.example.sprint_2_kotlin.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sprint_2_kotlin.viewmodel.ReadHistoryViewModel
import com.example.sprint_2_kotlin.viewmodel.BookmarkViewModel
import com.example.sprint_2_kotlin.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    isDarkMode: Boolean = false,
    onToggleDarkMode: (Boolean) -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToGuide: () -> Unit = {},
    onNavigateToReadHistory: () -> Unit = {},
    onNavigateToNewsDetail: (Int) -> Unit = {},
    onNavigateToBookmarks: () -> Unit = {},
    readHistoryViewModel: ReadHistoryViewModel = viewModel(),
    bookmarkViewModel: BookmarkViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel() // Add AuthViewModel
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Activity", "Achievements")
    val coroutineScope = rememberCoroutineScope()

    // Admin panel states
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showAdminPanel by remember { mutableStateOf(false) }
    var tapCount by remember { mutableStateOf(0) }

    // Edit Profile dialog state
    var showEditProfileDialog by remember { mutableStateOf(false) }

    // Logout confirmation dialog
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    // Contadores
    val readCount by readHistoryViewModel.readCount.collectAsState()
    val bookmarkCount by bookmarkViewModel.bookmarkCount.collectAsState()

    // Colores dinámicos según el tema
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                tapCount++
                                if (tapCount >= 3) {
                                    showPasswordDialog = true
                                    tapCount = 0
                                }
                            },
                            onLongClick = {
                                showPasswordDialog = true
                                tapCount = 0
                            }
                        )
                    ) {
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

                    IconButton(
                        onClick = { showLogoutConfirmation = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = Color(0xFFE53935)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor
                )
            )
        },
        bottomBar = {
            ProfileBottomNavigationBar(
                isDarkMode = isDarkMode,
                onNavigateToHome = onNavigateToHome,
                onNavigateToGuide = onNavigateToGuide
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
                UserProfileCard(
                    isDarkMode = isDarkMode,
                    onAdminClick = { showPasswordDialog = true },
                    onEditProfileClick = { showEditProfileDialog = true }
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                StatisticsGrid(
                    isDarkMode = isDarkMode,
                    readCount = readCount,
                    bookmarkCount = bookmarkCount,
                    onReadHistoryClick = onNavigateToReadHistory,
                    onBookmarksClick = onNavigateToBookmarks
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = surfaceColor,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selectedTab == index) textColor else secondaryTextColor
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            when (selectedTab) {
                0 -> {
                    // Activity Tab
                    item {
                        Text(
                            text = "Recent Activity",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    items(getRecentActivities()) { activity ->
                        ActivityItem(activity = activity, isDarkMode = isDarkMode)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                1 -> {
                    // Achievements Tab
                    item {
                        Text(
                            text = "Achievements",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Coming soon...",
                            fontSize = 14.sp,
                            color = secondaryTextColor
                        )
                    }
                }
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            containerColor = surfaceColor,
            icon = {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Logout",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Confirm Logout",
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to log out? You'll need to sign in again next time.",
                    color = secondaryTextColor
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            // Clear session using AuthViewModel
                            authViewModel.logout()
                            showLogoutConfirmation = false
                            // Navigate to auth screen
                            onLogout()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text("Logout", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutConfirmation = false }
                ) {
                    Text("Cancel", color = textColor)
                }
            }
        )
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            isDarkMode = isDarkMode,
            onToggleDarkMode = onToggleDarkMode,
            onDismiss = { showEditProfileDialog = false }
        )
    }

    // Admin Password Dialog
    if (showPasswordDialog) {
        AdminPasswordDialog(
            isDarkMode = isDarkMode,
            onDismiss = { showPasswordDialog = false },
            onPasswordCorrect = {
                showPasswordDialog = false
                showAdminPanel = true
            }
        )
    }

    // Admin Analytics Panel
    if (showAdminPanel) {
        AdminAnalyticsDialog(
            isDarkMode = isDarkMode,
            authViewModel = authViewModel,
            onDismiss = { showAdminPanel = false },
            onLogout = {
                coroutineScope.launch {
                    authViewModel.logout()
                    showAdminPanel = false
                    onLogout()
                }
            }
        )
    }
}

// ============================================
// EDIT PROFILE DIALOG (con Dark Mode toggle)
// ============================================
@Composable
fun EditProfileDialog(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = textColor,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Settings",
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        },
        text = {
            Column {
                // Dark Mode Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Theme",
                            tint = if (isDarkMode) Color(0xFFFFD54F) else Color(0xFFFFA726),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Dark Mode",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                            Text(
                                text = if (isDarkMode) "Enabled" else "Disabled",
                                fontSize = 12.sp,
                                color = secondaryTextColor
                            )
                        }
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onToggleDarkMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF9C27B0),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFB0B0B0)
                        )
                    )
                }

                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = if (isDarkMode) Color(0xFF333333) else Color(0xFFE0E0E0)
                )

                // Placeholder para futuras opciones
                Text(
                    text = "More settings coming soon...",
                    fontSize = 14.sp,
                    color = secondaryTextColor,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkMode) Color(0xFF9C27B0) else Color(0xFF1A1A1A)
                )
            ) {
                Text("Done", color = Color.White)
            }
        }
    )
}

@Composable
fun UserProfileCard(
    isDarkMode: Boolean = false,
    onAdminClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {}
) {
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
    val avatarBgColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = avatarBgColor,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        modifier = Modifier.size(40.dp),
                        tint = secondaryTextColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Anonymous User",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Active session",
                fontSize = 13.sp,
                color = secondaryTextColor
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge(
                    icon = Icons.Default.Verified,
                    text = "Trusted Verifier",
                    backgroundColor = Color(0xFFE3F2FD),
                    textColor = Color(0xFF1976D2)
                )
                Badge(
                    icon = Icons.Default.Star,
                    text = "Level 3",
                    backgroundColor = Color(0xFF1A1A1A),
                    textColor = Color.White
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onEditProfileClick,
                modifier = Modifier.fillMaxWidth(0.7f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = textColor
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Edit profile", fontSize = 14.sp)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onAdminClick,
                modifier = Modifier.fillMaxWidth(0.7f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9C27B0)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = "Admin",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("🧪 Admin Analytics", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun Badge(
    icon: ImageVector,
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatisticsGrid(
    isDarkMode: Boolean = false,
    readCount: Int = 0,
    bookmarkCount: Int = 0,
    onReadHistoryClick: () -> Unit = {},
    onBookmarksClick: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = Icons.Default.Visibility,
                value = "$readCount",
                label = "Articles read",
                iconColor = Color(0xFF2196F3),
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f),
                onClick = onReadHistoryClick
            )
            StatCard(
                icon = Icons.Default.Flag,
                value = "12",
                label = "Reports submitted",
                iconColor = Color(0xFFE53935),
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = Icons.Default.Bookmark,
                value = "$bookmarkCount",
                label = "Bookmarks",
                iconColor = Color(0xFFFFA726),
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f),
                onClick = onBookmarksClick
            )
            StatCard(
                icon = Icons.Default.TrendingUp,
                value = "28",
                label = "Day streak",
                iconColor = Color(0xFF9C27B0),
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color,
    isDarkMode: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { onClick?.invoke() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = secondaryTextColor
            )
        }
    }
}

data class ActivityData(
    val icon: ImageVector,
    val iconColor: Color,
    val title: String,
    val time: String
)

fun getRecentActivities(): List<ActivityData> {
    return listOf(
        ActivityData(
            icon = Icons.Default.Science,
            iconColor = Color(0xFF2196F3),
            title = "Advances in automatic verification technology",
            time = "2 hours ago"
        ),
        ActivityData(
            icon = Icons.Default.Flag,
            iconColor = Color(0xFFE53935),
            title = "Reported fake news about vaccines",
            time = "5 hours ago"
        ),
        ActivityData(
            icon = Icons.Default.Bookmark,
            iconColor = Color(0xFFFFA726),
            title = "Saved article about digital security",
            time = "Yesterday"
        )
    )
}

@Composable
fun ActivityItem(activity: ActivityData, isDarkMode: Boolean = false) {
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)
    val tertiaryTextColor = if (isDarkMode) Color(0xFF808080) else Color(0xFF888888)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = activity.iconColor.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = activity.icon,
                        contentDescription = null,
                        tint = activity.iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = activity.time,
                    fontSize = 12.sp,
                    color = tertiaryTextColor
                )
            }
        }
    }
}

@Composable
fun ProfileBottomNavigationBar(
    isDarkMode: Boolean = false,
    onNavigateToHome: () -> Unit = {},
    onNavigateToGuide: () -> Unit = {}
) {
    val containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White

    NavigationBar(
        containerColor = containerColor,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 12.sp) },
            selected = false,
            onClick = onNavigateToHome
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.MenuBook, contentDescription = "Guide") },
            label = { Text("Guide", fontSize = 12.sp) },
            selected = false,
            onClick = onNavigateToGuide
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile", fontSize = 12.sp) },
            selected = true,
            onClick = { /* Already on Profile */ }
        )
    }
}

// ============================================
// ADMIN PASSWORD DIALOG
// ============================================
@Composable
fun AdminPasswordDialog(
    isDarkMode: Boolean = false,
    onDismiss: () -> Unit,
    onPasswordCorrect: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val correctPassword = "admin123"

    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock",
                tint = textColor,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Admin Access Required",
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        },
        text = {
            Column {
                Text(
                    "Enter admin password to view analytics:",
                    color = textColor
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        showError = false
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    isError = showError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Incorrect password. Try 'admin123'",
                        color = Color(0xFFE53935),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password == correctPassword) {
                        onPasswordCorrect()
                    } else {
                        showError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkMode) Color(0xFF9C27B0) else Color(0xFF1A1A1A)
                )
            ) {
                Text("Access")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textColor)
            }
        }
    )
}

// ============================================
// ADMIN ANALYTICS DIALOG (placeholder)
// ============================================
@Composable
fun AdminAnalyticsDialog(
    isDarkMode: Boolean = false,
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color(0xFFE1E1E1) else Color(0xFF1A1A1A)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        icon = {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = "Admin",
                tint = Color(0xFF9C27B0),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Admin Analytics",
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        },
        text = {
            Text(
                "Analytics dashboard coming soon...",
                color = textColor
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkMode) Color(0xFF9C27B0) else Color(0xFF1A1A1A)
                )
            ) {
                Text("Close")
            }
        }
    )
}