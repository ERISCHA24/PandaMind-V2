package com.example.animepopular.ui.screens.profile

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
    onNavigateToHistory: () -> Unit,   // ✅ NEW — tombol Reading History di ProfileScreen
    onLogout: () -> Unit,              // ✅ NEW — navigasi ke Login setelah logout
    language: String = "en"
) {
    val username      by viewModel.username.collectAsStateWithLifecycle()
    val favoriteCount by viewModel.favoriteCount.collectAsStateWithLifecycle()
    val watchedCount  by viewModel.watchedCount.collectAsStateWithLifecycle()
    val reviewCount   by viewModel.reviewCount.collectAsStateWithLifecycle()

    // Dialog konfirmasi logout
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
                    onLogout()          // ✅ navigasi ke Login setelah logout
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
            // ── Profile Header ─────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = AccentColor.copy(0.15f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = AccentColor,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    username,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "MangaDex Member",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // ── Stats ──────────────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        value = watchedCount.toString(),
                        label = if (language == "id") "Dibaca" else "Read"
                    )
                    Divider(modifier = Modifier.width(1.dp).height(40.dp), color = DividerColor)
                    StatItem(
                        value = favoriteCount.toString(),
                        label = if (language == "id") "Favorit" else "Favorite"
                    )
                    Divider(modifier = Modifier.width(1.dp).height(40.dp), color = DividerColor)
                    StatItem(
                        value = reviewCount.toString(),
                        label = if (language == "id") "Review" else "Reviews"
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Preferensi ────────────────────────────────────────────────────
            SectionLabel(if (language == "id") "PREFERENSI" else "PREFERENCES")
            SettingsGroup {
                SettingsRow(
                    icon    = Icons.Filled.Settings,
                    label   = if (language == "id") "Pengaturan" else "Settings",
                    onClick = onNavigateToSettings
                )
                SettingsDivider()
                SettingsRow(
                    icon    = Icons.Filled.Info,
                    label   = if (language == "id") "Tentang Aplikasi" else "About App",
                    onClick = onNavigateToAbout
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Akun ──────────────────────────────────────────────────────────
            SectionLabel(if (language == "id") "AKUN" else "ACCOUNT")
            SettingsGroup {
                SettingsRow(
                    icon    = Icons.Filled.Notifications,
                    label   = if (language == "id") "Notifikasi" else "Notifications",
                    onClick = {}
                )
                SettingsDivider()
                // ✅ Reading History → navigasi ke HistoryScreen
                SettingsRow(
                    icon    = Icons.Filled.History,
                    label   = if (language == "id") "Riwayat Baca" else "Reading History",
                    onClick = onNavigateToHistory
                )
                SettingsDivider()
                SettingsRow(
                    icon    = Icons.Filled.Security,
                    label   = if (language == "id") "Izin Perangkat" else "Device Permissions",
                    onClick = {}
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Logout ────────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                SettingsRow(
                    icon       = Icons.Filled.Logout,
                    label      = if (language == "id") "Keluar" else "Log Out",
                    labelColor = MaterialTheme.colorScheme.error,
                    onClick    = { showLogoutDialog = true }  // ✅ tampilkan dialog dulu
                )
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = AccentColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color      = AccentColor,
        style      = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier   = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = CardBackground),
        content  = { Column { content() } }
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    labelColor: androidx.compose.ui.graphics.Color = TextPrimary,
    onClick: () -> Unit
) {
    Surface(
        onClick  = onClick,
        color    = CardBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
            Text(
                label,
                color    = labelColor,
                style    = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint     = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    Divider(modifier = Modifier.padding(start = 50.dp), color = DividerColor, thickness = 0.5.dp)
}