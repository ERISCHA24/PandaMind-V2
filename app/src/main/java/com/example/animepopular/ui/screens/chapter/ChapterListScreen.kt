package com.example.animepopular.ui.screens.chapter

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.animepopular.model.Chapter
import com.example.animepopular.model.ReadingProgress
import com.example.animepopular.ui.components.EmptyContent
import com.example.animepopular.ui.components.ErrorContent
import com.example.animepopular.ui.components.LoadingContent
import com.example.animepopular.ui.theme.*
import com.example.animepopular.viewmodel.ChapterListUiState
import com.example.animepopular.viewmodel.ChapterListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListScreen(
    viewModel: ChapterListViewModel,
    mangaTitle: String,
    onNavigateToReader: (chapterId: String, chapterTitle: String) -> Unit,
    onBack: () -> Unit,
    language: String = "en"
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedLang by viewModel.selectedLang.collectAsStateWithLifecycle()
    val progressList by viewModel.mangaProgress.collectAsStateWithLifecycle()
    val progressMap  = remember(progressList) { progressList.associateBy { it.chapterId } }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (language == "id") "Daftar Chapter" else "Chapter List",
                            color = TextPrimary, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            mangaTitle,
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // Language filter
                    val langs = listOf("en", "id", "ja")
                    langs.forEach { lang ->
                        FilterChipSmall(
                            label = lang.uppercase(),
                            selected = selectedLang == lang,
                            onClick = { viewModel.setLanguage(lang) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorPrimary)
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ChapterListUiState.Loading -> LoadingContent(Modifier.padding(padding))

            is ChapterListUiState.Error  -> ErrorContent(
                message = state.message,
                onRetry = { viewModel.loadChapters(reset = true) },
                modifier = Modifier.padding(padding)
            )

            is ChapterListUiState.Success -> {
                if (state.chapters.isEmpty()) {
                    EmptyContent(
                        message = if (language == "id") "Tidak ada chapter tersedia" else "No chapters available",
                        emoji = "📖",
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            Text(
                                "${state.chapters.size} ${if (language == "id") "chapter" else "chapters"}",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        items(state.chapters, key = { it.id }) { chapter ->
                            val progress = progressMap[chapter.id]
                            ChapterItem(
                                chapter = chapter,
                                progress = progress,
                                language = language,
                                onClick = {
                                    val label = buildChapterLabel(chapter)
                                    onNavigateToReader(chapter.id, label)
                                }
                            )
                        }

                        if (state.hasMore) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.loadMore() },
                                        border = BorderStroke(1.dp, AccentColor)
                                    ) {
                                        Text(
                                            if (language == "id") "Muat lebih banyak" else "Load more",
                                            color = AccentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterItem(
    chapter: Chapter,
    progress: ReadingProgress?,
    language: String,
    onClick: () -> Unit
) {
    val isRead     = progress != null && progress.currentPage >= progress.totalPages - 1
    val inProgress = progress != null && !isRead && progress.currentPage > 0
    val progressFraction = if (progress != null && progress.totalPages > 0) {
        progress.currentPage.toFloat() / progress.totalPages.toFloat()
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isRead     -> CardBackground.copy(alpha = 0.6f)
                inProgress -> CardBackground
                else       -> CardBackground
            }
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status icon
                Icon(
                    imageVector = when {
                        isRead     -> Icons.Filled.CheckCircle
                        inProgress -> Icons.Filled.PlayCircle
                        else       -> Icons.Filled.MenuBook
                    },
                    contentDescription = null,
                    tint = when {
                        isRead     -> AccentColor
                        inProgress -> RatingColor
                        else       -> TextSecondary.copy(0.5f)
                    },
                    modifier = Modifier.size(22.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = buildChapterLabel(chapter),
                        color = if (isRead) TextSecondary else TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isRead) FontWeight.Normal else FontWeight.SemiBold
                    )
                    chapter.title?.takeIf { it.isNotBlank() }?.let { title ->
                        Text(
                            text = title,
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (chapter.pages > 0) {
                            Text(
                                "${chapter.pages} ${if (language == "id") "hal." else "pages"}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            chapter.publishedAt,
                            color = TextSecondary.copy(0.7f),
                            fontSize = 11.sp
                        )
                        if (inProgress && progress != null) {
                            Text(
                                "p.${progress.currentPage + 1}/${progress.totalPages}",
                                color = RatingColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Progress bar for in-progress chapters
            if (inProgress && progressFraction > 0f) {
                LinearProgressIndicator(
                    progress     = progressFraction,
                    modifier     = Modifier.fillMaxWidth(),
                    color        = RatingColor,
                    trackColor   = DividerColor
                )
            }
        }
    }
}

@Composable
private fun FilterChipSmall(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) AccentColor else DividerColor,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Text(
            label,
            color = if (selected) TextPrimary else TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun buildChapterLabel(chapter: Chapter): String {
    val vol = chapter.volume?.let { "Vol.$it " } ?: ""
    val ch  = chapter.chapter?.let { "Ch.$it" } ?: "Oneshot"
    return "$vol$ch"
}