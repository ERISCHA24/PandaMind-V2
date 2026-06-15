package com.example.animepopular.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.animepopular.ui.components.*
import com.example.animepopular.ui.theme.*
import com.example.animepopular.viewmodel.HomeUiState
import com.example.animepopular.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToGenre: (String) -> Unit,
    onNavigateToNews: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    language: String = "en"
) {
    val uiState     by viewModel.uiState.collectAsStateWithLifecycle()
    // ✅ Real-time favoriteIds dari Room — tidak perlu reload list
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "PandaMind",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToLanguage) {
                        Icon(Icons.Filled.Language, contentDescription = "Language", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorPrimary)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                QuickActionRow(
                    language = language,
                    onGenre = { onNavigateToGenre("Action") },
                    onNews = onNavigateToNews,
                    onSchedule = onNavigateToSchedule
                )
            }

            when (val state = uiState) {
                is HomeUiState.Loading -> item { LoadingContent(Modifier.height(400.dp)) }

                is HomeUiState.Error -> item {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.loadManga() },
                        modifier = Modifier.height(400.dp)
                    )
                }

                is HomeUiState.Success -> {
                    val list = state.mangaList

                    if (list.isNotEmpty()) {
                        item { SectionHeader(if (language == "id") "✨ Unggulan" else "✨ Highlights") }
                        item {
                            MangaCarousel(
                                mangaList = list.take(5),
                                onDetailClick = onNavigateToDetail
                            )
                        }
                    }

                    item { SectionHeader(if (language == "id") "🔥 Manga Populer" else "🔥 Popular Manga") }

                    items(list, key = { it.id }) { manga ->
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

@Composable
private fun QuickActionRow(
    language: String,
    onGenre: () -> Unit,
    onNews: () -> Unit,
    onSchedule: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickButton(label = if (language == "id") "Genre" else "Genre", onClick = onGenre)
        Spacer(Modifier.width(8.dp))
        QuickButton(label = if (language == "id") "Berita" else "News", onClick = onNews)
        Spacer(Modifier.width(8.dp))
        QuickButton(label = if (language == "id") "Jadwal" else "Schedule", onClick = onSchedule)
    }
}

@Composable
private fun QuickButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        border = BorderStroke(1.dp, AccentColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(label, color = AccentColor, style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MangaCarousel(
    mangaList: List<com.example.animepopular.model.Manga>,
    onDetailClick: (String) -> Unit
) {
    val pagerState = rememberPagerState { mangaList.size }

    LaunchedEffect(pagerState) {
        while (true) {
            delay(3500)
            val next = (pagerState.currentPage + 1) % mangaList.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 12.dp)
        ) { page ->
            CarouselMangaCard(
                manga = mangaList[page],
                onDetailClick = { onDetailClick(mangaList[page].id) }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(mangaList.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (isSelected) 10.dp else 7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) AccentColor else TextSecondary.copy(0.4f))
                )
            }
        }
    }
}