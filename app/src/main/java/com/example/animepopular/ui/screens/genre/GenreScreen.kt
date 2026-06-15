package com.example.animepopular.ui.screens.genre

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.animepopular.ui.components.*
import com.example.animepopular.ui.theme.*
import com.example.animepopular.viewmodel.GenreUiState
import com.example.animepopular.viewmodel.GenreViewModel

private val QUICK_GENRES = listOf(
    "Action", "Adventure", "Comedy", "Drama", "Fantasy",
    "Horror", "Mystery", "Romance", "Sci-Fi", "Slice of Life",
    "Sports", "Supernatural", "Thriller", "Mecha", "Isekai"
)

@Composable
fun GenreScreen(
    viewModel: GenreViewModel,
    initialGenre: String = "Action",
    onNavigateToDetail: (String) -> Unit,
    language: String = "en"
) {
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val tags          by viewModel.tags.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    // ✅ Real-time favorite IDs
    val favoriteIds   by viewModel.favoriteIds.collectAsStateWithLifecycle()

    val displayGenres = if (tags.isNotEmpty()) tags.map { it.name }.take(15) else QUICK_GENRES

    LaunchedEffect(initialGenre) { viewModel.loadByGenre(initialGenre) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorPrimary)
                    .padding(12.dp)
            ) {
                Text(
                    "Genre: $selectedGenre",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                when (val s = uiState) {
                    is GenreUiState.Success ->
                        Text(
                            "${s.list.size} manga", color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )
                    else -> Spacer(Modifier.height(8.dp))
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    displayGenres.forEach { genre ->
                        val selected = genre == selectedGenre
                        OutlinedButton(
                            onClick = { viewModel.loadByGenre(genre) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, if (selected) AccentColor else DividerColor),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) AccentColor.copy(0.15f) else ColorPrimary
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                genre,
                                color = if (selected) AccentColor else TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is GenreUiState.Loading -> LoadingContent(Modifier.padding(padding))
            is GenreUiState.Error -> ErrorContent(
                message = state.message,
                onRetry = { viewModel.loadByGenre(selectedGenre) },
                modifier = Modifier.padding(padding)
            )
            is GenreUiState.Success -> {
                if (state.list.isEmpty()) {
                    EmptyContent(
                        message = if (language == "id") "Tidak ada manga ditemukan" else "No manga found for this genre",
                        emoji = "🗂️",
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(state.list, key = { it.id }) { manga ->
                            MangaCard(
                                manga = manga,
                                onDetailClick = { onNavigateToDetail(manga.id) },
                                onFavoriteToggle = { viewModel.toggleFavorite(manga) },
                                isFavoriteOverride = manga.id in favoriteIds  // ✅
                            )
                        }
                    }
                }
            }
        }
    }
}