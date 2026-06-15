package com.example.animepopular.ui.screens.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animepopular.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    language: String = "en"
) {
    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (language == "id") "ℹ️ Tentang Aplikasi" else "ℹ️ About App",
                        color = TextPrimary, fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Text("🎌", fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                if (language == "id") "Anime Populer" else "Popular Anime",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Version 2.0",
                color = AccentColor,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )
            Text(
                if (language == "id")
                    "Anime Populer adalah aplikasi untuk menjelajahi manga-manga terpopuler dari MangaDex. Nikmati fitur pencarian, favorit, jadwal, dan upload review gambar!"
                else
                    "Popular Anime is an app for exploring the most popular manga from MangaDex. Enjoy search, favorites, schedule, and image review upload features!",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Divider(color = DividerColor)
            Spacer(Modifier.height(20.dp))
            InfoRow(label = if (language == "id") "Dikembangkan oleh" else "Developed by",
                value = "ERISCHA MARSELA\n2410817120022")
            Spacer(Modifier.height(12.dp))
            InfoRow(label = if (language == "id") "Kontak" else "Contact",
                value = "@ris._.sela")
            Spacer(Modifier.height(12.dp))
            InfoRow(label = "API", value = "MangaDex API v5")
            Spacer(Modifier.height(12.dp))
            InfoRow(label = if (language == "id") "Universitas" else "University",
                value = "Universitas Lambung Mangkurat (ULM)")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text(value, color = TextPrimary, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
        }
    }
}