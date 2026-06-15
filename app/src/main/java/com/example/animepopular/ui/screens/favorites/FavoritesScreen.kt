package com.example.animepopular.ui.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.animepopular.ui.components.GuestGateScreen
import com.example.animepopular.ui.components.MangaCard
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
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(favorites, key = { it.id }) { manga ->
                MangaCard(
                    manga = manga,
                    onDetailClick = { onNavigateToDetail(manga.id) },
                    onFavoriteToggle = { viewModel.removeFavorite(manga) },
                    isFavoriteOverride = true
                )
            }
        }
    }
}