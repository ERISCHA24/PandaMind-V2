package com.example.animepopular.ui.screens.history

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.animepopular.model.ReadingHistory
import com.example.animepopular.ui.components.GuestGateScreen
import com.example.animepopular.ui.theme.*
import com.example.animepopular.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onBackClick: () -> Unit,   // ← NEW: back button callback
    language: String = "en"
) {
    val isGuest by viewModel.isGuest.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    // Dialog konfirmasi clear all
    var showClearDialog by remember { mutableStateOf(false) }
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = CardBackground,
            title = {
                Text(
                    if (language == "id") "Hapus Semua Riwayat?" else "Clear All History?",
                    color = TextPrimary, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (language == "id") "Semua riwayat bacaan akan dihapus permanen."
                    else "All reading history will be permanently deleted.",
                    color = TextSecondary, style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllHistory()
                    showClearDialog = false
                }) {
                    Text(
                        if (language == "id") "Hapus" else "Clear",
                        color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(if (language == "id") "Batal" else "Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                navigationIcon = {              // ← NEW: back button
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (language == "id") "Kembali" else "Back",
                            tint = TextPrimary
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            if (language == "id") "📖 Riwayat Bacaan" else "📖 Reading History",
                            color = TextPrimary, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (!isGuest) {
                            Text(
                                "${history.size} ${if (language == "id") "manga" else "manga"}",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                actions = {
                    if (!isGuest && history.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                Icons.Filled.DeleteSweep,
                                contentDescription = "Clear history",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorPrimary)
            )
        }
    ) { padding ->
        // ── GUEST GATE ────────────────────────────────────────────────────────
        if (isGuest) {
            GuestGateScreen(
                feature = if (language == "id") "Riwayat Bacaan" else "Reading History",
                description = if (language == "id")
                    "Login untuk menyimpan riwayat bacaanmu dan melanjutkan dari halaman terakhir di perangkat manapun."
                else
                    "Sign in to save your reading history and continue from the last page on any device.",
                emoji = "📖",
                language = language,
                onNavigateToLogin = onNavigateToLogin,
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        // ── EMPTY ─────────────────────────────────────────────────────────────
        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📖", fontSize = 64.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (language == "id") "Belum ada riwayat bacaan" else "No reading history yet",
                        color = TextPrimary, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (language == "id") "Mulai baca manga dan riwayat akan muncul di sini"
                        else "Start reading manga and your history will appear here",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            return@Scaffold
        }

        // ── LIST ──────────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history, key = { it.mangaId }) { entry ->
                HistoryCard(
                    entry = entry,
                    language = language,
                    onDetailClick = { onNavigateToDetail(entry.mangaId) },
                    onRemove = { viewModel.removeFromHistory(entry.mangaId) }
                )
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun HistoryCard(
    entry: ReadingHistory,
    language: String,
    onDetailClick: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Cover ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(100.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(entry.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = entry.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient overlay bawah
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(0.6f)),
                                startY = 50f
                            )
                        )
                )
            }

            Spacer(Modifier.width(12.dp))

            // ── Info ────────────────────────────────────────────────────────────
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    color = TextPrimary, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                if (entry.lastChapterTitle.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.BookmarkAdded, contentDescription = null,
                            tint = AccentColor, modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = entry.lastChapterTitle,
                            color = AccentColor,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }

                // Total chapters read
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.MenuBook, contentDescription = null,
                        tint = TextSecondary, modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${entry.totalChaptersRead} ${if (language == "id") "chapter dibaca" else "chapters read"}",
                        color = TextSecondary, style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = sdf.format(Date(entry.lastReadAt)),
                    color = TextSecondary.copy(0.7f),
                    fontSize = 10.sp
                )

                Spacer(Modifier.height(8.dp))

                // Tombol detail
                OutlinedButton(
                    onClick = onDetailClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, AccentColor)
                ) {
                    Text(
                        if (language == "id") "Buka Detail" else "Open Detail",
                        style = MaterialTheme.typography.labelSmall, color = AccentColor
                    )
                }
            }

            // ── Hapus button ──────────────────────────────────────────────────
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Close, contentDescription = "Remove",
                    tint = TextSecondary, modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}