package com.example.animepopular.data.repository

import android.content.Context
import android.net.Uri
import com.example.animepopular.model.Review
import com.example.animepopular.model.ReviewReply
import com.example.animepopular.util.ImageUtil
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID

class ReviewRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    companion object {
        private const val REVIEWS = "reviews"
        private const val REPLIES = "replies"
        private const val MEDIA_PATH = "review_media"
    }

    // ── Real-time listeners ───────────────────────────────────────────────────

    fun getReviewsForManga(mangaId: String): Flow<List<Review>> = callbackFlow {
        val listener = firestore.collection(REVIEWS)
            .whereEqualTo(FIELD_MANGA_ID, mangaId)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Failed to listen reviews for manga=$mangaId")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val reviews = snapshot?.documents?.mapNotNull { it.toReview() } ?: emptyList()
                trySend(reviews)
            }
        awaitClose { listener.remove() }
    }

    fun getRepliesForReview(reviewId: String): Flow<List<ReviewReply>> = callbackFlow {
        val listener = firestore.collection(REPLIES)
            .whereEqualTo(FIELD_REVIEW_ID, reviewId)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Failed to listen replies for review=$reviewId")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val replies = snapshot?.documents?.mapNotNull { it.toReply() } ?: emptyList()
                trySend(replies)
            }
        awaitClose { listener.remove() }
    }

    fun getTotalReviewCount(userId: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection(REVIEWS)
            .whereEqualTo(FIELD_USER_ID, userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Failed to count reviews for user=$userId")
                    trySend(0)
                    return@addSnapshotListener
                }
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    fun getReviewCount(mangaId: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection(REVIEWS)
            .whereEqualTo(FIELD_MANGA_ID, mangaId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(0)
                    return@addSnapshotListener
                }
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    // ── Write operations ──────────────────────────────────────────────────────

    suspend fun addReview(
        context: Context,
        review: Review,
        imageUris: List<Uri> = emptyList(),
        gifUri: Uri? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val imageUrls = imageUris.mapNotNull { uploadMedia(context, it, review.mangaId, false) }
            val gifUrl = gifUri?.let { uploadMedia(context, it, review.mangaId, true) }

            val data = hashMapOf(
                FIELD_MANGA_ID to review.mangaId,
                FIELD_USER_ID to review.userId,
                FIELD_USERNAME to review.username,
                FIELD_REVIEW_TEXT to review.reviewText,
                FIELD_RATING to review.rating.toDouble(),
                FIELD_TIMESTAMP to review.timestamp,
                FIELD_IMAGE_URLS to imageUrls,
                FIELD_GIF_URL to gifUrl
            )
            firestore.collection(REVIEWS).add(data).await()
            Timber.d("Global review posted for manga=${review.mangaId}")
            Unit
        }
    }

    suspend fun editReview(
        context: Context,
        reviewId: String,
        mangaId: String,
        newText: String,
        newRating: Float,
        existingImageUrls: List<String>,
        newImageUris: List<Uri>,
        newGifUri: Uri?,
        existingGifUrl: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val newUrls = newImageUris.mapNotNull { uploadMedia(context, it, mangaId, false) }
            val allImages = existingImageUrls + newUrls
            val gifUrl = when {
                newGifUri != null -> uploadMedia(context, newGifUri, mangaId, true)
                else -> existingGifUrl
            }

            firestore.collection(REVIEWS).document(reviewId).update(
                mapOf(
                    FIELD_REVIEW_TEXT to newText,
                    FIELD_RATING to newRating.toDouble(),
                    FIELD_TIMESTAMP to System.currentTimeMillis(),
                    FIELD_IMAGE_URLS to allImages,
                    FIELD_GIF_URL to gifUrl
                )
            ).await()
            Unit
        }
    }

    suspend fun deleteReview(reviewId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val replies = firestore.collection(REPLIES)
                .whereEqualTo(FIELD_REVIEW_ID, reviewId)
                .get()
                .await()
            val batch = firestore.batch()
            replies.documents.forEach { batch.delete(it.reference) }
            batch.delete(firestore.collection(REVIEWS).document(reviewId))
            batch.commit().await()
            Unit
        }
    }

    suspend fun addReply(reply: ReviewReply): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            firestore.collection(REPLIES).add(
                hashMapOf(
                    FIELD_REVIEW_ID to reply.reviewId,
                    FIELD_MANGA_ID to reply.mangaId,
                    FIELD_USER_ID to reply.userId,
                    FIELD_USERNAME to reply.username,
                    FIELD_REPLY_TEXT to reply.replyText,
                    FIELD_TIMESTAMP to reply.timestamp
                )
            ).await()
            Unit
        }
    }

    suspend fun deleteReply(replyId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            firestore.collection(REPLIES).document(replyId).delete().await()
            Unit
        }
    }

    // ── Storage ───────────────────────────────────────────────────────────────

    private suspend fun uploadMedia(
        context: Context,
        uri: Uri,
        mangaId: String,
        isGif: Boolean
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val ext = if (isGif) "gif" else ImageUtil.getExtensionFromUri(context, uri)
            val path = "$MEDIA_PATH/$mangaId/${UUID.randomUUID()}.$ext"
            val ref = storage.reference.child(path)
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        }.onFailure { Timber.e(it, "Failed to upload review media") }
            .getOrNull()
    }

    // ── Mappers ─────────────────────────────────────────────────────────────────

    private fun com.google.firebase.firestore.DocumentSnapshot.toReview(): Review? {
        val id = id
        return Review(
            id = id,
            mangaId = getString(FIELD_MANGA_ID) ?: return null,
            userId = getString(FIELD_USER_ID) ?: "",
            username = getString(FIELD_USERNAME) ?: "Anonymous",
            reviewText = getString(FIELD_REVIEW_TEXT) ?: return null,
            rating = (getDouble(FIELD_RATING) ?: 0.0).toFloat(),
            timestamp = getLong(FIELD_TIMESTAMP) ?: 0L,
            imagePaths = (get(FIELD_IMAGE_URLS) as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            gifPath = getString(FIELD_GIF_URL)
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toReply(): ReviewReply? {
        return ReviewReply(
            id = id,
            reviewId = getString(FIELD_REVIEW_ID) ?: return null,
            mangaId = getString(FIELD_MANGA_ID) ?: "",
            userId = getString(FIELD_USER_ID) ?: "",
            username = getString(FIELD_USERNAME) ?: "Anonymous",
            replyText = getString(FIELD_REPLY_TEXT) ?: return null,
            timestamp = getLong(FIELD_TIMESTAMP) ?: System.currentTimeMillis()
        )
    }
}

private const val FIELD_MANGA_ID = "mangaId"
private const val FIELD_USER_ID = "userId"
private const val FIELD_USERNAME = "username"
private const val FIELD_REVIEW_TEXT = "reviewText"
private const val FIELD_REPLY_TEXT = "replyText"
private const val FIELD_RATING = "rating"
private const val FIELD_TIMESTAMP = "timestamp"
private const val FIELD_REVIEW_ID = "reviewId"
private const val FIELD_IMAGE_URLS = "imageUrls"
private const val FIELD_GIF_URL = "gifUrl"
