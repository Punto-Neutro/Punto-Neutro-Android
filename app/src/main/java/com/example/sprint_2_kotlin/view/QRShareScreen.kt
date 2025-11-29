package com.example.sprint_2_kotlin.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRShareScreen(
    isDarkMode: Boolean = false,
    newsItemUrl: String,
    newsItemTitle: String,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCopiedMessage by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cachedUrl by remember { mutableStateOf(newsItemUrl) }

    // Colores dinámicos según el tema
    val backgroundColor = if (isDarkMode) androidx.compose.ui.graphics.Color(0xFF121212) else androidx.compose.ui.graphics.Color(0xFFF5F5F5)
    val surfaceColor = if (isDarkMode) androidx.compose.ui.graphics.Color(0xFF1E1E1E) else androidx.compose.ui.graphics.Color.White
    val textColor = if (isDarkMode) androidx.compose.ui.graphics.Color(0xFFE1E1E1) else androidx.compose.ui.graphics.Color(0xFF1A1A1A)
    val secondaryTextColor = if (isDarkMode) androidx.compose.ui.graphics.Color(0xFFB0B0B0) else androidx.compose.ui.graphics.Color(0xFF666666)
    val buttonColor = if (isDarkMode) androidx.compose.ui.graphics.Color(0xFF9C27B0) else androidx.compose.ui.graphics.Color(0xFF1976D2)

    // Load cached QR code and URL on launch
    LaunchedEffect(newsItemUrl) {
        scope.launch {
            // Try to load from cache first
            val cached = loadCachedQRData(context, newsItemUrl)
            if (cached != null) {
                qrBitmap = cached.first
                cachedUrl = cached.second
            } else {
                // Generate new QR code and cache it
                val newBitmap = withContext(Dispatchers.Default) {
                    generateQRCode(newsItemUrl)
                }
                qrBitmap = newBitmap

                // Cache the QR code and URL
                newBitmap?.let {
                    cacheQRData(context, newsItemUrl, it, newsItemUrl)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Share News",
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "Scan to view article",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // News title
            Text(
                text = newsItemTitle,
                fontSize = 16.sp,
                color = secondaryTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // QR Code
            Card(
                modifier = Modifier.size(280.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    qrBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                    } ?: CircularProgressIndicator()
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // URL display
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = surfaceColor
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Article URL:",
                        fontSize = 12.sp,
                        color = secondaryTextColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = cachedUrl,
                        fontSize = 14.sp,
                        color = textColor,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Copy to clipboard button
            Button(
                onClick = {
                    copyToClipboard(context, cachedUrl)
                    showCopiedMessage = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Copy URL to Clipboard",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Copied message
            if (showCopiedMessage) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showCopiedMessage = false
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "✓ URL copied to clipboard!",
                    color = if (isDarkMode) androidx.compose.ui.graphics.Color(0xFF4CAF50) else androidx.compose.ui.graphics.Color(0xFF2E7D32),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun generateQRCode(url: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("News URL", text)
    clipboard.setPrimaryClip(clip)
}

// Cache management functions
private fun getCacheDir(context: Context): File {
    val cacheDir = File(context.cacheDir, "qr_codes")
    if (!cacheDir.exists()) {
        cacheDir.mkdirs()
    }
    return cacheDir
}

private fun getUrlHash(url: String): String {
    val digest = MessageDigest.getInstance("MD5")
    val hash = digest.digest(url.toByteArray())
    return hash.joinToString("") { "%02x".format(it) }
}

private suspend fun cacheQRData(
    context: Context,
    url: String,
    bitmap: Bitmap,
    originalUrl: String
) = withContext(Dispatchers.IO) {
    try {
        val cacheDir = getCacheDir(context)
        val urlHash = getUrlHash(url)

        // Save bitmap
        val bitmapFile = File(cacheDir, "${urlHash}.png")
        FileOutputStream(bitmapFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // Save URL
        val urlFile = File(cacheDir, "${urlHash}.txt")
        urlFile.writeText(originalUrl)

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private suspend fun loadCachedQRData(
    context: Context,
    url: String
): Pair<Bitmap, String>? = withContext(Dispatchers.IO) {
    try {
        val cacheDir = getCacheDir(context)
        val urlHash = getUrlHash(url)

        val bitmapFile = File(cacheDir, "${urlHash}.png")
        val urlFile = File(cacheDir, "${urlHash}.txt")

        if (bitmapFile.exists() && urlFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(bitmapFile.absolutePath)
            val cachedUrl = urlFile.readText()

            if (bitmap != null && cachedUrl.isNotEmpty()) {
                return@withContext Pair(bitmap, cachedUrl)
            }
        }
        null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}