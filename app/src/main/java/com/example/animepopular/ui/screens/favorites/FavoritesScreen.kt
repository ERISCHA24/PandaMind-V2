package com.example.animepopular.ui.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.animepopular.model.Manga
import com.example.animepopular.ui.components.GuestGateScreen
import com.example.animepopular.ui.theme.*
import com.example.animepopular.viewmodel.FavoritesViewModel

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    language: String = "en"
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val isGuest   by viewModel.isGuest.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    if (language == "id") "❤️ Favorit Saya" else "❤️ My Favorites",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = TextPrimary
                )
                if (!isGuest) {
                    Text(
                        "${favorites.size} manga",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    ) { padding ->

        if (isGuest) {
            GuestGateScreen(
                feature = if (language == "id") "Favorit" else "Favorites",
                description = if (language == "id")
                    "Login untuk menyimpan manga favoritmu dan mengaksesnya kapan saja."
                else
                    "Sign in to save your favorite manga and access them anytime.",
                emoji = "❤️",
                language = language,
                onNavigateToLogin = onNavigateToLogin,
                showSignInButton = false,   // ✅ NEW — tombol disembunyikan
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        if (favorites.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💔", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (language == "id") "Belum ada manga favorit" else "No favorites yet",
                        color = TextSecondary, style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (language == "id") "Tekan ❤️ pada manga manapun untuk menambahkan"
                        else "Tap ❤️ on any manga to add it here",
                        color = TextSecondary.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = 12.dp, end = 12.dp,
                top = 8.dp, bottom = 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(favorites, key = { it.id }) { manga ->
                FavoriteCard(
                    manga = manga,
                    language = language,
                    onDetailClick = { onNavigateToDetail(manga.id) },
                    onRemoveFavorite = { viewModel.removeFavorite(manga) }
                )
            }
        }
    }
}

// ── Favorite Card ─────────────────────────────────────────────────────────────
// Menampilkan cover, judul, deskripsi, rating, status, dan tombol detail + hapus

@Composable
private fun FavoriteCard(
    manga: Manga,
    language: String,
    onDetailClick: () -> Unit,
    onRemoveFavorite: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // ── Cover Image ───────────────────────────────────────────────────
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(manga.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(90.dp)
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(Modifier.width(12.dp))

            // ── Info ──────────────────────────────────────────────────────────
            Column(Modifier.weight(1f)) {

                // Judul
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                // Rating + Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "★ ${"%.1f".format(manga.rating)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = RatingColor,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = manga.status.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // ✅ Deskripsi manga — diambil dari Room (sudah disimpan dari API saat toggle)
                val desc = manga.description.trim()
                if (desc.isNotBlank()) {
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                } else {
                    Text(
                        text = if (language == "id") "Tidak ada sinopsis." else "No description available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = 0.5f)
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Tombol aksi
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tombol Detail
                    Button(
                        onClick = onDetailClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            if (language == "id") "Detail" else "Detail",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Tombol hapus dari favorit
                    OutlinedButton(
                        onClick = onRemoveFavorite,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = "Remove",
                            tint = AccentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (language == "id") "Hapus" else "Remove",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentColor
                        )
                    }
                }
            }
        }
    }
}