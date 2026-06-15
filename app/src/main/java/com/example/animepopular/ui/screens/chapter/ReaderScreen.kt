package com.example.animepopular.ui.screens.chapter

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.animepopular.ui.theme.*
import com.example.animepopular.viewmodel.ReaderUiState
import com.example.animepopular.viewmodel.ReaderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    chapterId: String,
    mangaId: String,
    chapterTitle: String,
    onBack: () -> Unit,
    language: String = "en"
) {
    val context     = LocalContext.current
    val pagesState  by viewModel.pagesState.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val dataSaver   by viewModel.dataSaver.collectAsStateWithLifecycle()
    val scope       = rememberCoroutineScope()
    var showUi      by remember { mutableStateOf(true) }

    // Load chapter saat pertama masuk
    LaunchedEffect(chapterId) {
        viewModel.loadChapter(chapterId, mangaId)
    }

    // Simpan progress saat keluar dari screen
    DisposableEffect(chapterId) {
        onDispose {
            viewModel.saveProgressNow()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val state = pagesState) {
            is ReaderUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentColor)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (language == "id") "Memuat halaman..." else "Loading pages...",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            is ReaderUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.WifiOff,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(state.message, color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp))
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadChapter(chapterId, mangaId) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                        ) {
                            Text(if (language == "id") "Coba Lagi" else "Retry")
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onBack) {
                            Text(if (language == "id") "Kembali" else "Go Back",
                                color = TextSecondary)
                        }
                    }
                }
            }

            is ReaderUiState.Success -> {
                val pages      = state.pages
                val totalPages = pages.totalPages

                // PagerState — initialPage dari ViewModel (progress tersimpan)
                val safeInitPage = currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                val pagerState   = rememberPagerState(initialPage = safeInitPage) { totalPages }

                // Sinkronisasi: pager → viewmodel (user swipe)
                LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                    if (!pagerState.isScrollInProgress &&
                        pagerState.currentPage != currentPage) {
                        viewModel.goToPage(pagerState.currentPage)
                    }
                }

                // Sinkronisasi: viewmodel → pager (tombol prev/next atau slider)
                LaunchedEffect(currentPage) {
                    if (!pagerState.isScrollInProgress &&
                        pagerState.currentPage != currentPage) {
                        pagerState.scrollToPage(currentPage)   // scrollToPage, BUKAN animateScrollToPage
                    }
                }

                // ── Halaman Manga (HorizontalPager) ──────────────────────────
                HorizontalPager(
                    state    = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { showUi = !showUi })
                        },
                    // Non-aktifkan fling jarak jauh agar tidak skip banyak halaman
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = pagerState,
                        snapPositionalThreshold = 0.3f
                    ),
                    beyondBoundsPageCount = 1   // pre-load 1 halaman di kiri/kanan
                ) { pageIndex ->
                    val pageUrl = pages.getPageUrl(pageIndex, dataSaver)
                    MangaPageImage(
                        url         = pageUrl,
                        pageNumber  = pageIndex + 1,
                        totalPages  = totalPages,
                        language    = language
                    )
                }

                // ── Top Overlay ───────────────────────────────────────────────
                AnimatedVisibility(
                    visible = showUi,
                    enter   = fadeIn() + slideInVertically { -it },
                    exit    = fadeOut() + slideOutVertically { -it }
                ) {
                    ReaderTopBar(
                        chapterTitle = chapterTitle,
                        dataSaver    = dataSaver,
                        onBack       = {
                            viewModel.saveProgressNow()
                            onBack()
                        },
                        onToggleDataSaver = { viewModel.toggleDataSaver() }
                    )
                }

                // ── Bottom Overlay ────────────────────────────────────────────
                AnimatedVisibility(
                    visible  = showUi,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter    = fadeIn() + slideInVertically { it },
                    exit     = fadeOut() + slideOutVertically { it }
                ) {
                    ReaderBottomBar(
                        currentPage = currentPage,
                        totalPages  = totalPages,
                        language    = language,
                        onPrev      = { viewModel.prevPage() },
                        onNext      = { viewModel.nextPage() },
                        onSlider    = { page -> viewModel.goToPage(page) }
                    )
                }
            }
        }
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@Composable
private fun ReaderTopBar(
    chapterTitle: String,
    dataSaver: Boolean,
    onBack: () -> Unit,
    onToggleDataSaver: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.75f))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text     = chapterTitle,
                color    = Color.White,
                fontWeight = FontWeight.Bold,
                style    = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            )
            // Data-saver toggl
        }
    }
}

// ── Bottom Bar ────────────────────────────────────────────────────────────────

@Composable
private fun ReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    language: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSlider: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.80f))
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Halaman X / Y
        Text(
            text  = "${currentPage + 1} / $totalPages",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        // Slider navigasi
        if (totalPages > 1) {
            Slider(
                value         = currentPage.toFloat(),
                onValueChange = { onSlider(it.toInt()) },
                valueRange    = 0f..(totalPages - 1).toFloat(),
                steps         = (totalPages - 2).coerceAtLeast(0),
                colors        = SliderDefaults.colors(
                    thumbColor        = AccentColor,
                    activeTrackColor  = AccentColor,
                    inactiveTrackColor = Color.White.copy(0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Tombol Prev / Next
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(
                onClick  = onPrev,
                enabled  = currentPage > 0
            ) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Prev",
                    tint   = if (currentPage > 0) Color.White else Color.White.copy(0.3f),
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text  = if (language == "id") "← Geser untuk pindah halaman →"
                else "← Swipe to change page →",
                color = Color.White.copy(0.45f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick  = onNext,
                enabled  = currentPage < totalPages - 1
            ) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint   = if (currentPage < totalPages - 1) Color.White else Color.White.copy(0.3f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

// ── Halaman Manga ─────────────────────────────────────────────────────────────

@Composable
private fun MangaPageImage(
    url: String,
    pageNumber: Int,
    totalPages: Int,
    language: String = "en"
) {
    val context = LocalContext.current

    Box(
        modifier     = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .size(coil.size.Size.ORIGINAL)
                .memoryCacheKey("page_${url.hashCode()}")
                .diskCacheKey("page_${url.hashCode()}")
                .build(),
            contentDescription = "Page $pageNumber of $totalPages",
            contentScale = ContentScale.Fit,
            modifier     = Modifier.fillMaxSize(),
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color     = AccentColor,
                            modifier  = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "p. $pageNumber",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            error = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.BrokenImage,
                        contentDescription = null,
                        tint     = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (language == "id") "Gagal memuat halaman $pageNumber"
                        else "Failed to load page $pageNumber",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        )

        // Badge nomor halaman (pojok kanan bawah)
        Surface(
            shape  = RoundedCornerShape(8.dp),
            color  = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 72.dp, end = 10.dp)  // di atas bottom bar
        ) {
            Text(
                text     = "$pageNumber / $totalPages",
                color    = Color.White,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}