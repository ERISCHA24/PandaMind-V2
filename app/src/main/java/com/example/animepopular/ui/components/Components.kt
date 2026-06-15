package com.example.animepopular.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.animepopular.model.Manga
import com.example.animepopular.ui.theme.*

// ── GIF-aware Image Loader ─────────────────────────────────────────────────────

@Composable
fun rememberGifImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember {
        ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
}

// ── Manga Card (List Item) ─────────────────────────────────────────────────────
//
// Parameter [isFavoriteOverride]:
//   - null  → gunakan manga.isFavorite (default, kompatibel dengan kode lama)
//   - true/false → override dari ViewModel favoriteIds Flow (real-time)
//
// Dengan cara ini semua screen (Home, Search, TopRated, Genre) bisa
// menampilkan state ❤ yang akurat tanpa harus reload list.

@Composable
fun MangaCard(
    manga: Manga,
    onDetailClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
    isFavoriteOverride: Boolean? = null          // ✅ NEW
) {
    val context = LocalContext.current
    val isFav = isFavoriteOverride ?: manga.isFavorite  // ✅ resolve state

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Cover Image
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(manga.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(88.dp)
                    .height(124.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    manga.year?.let {
                        Text(
                            text = it.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    if (manga.tags.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccentColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = manga.tags.take(2).joinToString(" / "),
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row {
                    Text(
                        text = "★ ${"%.1f".format(manga.rating)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = RatingColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = manga.status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = manga.description.ifBlank { "No description available." },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDetailClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        border = BorderStroke(1.dp, AccentColor)
                    ) {
                        Text("Detail", style = MaterialTheme.typography.labelSmall, color = AccentColor)
                    }

                    // ✅ Tombol favorit dengan animasi
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        AnimatedContent(
                            targetState = isFav,
                            transitionSpec = {
                                scaleIn(initialScale = 0.7f) + fadeIn() togetherWith
                                        scaleOut(targetScale = 0.7f) + fadeOut()
                            },
                            label = "fav_icon"
                        ) { fav ->
                            Icon(
                                imageVector = if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (fav) "Remove from favorites" else "Add to favorites",
                                tint = if (fav) AccentColor else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Carousel / Feature Card ────────────────────────────────────────────────────

@Composable
fun CarouselMangaCard(
    manga: Manga,
    onDetailClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(context).data(manga.coverUrl).crossfade(true).build(),
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.6f
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(0.9f)),
                            startY = 60f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    manga.title, color = Color.White, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium, maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (manga.tags.isNotEmpty()) {
                    Text(
                        manga.tags.take(2).joinToString(" / "),
                        color = Color.LightGray, style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onDetailClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Detail", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ── Loading State ──────────────────────────────────────────────────────────────

@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AccentColor)
    }
}

// ── Error State ────────────────────────────────────────────────────────────────

@Composable
fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AccentColor)) {
            Text("Retry")
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
fun EmptyContent(message: String, emoji: String = "🌙", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text(message, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Section Header ─────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ── Rating Bar ─────────────────────────────────────────────────────────────────

@Composable
fun RatingBar(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    maxRating: Int = 10,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${rating.toInt()}/10", color = RatingColor, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(40.dp)
        )
        Slider(
            value = rating,
            onValueChange = onRatingChange,
            valueRange = 1f..maxRating.toFloat(),
            steps = maxRating - 2,
            colors = SliderDefaults.colors(
                thumbColor = RatingColor,
                activeTrackColor = RatingColor
            ),
            modifier = Modifier.weight(1f)
        )
    }
}