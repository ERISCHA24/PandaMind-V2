package com.example.animepopular.data.repository

import com.example.animepopular.data.local.dao.*
import com.example.animepopular.data.local.entity.*
import com.example.animepopular.model.*
import com.example.animepopular.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

private val repoJson = Json { ignoreUnknownKeys = true }

class FavoritesRepository(
    private val favoriteDao: FavoriteDao,
    private val watchedDao: WatchedDao,
    private val reviewDao: ReviewDao,
    private val reviewReplyDao: ReviewReplyDao
) {

    // ── Favorites ─────────────────────────────────────────────────────────────

    fun getAllFavorites(userId: String): Flow<List<Manga>> =
        favoriteDao.getAllFavorites(userId).map { list ->
            list.map { fav ->
                Manga(
                    id            = fav.mangaId,
                    title         = fav.title,
                    titleJa       = fav.title,
                    // ✅ description dari Room (sudah disimpan saat ditambahkan)
                    description   = fav.description,
                    status        = fav.status,
                    year          = null,
                    contentRating = "safe",
                    coverUrl      = buildCoverUrl(fav.mangaId, fav.coverFileName),
                    tags          = emptyList(),
                    authorName    = "",
                    followsCount  = 0,
                    rating        = fav.rating,
                    isFavorite    = true
                )
            }
        }

    fun getAllFavoriteIds(userId: String): Flow<Set<String>> =
        favoriteDao.getFavoriteIds(userId).map { it.toSet() }

    fun isFavoriteFlow(mangaId: String, userId: String): Flow<Boolean> =
        favoriteDao.isFavoriteFlow(mangaId, userId)

    /**
     * Toggle favorit.
     * Return nilai: title manga jika BARU DITAMBAHKAN, null jika dihapus.
     * Caller (ViewModel) menggunakan return value ini untuk menampilkan snackbar.
     */
    suspend fun toggleFavorite(manga: Manga, userId: String): String? =
        withContext(Dispatchers.IO) {
            if (favoriteDao.isFavorite(manga.id, userId)) {
                favoriteDao.delete(manga.id, userId)
                Timber.d("[$userId] Removed favorite: ${manga.title}")
                null   // dihapus → tidak perlu snackbar "ditambahkan"
            } else {
                val coverFileName = manga.coverUrl
                    .substringAfterLast("/")
                    .removeSuffix(".512.jpg")
                    .removeSuffix(".256.jpg")
                favoriteDao.insert(
                    FavoriteEntity(
                        mangaId       = manga.id,
                        userId        = userId,
                        title         = manga.title,
                        coverFileName = coverFileName,
                        status        = manga.status,
                        rating        = manga.rating,
                        // ✅ simpan description agar FavoritesScreen bisa menampilkannya
                        description   = manga.description
                    )
                )
                Timber.d("[$userId] Added favorite: ${manga.title}")
                manga.title   // ditambahkan → kembalikan title untuk snackbar
            }
        }

    fun getFavoriteCount(userId: String): Flow<Int> =
        favoriteDao.getFavoriteCount(userId)

    // ── Watched ───────────────────────────────────────────────────────────────

    fun isWatchedFlow(mangaId: String, userId: String): Flow<Boolean> =
        watchedDao.isWatchedFlow(mangaId, userId)

    suspend fun toggleWatched(mangaId: String, title: String, userId: String) =
        withContext(Dispatchers.IO) {
            if (watchedDao.isWatched(mangaId, userId)) {
                watchedDao.delete(mangaId, userId)
            } else {
                watchedDao.insert(WatchedEntity(mangaId = mangaId, userId = userId, title = title))
            }
        }

    fun getWatchedCount(userId: String): Flow<Int> =
        watchedDao.getWatchedCount(userId)

    // ── Reviews ────────────────────────────────────────────────────────────────

    fun getReviewsForManga(mangaId: String): Flow<List<Review>> =
        reviewDao.getReviewsForManga(mangaId).map { list -> list.map { it.toReviewDomain() } }

    fun getReviewCount(mangaId: String): Flow<Int> =
        reviewDao.getReviewCount(mangaId)

    fun getTotalReviewCount(userId: String): Flow<Int> =
        reviewDao.getTotalReviewCount(userId)

    suspend fun addReview(review: Review) = withContext(Dispatchers.IO) {
        reviewDao.insert(review.toReviewEntity())
        Timber.d("Review added for manga=${review.mangaId} by ${review.userId}")
    }

    suspend fun editReview(
        reviewId: Long,
        newText: String,
        newRating: Float,
        newImagePaths: List<String>,
        newGifPath: String?
    ) = withContext(Dispatchers.IO) {
        val existing = reviewDao.getById(reviewId) ?: return@withContext
        val updated = existing.copy(
            reviewText = newText, rating = newRating,
            imagePaths = repoJson.encodeToString(newImagePaths),
            gifPath = newGifPath,
            timestamp = System.currentTimeMillis()
        )
        reviewDao.update(updated)
    }

    suspend fun deleteReview(reviewId: Long) = withContext(Dispatchers.IO) {
        reviewReplyDao.deleteByReviewId(reviewId)
        reviewDao.deleteById(reviewId)
    }

    // ── Replies ───────────────────────────────────────────────────────────────

    fun getRepliesForReview(reviewId: Long): Flow<List<ReviewReply>> =
        reviewReplyDao.getRepliesForReview(reviewId).map { list -> list.map { it.toReplyDomain() } }

    suspend fun addReply(reply: ReviewReply) = withContext(Dispatchers.IO) {
        reviewReplyDao.insert(reply.toReplyEntity())
    }

    suspend fun deleteReply(replyId: Long) = withContext(Dispatchers.IO) {
        reviewReplyDao.deleteById(replyId)
    }

    // ── Clear guest data ──────────────────────────────────────────────────────

    suspend fun clearGuestData() = withContext(Dispatchers.IO) {
        val guestId = Constants.GUEST_USER_ID
        favoriteDao.deleteAllForUser(guestId)
        watchedDao.deleteAllForUser(guestId)
        reviewDao.deleteAllForUser(guestId)
        reviewReplyDao.deleteAllForUser(guestId)
        Timber.d("Guest data cleared")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildCoverUrl(mangaId: String, fileName: String): String {
        if (fileName.isBlank()) return ""
        return "https://uploads.mangadex.org/covers/$mangaId/$fileName.512.jpg"
    }
}

// ── Extension mappers ─────────────────────────────────────────────────────────

private val extJson = Json { ignoreUnknownKeys = true }

private fun ReviewEntity.toReviewDomain(): Review {
    val paths = try { extJson.decodeFromString<List<String>>(imagePaths) } catch (e: Exception) { emptyList() }
    return Review(
        id = id.toString(), mangaId = mangaId, userId = userId, username = username,
        reviewText = reviewText, rating = rating,
        timestamp = timestamp, imagePaths = paths, gifPath = gifPath
    )
}

private fun Review.toReviewEntity() = ReviewEntity(
    id = id.toLongOrNull() ?: 0L, mangaId = mangaId, userId = userId, username = username,
    reviewText = reviewText, rating = rating, timestamp = timestamp,
    imagePaths = extJson.encodeToString(imagePaths), gifPath = gifPath
)

private fun ReviewReplyEntity.toReplyDomain() = ReviewReply(
    id = id.toString(), reviewId = reviewId.toString(), mangaId = mangaId,
    userId = userId, username = username,
    replyText = replyText, timestamp = timestamp
)

private fun ReviewReply.toReplyEntity() = ReviewReplyEntity(
    id = id.toLongOrNull() ?: 0L, reviewId = reviewId.toLongOrNull() ?: 0L, mangaId = mangaId,
    userId = userId, username = username,
    replyText = replyText, timestamp = timestamp
)