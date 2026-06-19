package com.example.animepopular.ui.screens.toprated

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.animepopular.ui.components.*
import com.example.animepopular.ui.theme.*
import com.example.animepopular.viewmodel.TopRatedUiState
import com.example.animepopular.viewmodel.TopRatedViewModel

@Composable
fun TopRatedScreen(
    viewModel: TopRatedViewModel,
    onNavigateToDetail: (String) -> Unit,
    language: String = "en"
) {
    val uiState     by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // ✅ Snackbar saat manga ditambahkan ke favorit
    LaunchedEffect(Unit) {
        viewModel.favoriteAddedEvent.collect { title ->
            val message = if (language == "id")
                "\"$title\" ditambahkan ke Favorit ❤️"
            else
                "\"$title\" added to Favorites ❤️"
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    if (language == "id") "🏆 Peringkat Teratas" else "🏆 Top Rated",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is TopRatedUiState.Loading -> LoadingContent(Modifier.padding(padding))
            is TopRatedUiState.Error -> ErrorContent(
                message = state.message,
                onRetry = { viewModel.loadTopRated() },
                modifier = Modifier.padding(padding)
            )
            is TopRatedUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    itemsIndexed(state.list, key = { _, it -> it.id }) { index, manga ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, top = if (index == 0) 8.dp else 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (index) {
                                    0    -> RatingColor
                                    1    -> TextSecondary.copy(0.8f)
                                    2    -> AccentColor.copy(0.7f)
                                    else -> SurfaceColor
                                },
                                modifier = Modifier.size(width = 32.dp, height = 32.dp)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "#${index + 1}",
                                        color = if (index < 3) ColorPrimary else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            MangaCard(
                                manga = manga,
                                onDetailClick = { onNavigateToDetail(manga.id) },
                                onFavoriteToggle = { viewModel.toggleFavorite(manga) },
                                modifier = Modifier.weight(1f),
                                isFavoriteOverride = manga.id in favoriteIds
                            )
                        }
                    }
                }
            }
        }
    }
}