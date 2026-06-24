package com.example.animepopular.ui.screens.detail

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.animepopular.model.Review
import com.example.animepopular.model.ReviewReply
import com.example.animepopular.ui.components.LoadingContent
import com.example.animepopular.ui.components.RatingBar
import com.example.animepopular.ui.components.rememberGifImageLoader
import com.example.animepopular.ui.theme.*
import com.example.animepopular.util.ImageUtil
import com.example.animepopular.viewmodel.DetailViewModel
import com.example.animepopular.viewmodel.ReviewSubmitStatus
import com.example.animepopular.data.remote.dto.ApiResult
import com.google.accompanist.permissions.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    mangaId: String,
    onBack: () -> Unit,
    onNavigateToChapters: (mangaId: String, mangaTitle: String, coverUrl: String) -> Unit,   // ✅ NEW param
    language: String = "en"
) {
    val context         = LocalContext.current
    val gifImageLoader  = rememberGifImageLoader()
    val mangaState      by viewModel.manga.collectAsStateWithLifecycle()
    val reviews         by viewModel.reviews.collectAsStateWithLifecycle()
    val isFavorite      by viewModel.isFavorite.collectAsStateWithLifecycle()
    val repliesMap      by viewModel.repliesMap.collectAsStateWithLifecycle()
    val submitStatus    by viewModel.reviewSubmitStatus.collectAsStateWithLifecycle()
    val editingReview   by viewModel.editingReview.collectAsStateWithLifecycle()
    val replyingToId    by viewModel.replyingToReviewId.collectAsStateWithLifecycle()

    // New review form
    val username        by viewModel.reviewUsername.collectAsStateWithLifecycle()
    val reviewText      by viewModel.reviewText.collectAsStateWithLifecycle()
    val reviewRating    by viewModel.reviewRating.collectAsStateWithLifecycle()
    val selectedImages  by viewModel.selectedImageUris.collectAsStateWithLifecycle()
    val selectedGif     by viewModel.selectedGifUri.collectAsStateWithLifecycle()

    // Edit review form
    val editText        by viewModel.editText.collectAsStateWithLifecycle()
    val editRating      by viewModel.editRating.collectAsStateWithLifecycle()
    val editImages      by viewModel.editImageUris.collectAsStateWithLifecycle()
    val editGif         by viewModel.editGifUri.collectAsStateWithLifecycle()

    // Reply form
    val replyUsername   by viewModel.replyUsername.collectAsStateWithLifecycle()
    val replyText       by viewModel.replyText.collectAsStateWithLifecycle()

    LaunchedEffect(mangaId) { viewModel.loadManga(mangaId) }

    val snackbarHostState = remember { SnackbarHostState() }

    // ✅ Snackbar untuk review submit
    LaunchedEffect(submitStatus) {
        when (submitStatus) {
            is ReviewSubmitStatus.Success -> {
                snackbarHostState.showSnackbar(if (language == "id") "Berhasil!" else "Success!")
                viewModel.resetSubmitStatus()
            }
            is ReviewSubmitStatus.Error -> {
                snackbarHostState.showSnackbar((submitStatus as ReviewSubmitStatus.Error).message)
                viewModel.resetSubmitStatus()
            }
            else -> {}
        }
    }

    // ✅ Snackbar notifikasi saat manga ditambahkan ke favorit
    LaunchedEffect(Unit) {
        viewModel.favoriteAddedEvent.collect { title ->
            val message = if (language == "id")
                "\"$title\" ditambahkan ke Favorit ❤️"
            else
                "\"$title\" added to Favorites ❤️"
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Permission + pickers
    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberMultiplePermissionsState(listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO))
    } else {
        rememberMultiplePermissionsState(listOf(Manifest.permission.READ_EXTERNAL_STORAGE))
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (editingReview != null) uris.forEach { viewModel.addEditImage(it) }
        else uris.forEach { viewModel.addImage(it) }
    }
    val gifPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (editingReview != null) viewModel.setEditGif(uri)
        else uri?.let { viewModel.setGif(it) }
    }

    // Delete confirmation dialog
    var reviewToDelete by remember { mutableStateOf<String?>(null) }
    if (reviewToDelete != null) {
        AlertDialog(
            onDismissRequest = { reviewToDelete = null },
            containerColor = CardBackground,
            title = { Text(if (language == "id") "Hapus Review?" else "Delete Review?", color = TextPrimary) },
            text  = { Text(if (language == "id") "Review ini akan dihapus beserta balasannya."
            else "This review and all its replies will be deleted.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    reviewToDelete?.let { viewModel.deleteReview(it) }
                    reviewToDelete = null
                }) { Text(if (language == "id") "Hapus" else "Delete", color = AccentColor) }
            },
            dismissButton = {
                TextButton(onClick = { reviewToDelete = null }) {
                    Text(if (language == "id") "Batal" else "Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text(if (language == "id") "Detail" else "Detail", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorPrimary)
            )
        }
    ) { padding ->
        when (val state = mangaState) {
            is ApiResult.Loading -> LoadingContent(Modifier.fillMaxSize().padding(padding))

            is ApiResult.Success -> {
                val manga = state.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Cover Banner ──────────────────────────────────────────
                    Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(manga.coverUrl).crossfade(true).build(),
                            contentDescription = manga.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = 0.55f
                        )
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.verticalGradient(colors = listOf(Color.Transparent, BackgroundDark))
                            )
                        )
                        // ✅ Hanya tombol Favorit — tombol Watched dihapus
                        Row(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionIconButton(
                                icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                tint = if (isFavorite) AccentColor else TextSecondary,
                                onClick = { viewModel.toggleFavorite() }
                            )
                        }
                    }

                    // ── Info Card ─────────────────────────────────────────────
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).offset(y = (-20).dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                Text(manga.title, style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                                manga.year?.let {
                                    Surface(shape = RoundedCornerShape(8.dp), color = AccentColor.copy(0.15f)) {
                                        Text("$it", color = AccentColor,
                                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                            if (manga.tags.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(manga.tags.take(5)) { tag ->
                                        Surface(shape = RoundedCornerShape(6.dp), color = AccentColor.copy(0.15f)) {
                                            Text(tag, color = AccentColor, style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                        }
                                    }
                                }
                            }
                            Divider(Modifier.padding(vertical = 10.dp), color = DividerColor)
                            // ✅ Hanya Rating + Status + Content (Watched dihapus dari row ini)
                            Row(Modifier.fillMaxWidth()) {
                                InfoItem("Rating", "★ ${"%.1f".format(manga.rating)}", RatingColor, Modifier.weight(1f))
                                InfoItem("Status", manga.status.replaceFirstChar { it.uppercase() }, AccentColor, Modifier.weight(1f))
                                InfoItem("Content", manga.contentRating.replaceFirstChar { it.uppercase() }, TextSecondary, Modifier.weight(1f))
                            }
                            if (manga.authorName.isNotBlank() && manga.authorName != "Unknown") {
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Person, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(manga.authorName, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Divider(Modifier.padding(vertical = 10.dp), color = DividerColor)
                            Text(if (language == "id") "Sinopsis" else "Synopsis",
                                color = TextSecondary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(manga.description.ifBlank { if (language == "id") "Tidak ada sinopsis." else "No synopsis available." },
                                color = TextPrimary, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)

                            // ── Read Chapters Button ──────────────────────────
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { onNavigateToChapters(manga.id, manga.title, manga.coverUrl) },   // ✅ NEW — kirim coverUrl
                                colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (language == "id") "Baca Chapter" else "Read Chapters",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ── CARD 1: Review Pengguna ─────────────────────────────
                    Card(
                        modifier  = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 4.dp),
                        shape     = RoundedCornerShape(20.dp),
                        colors    = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                if (language == "id") "💬 Chat Global" else "💬 Global Chat",
                                color      = TextPrimary,
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (language == "id")
                                    "${reviews.size} pesan · sinkron real-time via Firestore"
                                else
                                    "${reviews.size} messages · real-time via Firestore",
                                color    = TextSecondary,
                                style    = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                            )

                            reviews.forEach { review ->
                                val replies = repliesMap[review.id] ?: emptyList()
                                ReviewCard(
                                    review        = review,
                                    replies       = replies,
                                    imageLoader   = gifImageLoader,
                                    language      = language,
                                    onEdit        = { viewModel.startEditing(review) },
                                    onDelete      = { reviewToDelete = review.id },
                                    onReply       = { viewModel.startReply(review.id) },
                                    onDeleteReply = { viewModel.deleteReply(it) }
                                )
                                Spacer(Modifier.height(8.dp))
                            }

                            if (reviews.isEmpty()) {
                                Text(
                                    if (language == "id") "Belum ada review. Jadilah yang pertama!"
                                    else "No reviews yet. Be the first!",
                                    color    = TextSecondary,
                                    style    = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            AnimatedVisibility(visible = editingReview != null) {
                                Column {
                                    Divider(Modifier.padding(vertical = 12.dp), color = DividerColor)
                                    EditReviewSection(
                                        editText      = editText,
                                        editRating    = editRating,
                                        editImages    = editImages,
                                        editGif       = editGif,
                                        existingPaths = editingReview?.imagePaths ?: emptyList(),
                                        language      = language,
                                        submitStatus  = submitStatus,
                                        onTextChange      = viewModel::onEditTextChange,
                                        onRatingChange    = viewModel::onEditRatingChange,
                                        onPickImages      = {
                                            if (mediaPermission.allPermissionsGranted) imagePicker.launch("image/*")
                                            else mediaPermission.launchMultiplePermissionRequest()
                                        },
                                        onPickGif         = {
                                            if (mediaPermission.allPermissionsGranted) gifPicker.launch("image/gif")
                                            else mediaPermission.launchMultiplePermissionRequest()
                                        },
                                        onRemoveEditImage = viewModel::removeEditImage,
                                        onClearGif        = { viewModel.setEditGif(null) },
                                        onSubmit          = { viewModel.submitEdit(context) },
                                        onCancel          = viewModel::cancelEditing,
                                        gifLoader         = gifImageLoader
                                    )
                                }
                            }

                            AnimatedVisibility(visible = replyingToId != null) {
                                Column {
                                    Divider(Modifier.padding(vertical = 12.dp), color = DividerColor)
                                    ReplySection(
                                        replyUsername    = replyUsername,
                                        replyText        = replyText,
                                        language         = language,
                                        onUsernameChange = viewModel::onReplyUsernameChange,
                                        onTextChange     = viewModel::onReplyTextChange,
                                        onSubmit         = viewModel::submitReply,
                                        onCancel         = viewModel::cancelReply
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── CARD 2: Tulis Review Baru ─────────────────────────────
                    Card(
                        modifier  = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 4.dp),
                        shape     = RoundedCornerShape(20.dp),
                        colors    = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                if (language == "id") "✍️ Tulis Review" else "✍️ Write a Review",
                                color      = TextPrimary,
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier   = Modifier.padding(bottom = 14.dp)
                            )

                            OutlinedTextField(
                                value         = username,
                                onValueChange = viewModel::onUsernameChange,
                                label         = { Text("Username", color = TextSecondary) },
                                singleLine    = true,
                                colors        = tfColors(),
                                shape         = RoundedCornerShape(12.dp),
                                modifier      = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value         = reviewText,
                                onValueChange = viewModel::onReviewTextChange,
                                label         = {
                                    Text(
                                        if (language == "id") "Tulis reviewmu di sini..."
                                        else "Write your review here...",
                                        color = TextSecondary
                                    )
                                },
                                minLines      = 3,
                                maxLines      = 6,
                                colors        = tfColors(),
                                shape         = RoundedCornerShape(12.dp),
                                modifier      = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                if (language == "id") "Rating kamu:" else "Your rating:",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                            RatingBar(
                                rating         = reviewRating,
                                onRatingChange = viewModel::onRatingChange,
                                modifier       = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                if (language == "id") "📷 Tambah Gambar / GIF" else "📷 Add Images / GIF",
                                color      = TextPrimary,
                                style      = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MediaPickerButton(
                                    label  = if (language == "id") "Gambar (${selectedImages.size}/5)"
                                    else "Images (${selectedImages.size}/5)",
                                    icon   = Icons.Filled.Image,
                                    active = selectedImages.isNotEmpty()
                                ) {
                                    if (mediaPermission.allPermissionsGranted) imagePicker.launch("image/*")
                                    else mediaPermission.launchMultiplePermissionRequest()
                                }
                                MediaPickerButton(
                                    label  = if (selectedGif != null) "GIF ✓" else "GIF",
                                    icon   = Icons.Filled.Gif,
                                    active = selectedGif != null
                                ) {
                                    if (mediaPermission.allPermissionsGranted) gifPicker.launch("image/gif")
                                    else mediaPermission.launchMultiplePermissionRequest()
                                }
                            }

                            AnimatedVisibility(visible = selectedImages.isNotEmpty() || selectedGif != null) {
                                Column {
                                    Spacer(Modifier.height(10.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(selectedImages) { uri ->
                                            SelectedMediaThumb(uri, gifImageLoader, false) {
                                                viewModel.removeImage(uri)
                                            }
                                        }
                                        selectedGif?.let { gif ->
                                            item {
                                                SelectedMediaThumb(gif, gifImageLoader, true) {
                                                    viewModel.clearGif()
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Button(
                                onClick  = { viewModel.submitReview(context) },
                                enabled  = submitStatus !is ReviewSubmitStatus.Loading,
                                colors   = ButtonDefaults.buttonColors(containerColor = AccentColor),
                                shape    = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                if (submitStatus is ReviewSubmitStatus.Loading) {
                                    CircularProgressIndicator(
                                        color       = Color.White,
                                        modifier    = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (language == "id") "Kirim Review" else "Submit Review",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(80.dp))
                }
            }
            else -> LoadingContent(Modifier.fillMaxSize().padding(padding))
        }
    }
}

// ── Review Card ───────────────────────────────────────────────────────────────

@Composable
private fun ReviewCard(
    review: Review,
    replies: List<ReviewReply>,
    imageLoader: ImageLoader,
    language: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReply: () -> Unit,
    onDeleteReply: (String) -> Unit
) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    var showReplies by remember { mutableStateOf(false) }
    var showMenu    by remember { mutableStateOf(false) }

    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceColor, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, null, tint = AccentColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(review.username, color = AccentColor, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("★ ${"%.1f".format(review.rating)}", color = RatingColor,
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.MoreVert, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (language == "id") "Edit" else "Edit", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Filled.Edit, null, tint = AccentColor) },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text(if (language == "id") "Hapus" else "Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(review.reviewText, color = TextPrimary, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)

            val allMedia = review.imagePaths + listOfNotNull(review.gifPath)
            if (allMedia.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(allMedia) { path ->
                        val model: Any? = when {
                            path.startsWith("http", ignoreCase = true) -> path
                            else -> {
                                val file = File(path)
                                if (file.exists()) file else null
                            }
                        }
                        if (model != null) {
                            val isGif = ImageUtil.isGif(path)
                            Box {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(model).crossfade(true).build(),
                                    imageLoader = imageLoader,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, if (isGif) AccentColor else DividerColor, RoundedCornerShape(8.dp))
                                )
                                if (isGif) {
                                    Surface(color = AccentColor, shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.align(Alignment.BottomStart).padding(2.dp)) {
                                        Text("GIF", color = Color.White, style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(sdf.format(Date(review.timestamp)), color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onReply, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                    Icon(Icons.Filled.Reply, null, tint = AccentColor, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (language == "id") "Balas" else "Reply", color = AccentColor,
                        style = MaterialTheme.typography.labelSmall)
                }
                if (replies.isNotEmpty()) {
                    TextButton(onClick = { showReplies = !showReplies },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("${replies.size} ${if (language == "id") "balasan" else "replies"}",
                            color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        Icon(if (showReplies) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            AnimatedVisibility(showReplies && replies.isNotEmpty()) {
                Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp)) {
                    Divider(Modifier.padding(bottom = 8.dp), color = DividerColor)
                    replies.forEach { reply ->
                        ReplyItem(reply = reply, language = language, onDelete = { onDeleteReply(reply.id) })
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyItem(reply: ReviewReply, language: String, onDelete: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.SubdirectoryArrowRight, null, tint = DividerColor,
            modifier = Modifier.size(16.dp).padding(top = 2.dp))
        Spacer(Modifier.width(6.dp))
        Surface(shape = RoundedCornerShape(8.dp), color = CardBackground, modifier = Modifier.weight(1f)) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(reply.username, color = AccentColor, style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold)
                    Text(reply.replyText, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                    Text(sdf.format(Date(reply.timestamp)), color = TextSecondary, fontSize = 10.sp)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Filled.Close, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// ── Edit Review Section ───────────────────────────────────────────────────────

@Composable
private fun EditReviewSection(
    editText: String, editRating: Float,
    editImages: List<Uri>, editGif: Uri?,
    existingPaths: List<String>,
    language: String, submitStatus: ReviewSubmitStatus,
    onTextChange: (String) -> Unit, onRatingChange: (Float) -> Unit,
    onPickImages: () -> Unit, onPickGif: () -> Unit,
    onRemoveEditImage: (Uri) -> Unit, onClearGif: () -> Unit,
    onSubmit: () -> Unit, onCancel: () -> Unit,
    gifLoader: ImageLoader
) {
    val context = LocalContext.current
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (language == "id") "✏️ Edit Review" else "✏️ Edit Review",
                color = AccentColor, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            TextButton(onClick = onCancel) {
                Text(if (language == "id") "Batal" else "Cancel", color = TextSecondary)
            }
        }
        OutlinedTextField(value = editText, onValueChange = onTextChange,
            label = { Text("Review", color = TextSecondary) },
            minLines = 3, maxLines = 6, colors = tfColors(),
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        RatingBar(rating = editRating, onRatingChange = onRatingChange, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        if (existingPaths.isNotEmpty()) {
            Text(if (language == "id") "Gambar saat ini:" else "Current images:",
                color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                items(existingPaths) { path ->
                    val model: Any? = when {
                        path.startsWith("http", ignoreCase = true) -> path
                        else -> {
                            val f = File(path)
                            if (f.exists()) f else null
                        }
                    }
                    if (model != null) {
                        AsyncImage(model = ImageRequest.Builder(context).data(model).build(),
                            imageLoader = gifLoader, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MediaPickerButton(label = if (language == "id") "Tambah Gambar" else "Add Images",
                icon = Icons.Filled.Image, active = editImages.isNotEmpty(), onClick = onPickImages)
            MediaPickerButton(label = if (editGif != null) "GIF ✓" else "GIF",
                icon = Icons.Filled.Gif, active = editGif != null, onClick = onPickGif)
        }
        if (editImages.isNotEmpty() || editGif != null) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                items(editImages) { uri -> SelectedMediaThumb(uri, gifLoader, false) { onRemoveEditImage(uri) } }
                editGif?.let { item { SelectedMediaThumb(it, gifLoader, true, onClearGif) } }
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = onSubmit, enabled = submitStatus !is ReviewSubmitStatus.Loading,
            colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
            shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().height(42.dp)) {
            Text(if (language == "id") "Simpan Perubahan" else "Save Changes", fontWeight = FontWeight.Bold)
        }
    }
}

// ── Reply Section ─────────────────────────────────────────────────────────────

@Composable
private fun ReplySection(
    replyUsername: String, replyText: String, language: String,
    onUsernameChange: (String) -> Unit, onTextChange: (String) -> Unit,
    onSubmit: () -> Unit, onCancel: () -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (language == "id") "💬 Balas Review" else "💬 Reply to Review",
                color = AccentColor, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            TextButton(onClick = onCancel) {
                Text(if (language == "id") "Batal" else "Cancel", color = TextSecondary)
            }
        }
        OutlinedTextField(value = replyUsername, onValueChange = onUsernameChange,
            label = { Text("Username", color = TextSecondary) },
            singleLine = true, colors = tfColors(),
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = replyText, onValueChange = onTextChange,
            label = { Text(if (language == "id") "Balasan..." else "Reply...", color = TextSecondary) },
            minLines = 2, maxLines = 4, colors = tfColors(),
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = onSubmit,
            colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
            shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().height(42.dp)) {
            Text(if (language == "id") "Kirim Balasan" else "Send Reply", fontWeight = FontWeight.Bold)
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp).clip(CircleShape).background(SurfaceColor.copy(0.8f))
    ) { Icon(icon, null, tint = tint) }
}

@Composable
private fun InfoItem(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(2.dp))
        Text(value, color = valueColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MediaPickerButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(onClick = onClick,
        border = BorderStroke(1.dp, if (active) AccentColor else DividerColor),
        shape = RoundedCornerShape(12.dp)) {
        Icon(icon, null, tint = if (active) AccentColor else TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = if (active) AccentColor else TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SelectedMediaThumb(uri: Uri, imageLoader: ImageLoader, isGif: Boolean, onRemove: () -> Unit) {
    val context = LocalContext.current
    Box(modifier = Modifier.size(72.dp)) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(uri).crossfade(true).build(),
            imageLoader = imageLoader, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                .border(1.5.dp, if (isGif) AccentColor else DividerColor, RoundedCornerShape(10.dp))
        )
        if (isGif) {
            Surface(color = AccentColor, shape = RoundedCornerShape(4.dp),
                modifier = Modifier.align(Alignment.BottomStart).padding(3.dp)) {
                Text("GIF", color = Color.White, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
            }
        }
        IconButton(onClick = onRemove,
            modifier = Modifier.size(20.dp).align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)
                .clip(CircleShape).background(Color.Black.copy(0.7f))) {
            Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun tfColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentColor, unfocusedBorderColor = DividerColor,
    focusedContainerColor = SurfaceColor, unfocusedContainerColor = SurfaceColor,
    cursorColor = AccentColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedLabelColor = AccentColor, unfocusedLabelColor = TextSecondary
)