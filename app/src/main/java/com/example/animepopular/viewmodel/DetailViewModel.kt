package com.example.animepopular.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.animepopular.data.preferences.AppPreferences
import com.example.animepopular.data.remote.dto.ApiResult
import com.example.animepopular.data.repository.FavoritesRepository
import com.example.animepopular.data.repository.MangaRepository
import com.example.animepopular.data.repository.ReviewRepository
import com.example.animepopular.model.Manga
import com.example.animepopular.model.Review
import com.example.animepopular.model.ReviewReply
import com.example.animepopular.util.Constants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModel(
    private val mangaRepository: MangaRepository,
    private val favoritesRepository: FavoritesRepository,
    private val reviewRepository: ReviewRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _manga = MutableStateFlow<ApiResult<Manga>>(ApiResult.Loading)
    val manga: StateFlow<ApiResult<Manga>> = _manga.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    val userId: StateFlow<String> = preferences.userIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Constants.GUEST_USER_ID)

    val isGuest: StateFlow<Boolean> = preferences.userModeFlow
        .map { it == Constants.USER_MODE_GUEST }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _favoriteAddedEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val favoriteAddedEvent: SharedFlow<String> = _favoriteAddedEvent.asSharedFlow()

    // ── New Review Form ───────────────────────────────────────────────────────
    private val _reviewUsername    = MutableStateFlow("")
    val reviewUsername: StateFlow<String> = _reviewUsername
    private val _reviewText        = MutableStateFlow("")
    val reviewText: StateFlow<String> = _reviewText
    private val _reviewRating      = MutableStateFlow(7f)
    val reviewRating: StateFlow<Float> = _reviewRating
    private val _selectedImageUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImageUris: StateFlow<List<Uri>> = _selectedImageUris.asStateFlow()
    private val _selectedGifUri    = MutableStateFlow<Uri?>(null)
    val selectedGifUri: StateFlow<Uri?> = _selectedGifUri.asStateFlow()

    // ── Edit Review Form ──────────────────────────────────────────────────────
    private val _editingReview  = MutableStateFlow<Review?>(null)
    val editingReview: StateFlow<Review?> = _editingReview.asStateFlow()
    private val _editText       = MutableStateFlow("")
    val editText: StateFlow<String> = _editText
    private val _editRating     = MutableStateFlow(7f)
    val editRating: StateFlow<Float> = _editRating
    private val _editImageUris  = MutableStateFlow<List<Uri>>(emptyList())
    val editImageUris: StateFlow<List<Uri>> = _editImageUris.asStateFlow()
    private val _editGifUri     = MutableStateFlow<Uri?>(null)
    val editGifUri: StateFlow<Uri?> = _editGifUri.asStateFlow()

    // ── Reply Form ─────────────────────────────────────────────────────────────
    private val _replyingToReviewId = MutableStateFlow<String?>(null)
    val replyingToReviewId: StateFlow<String?> = _replyingToReviewId.asStateFlow()
    private val _replyUsername  = MutableStateFlow("")
    val replyUsername: StateFlow<String> = _replyUsername
    private val _replyText      = MutableStateFlow("")
    val replyText: StateFlow<String> = _replyText

    private val _repliesMap = MutableStateFlow<Map<String, List<ReviewReply>>>(emptyMap())
    val repliesMap: StateFlow<Map<String, List<ReviewReply>>> = _repliesMap.asStateFlow()

    private val _submitStatus = MutableStateFlow<ReviewSubmitStatus>(ReviewSubmitStatus.Idle)
    val reviewSubmitStatus: StateFlow<ReviewSubmitStatus> = _submitStatus.asStateFlow()

    private var currentMangaId: String = ""
    private val replyJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    // ── Load ──────────────────────────────────────────────────────────────────

    fun loadManga(mangaId: String) {
        currentMangaId = mangaId
        viewModelScope.launch {
            _manga.value = ApiResult.Loading
            _manga.value = mangaRepository.getMangaById(mangaId)

            userId.flatMapLatest { uid ->
                favoritesRepository.isFavoriteFlow(mangaId, uid)
            }.collect { _isFavorite.value = it }
        }
        viewModelScope.launch {
            reviewRepository.getReviewsForManga(mangaId).collect { reviews ->
                _reviews.value = reviews
                syncReplyListeners(reviews)
            }
        }
    }

    private fun syncReplyListeners(reviews: List<Review>) {
        val activeIds = reviews.map { it.id }.toSet()
        replyJobs.keys.filter { it !in activeIds }.forEach { id ->
            replyJobs.remove(id)?.cancel()
            _repliesMap.value = _repliesMap.value - id
        }
        reviews.forEach { review ->
            if (review.id !in replyJobs) {
                replyJobs[review.id] = viewModelScope.launch {
                    reviewRepository.getRepliesForReview(review.id).collect { replies ->
                        _repliesMap.value = _repliesMap.value.toMutableMap().also {
                            it[review.id] = replies
                        }
                    }
                }
            }
        }
    }

    // ── Favorite ───────────────────────────────────────────────────────────────

    fun toggleFavorite() {
        viewModelScope.launch {
            val m = (_manga.value as? ApiResult.Success)?.data ?: return@launch
            val addedTitle = favoritesRepository.toggleFavorite(m, userId.value)
            if (addedTitle != null) {
                _favoriteAddedEvent.emit(addedTitle)
            }
        }
    }

    // ── New Review ─────────────────────────────────────────────────────────────

    fun onUsernameChange(v: String)   { _reviewUsername.value = v }
    fun onReviewTextChange(v: String) { _reviewText.value = v }
    fun onRatingChange(v: Float)      { _reviewRating.value = v }
    fun addImage(uri: Uri)            { if (_selectedImageUris.value.size < 5) _selectedImageUris.value += uri }
    fun removeImage(uri: Uri)         { _selectedImageUris.value = _selectedImageUris.value.filter { it != uri } }
    fun setGif(uri: Uri?)             { _selectedGifUri.value = uri }
    fun clearGif()                    { _selectedGifUri.value = null }

    fun submitReview(context: Context) {
        val username = _reviewUsername.value.trim()
        val text     = _reviewText.value.trim()
        if (username.isBlank()) { _submitStatus.value = ReviewSubmitStatus.Error("Username cannot be empty"); return }
        if (text.isBlank())     { _submitStatus.value = ReviewSubmitStatus.Error("Review cannot be empty"); return }

        viewModelScope.launch {
            _submitStatus.value = ReviewSubmitStatus.Loading
            val result = reviewRepository.addReview(
                context = context,
                review = Review(
                    mangaId = currentMangaId,
                    userId = userId.value,
                    username = username,
                    reviewText = text,
                    rating = _reviewRating.value,
                    timestamp = System.currentTimeMillis()
                ),
                imageUris = _selectedImageUris.value,
                gifUri = _selectedGifUri.value
            )
            if (result.isSuccess) {
                _reviewUsername.value = ""
                _reviewText.value = ""
                _reviewRating.value = 7f
                _selectedImageUris.value = emptyList()
                _selectedGifUri.value = null
                _submitStatus.value = ReviewSubmitStatus.Success
            } else {
                Timber.e(result.exceptionOrNull(), "Failed to submit review")
                _submitStatus.value = ReviewSubmitStatus.Error(
                    result.exceptionOrNull()?.message ?: "Failed to send review"
                )
            }
        }
    }

    // ── Edit Review ────────────────────────────────────────────────────────────

    fun startEditing(review: Review) {
        _editingReview.value = review
        _editText.value = review.reviewText
        _editRating.value = review.rating
        _editImageUris.value = emptyList()
        _editGifUri.value = null
    }

    fun cancelEditing() { _editingReview.value = null }

    fun onEditTextChange(v: String)  { _editText.value = v }
    fun onEditRatingChange(v: Float) { _editRating.value = v }
    fun addEditImage(uri: Uri)       { if (_editImageUris.value.size < 5) _editImageUris.value += uri }
    fun removeEditImage(uri: Uri)    { _editImageUris.value = _editImageUris.value.filter { it != uri } }
    fun setEditGif(uri: Uri?)        { _editGifUri.value = uri }

    fun submitEdit(context: Context) {
        val review = _editingReview.value ?: return
        val text   = _editText.value.trim()
        if (text.isBlank()) { _submitStatus.value = ReviewSubmitStatus.Error("Review cannot be empty"); return }

        viewModelScope.launch {
            _submitStatus.value = ReviewSubmitStatus.Loading
            val result = reviewRepository.editReview(
                context = context,
                reviewId = review.id,
                mangaId = review.mangaId,
                newText = text,
                newRating = _editRating.value,
                existingImageUrls = review.imagePaths,
                newImageUris = _editImageUris.value,
                newGifUri = _editGifUri.value,
                existingGifUrl = review.gifPath
            )
            if (result.isSuccess) {
                _editingReview.value = null
                _submitStatus.value = ReviewSubmitStatus.Success
            } else {
                _submitStatus.value = ReviewSubmitStatus.Error(
                    result.exceptionOrNull()?.message ?: "Failed to update review"
                )
            }
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch { reviewRepository.deleteReview(reviewId) }
    }

    // ── Reply ──────────────────────────────────────────────────────────────────

    fun startReply(reviewId: String) {
        _replyingToReviewId.value = reviewId
        _replyUsername.value = ""
        _replyText.value = ""
    }

    fun cancelReply() { _replyingToReviewId.value = null }
    fun onReplyUsernameChange(v: String) { _replyUsername.value = v }
    fun onReplyTextChange(v: String)     { _replyText.value = v }

    fun submitReply() {
        val reviewId = _replyingToReviewId.value ?: return
        val username = _replyUsername.value.trim()
        val text     = _replyText.value.trim()
        if (username.isBlank()) { _submitStatus.value = ReviewSubmitStatus.Error("Username required"); return }
        if (text.isBlank())     { _submitStatus.value = ReviewSubmitStatus.Error("Reply cannot be empty"); return }

        viewModelScope.launch {
            val result = reviewRepository.addReply(
                ReviewReply(
                    reviewId = reviewId,
                    mangaId = currentMangaId,
                    userId = userId.value,
                    username = username,
                    replyText = text
                )
            )
            if (result.isSuccess) {
                _replyingToReviewId.value = null
                _replyUsername.value = ""
                _replyText.value = ""
                _submitStatus.value = ReviewSubmitStatus.Success
            } else {
                _submitStatus.value = ReviewSubmitStatus.Error(
                    result.exceptionOrNull()?.message ?: "Failed to send reply"
                )
            }
        }
    }

    fun deleteReply(replyId: String) {
        viewModelScope.launch { reviewRepository.deleteReply(replyId) }
    }

    fun resetSubmitStatus() { _submitStatus.value = ReviewSubmitStatus.Idle }

    override fun onCleared() {
        super.onCleared()
        replyJobs.values.forEach { it.cancel() }
        replyJobs.clear()
    }

    class Factory(
        private val mangaRepository: MangaRepository,
        private val favoritesRepository: FavoritesRepository,
        private val reviewRepository: ReviewRepository,
        private val preferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            DetailViewModel(mangaRepository, favoritesRepository, reviewRepository, preferences) as T
    }
}

sealed class ReviewSubmitStatus {
    data object Idle    : ReviewSubmitStatus()
    data object Loading : ReviewSubmitStatus()
    data object Success : ReviewSubmitStatus()
    data class Error(val message: String) : ReviewSubmitStatus()
}
