package com.example.animepopular.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.animepopular.ui.theme.*
import com.example.animepopular.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onLogout: () -> Unit,
    language: String = "en"
) {
    val username      by viewModel.username.collectAsStateWithLifecycle()
    val favoriteCount by viewModel.favoriteCount.collectAsStateWithLifecycle()
    val watchedCount  by viewModel.watchedCount.collectAsStateWithLifecycle()
    val reviewCount   by viewModel.reviewCount.collectAsStateWithLifecycle()
    val historyCount  by viewModel.historyCount.collectAsStateWithLifecycle()
    val isGuest       by viewModel.isGuest.collectAsStateWithLifecycle()

    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = CardBackground,
            title = {
                Text(
                    if (language == "id") "Keluar dari akun?" else "Sign out?",
                    color = TextPrimary, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (language == "id")
                        "Kamu akan keluar dari akun PandaMind."
                    else
                        "You will be signed out of PandaMind.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                    onLogout()
                }) {
                    Text(
                        if (language == "id") "Keluar" else "Sign Out",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(if (language == "id") "Batal" else "Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(containerColor = BackgroundDark) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Hero Banner ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AccentColor.copy(alpha = 0.30f),
                                ColorPrimary,
                                BackgroundDark
                            )
                        )
                    )
            ) {
                // Decorative circles
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .offset(x = 70.dp, y = (-55).dp)
                        .align(Alignment.TopEnd)
                        .background(AccentColor.copy(alpha = 0.07f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .offset(x = (-18).dp, y = 16.dp)
                        .align(Alignment.TopStart)
                        .background(AccentColor.copy(alpha = 0.05f), CircleShape)
                )

                // Avatar + name anchored to bottom-left
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar ring
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            shape = CircleShape,
                            color = CardBackground,
                            border = BorderStroke(
                                width = 2.5.dp,
                                color = if (isGuest) DividerColor else AccentColor
                            ),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (isGuest) {
                                    Icon(
                                        Icons.Filled.PersonOutline,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(34.dp)
                                    )
                                } else {
                                    Text(
                                        text = username.take(1).uppercase(),
                                        color = AccentColor,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column {
                        Text(
                            text = username,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isGuest)
                                DividerColor.copy(alpha = 0.6f)
                            else
                                AccentColor.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = if (isGuest)
                                    (if (language == "id") "Mode Tamu" else "Guest Mode")
                                else
                                    "MangaDex Member",
                                color = if (isGuest) TextSecondary else AccentColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Stat Cards ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    icon  = Icons.Filled.BookmarkAdded,
                    value = watchedCount.toString(),
                    label = if (language == "id") "Dibaca" else "Read",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon  = Icons.Filled.Favorite,
                    value = favoriteCount.toString(),
                    label = if (language == "id") "Favorit" else "Saved",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon  = Icons.Filled.RateReview,
                    value = reviewCount.toString(),
                    label = if (language == "id") "Ulasan" else "Reviews",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Section: App ───────────────────────────────────────────────────
            SectionLabel(if (language == "id") "APLIKASI" else "APP")
            Spacer(Modifier.height(6.dp))
            MenuGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
                MenuRow(
                    icon     = Icons.Filled.Settings,
                    iconTint = Color(0xFF7C83FD),
                    label    = if (language == "id") "Pengaturan" else "Settings",
                    subtitle = if (language == "id") "Bahasa, tema, tampilan" else "Language, theme, display",
                    onClick  = onNavigateToSettings
                )
                MenuDivider()
                MenuRow(
                    icon     = Icons.Filled.Info,
                    iconTint = Color(0xFF4ECDC4),
                    label    = if (language == "id") "Tentang Aplikasi" else "About App",
                    subtitle = "PandaMind v2.0 · MangaDex API",
                    onClick  = onNavigateToAbout
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Section: Activity ──────────────────────────────────────────────
            SectionLabel(if (language == "id") "AKTIVITAS" else "ACTIVITY")
            Spacer(Modifier.height(6.dp))
            MenuGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
                MenuRow(
                    icon     = Icons.Filled.History,
                    iconTint = RatingColor,
                    label    = if (language == "id") "Riwayat Baca" else "Reading History",
                    subtitle = if (historyCount > 0)
                        (if (language == "id") "$historyCount manga tersimpan" else "$historyCount manga tracked")
                    else
                        (if (language == "id") "Belum ada riwayat" else "Nothing yet"),
                    badge    = if (historyCount > 0) historyCount.toString() else null,
                    onClick  = onNavigateToHistory
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Auth Action ────────────────────────────────────────────────────
            if (isGuest) {
                Button(
                    onClick = onLogout,
                    colors  = ButtonDefaults.buttonColors(containerColor = AccentColor),
                    shape   = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(52.dp)
                ) {
                    Icon(Icons.Filled.Login, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (language == "id") "Masuk ke Akun" else "Sign In to Account",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    border  = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                    shape   = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(52.dp)
                ) {
                    Icon(
                        Icons.Filled.Logout,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (language == "id") "Keluar dari Akun" else "Log Out",
                        color      = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp
                    )
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = AccentColor.copy(alpha = 0.13f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint     = AccentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text       = value,
                color      = TextPrimary,
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = label,
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        color         = TextSecondary,
        style         = MaterialTheme.typography.labelSmall,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier      = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
    )
}

@Composable
private fun MenuGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    subtitle: String?   = null,
    badge: String?      = null,
    iconTint: Color     = AccentColor,
    onClick: () -> Unit
) {
    Surface(
        onClick  = onClick,
        color    = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon pill
            Surface(
                shape    = RoundedCornerShape(10.dp),
                color    = iconTint.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint     = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text       = label,
                    color      = TextPrimary,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = subtitle,
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AccentColor
                ) {
                    Text(
                        text       = badge,
                        color      = Color.White,
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint     = TextSecondary.copy(alpha = 0.45f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(start = 70.dp, end = 16.dp),
        color     = DividerColor,
        thickness = 0.5.dp
    )
}