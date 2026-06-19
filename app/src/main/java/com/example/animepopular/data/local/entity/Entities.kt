package com.example.animepopular.data.local.entity

import androidx.room.*

// ── Manga Entity (cached from API — tidak butuh userId, cache global) ──────────

@Entity(tableName = "manga_cache")
data class MangaEntity(
    @PrimaryKey val id: String,
    val titleEn: String,
    val titleJa: String,
    val descriptionEn: String,
    val status: String,
    val year: Int?,
    val contentRating: String,
    val coverFileName: String,
    val tags: String,           // JSON string of tag names
    val authorName: String,
    val followsCount: Int,
    val ratingAverage: Double,
    val cachedAt: Long = System.currentTimeMillis()
)

// ── Favorite Entity ───────────────────────────────────────────────────────────
// userId membedakan favorit antar user. Guest = "__guest__"

@Entity(
    tableName = "favorites",
    primaryKeys = ["mangaId", "userId"]
)
data class FavoriteEntity(
    val mangaId: String,
    val userId: String,
    val title: String,
    val coverFileName: String,
    val status: String,
    val rating: Double,
    // ✅ Field baru: deskripsi manga untuk ditampilkan di halaman favorit
    val description: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

// ── Watched/Read Entity ────────────────────────────────────────────────────────

@Entity(
    tableName = "watched",
    primaryKeys = ["mangaId", "userId"]
)
data class WatchedEntity(
    val mangaId: String,
    val userId: String,
    val title: String,
    val markedAt: Long = System.currentTimeMillis()
)

// ── Review Entity ─────────────────────────────────────────────────────────────

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mangaId: String,
    val userId: String,
    val username: String,
    val reviewText: String,
    val rating: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val imagePaths: String = "",
    val gifPath: String? = null
)

// ── Cache Metadata ─────────────────────────────────────────────────────────────

@Entity(tableName = "cache_metadata")
data class CacheMetadataEntity(
    @PrimaryKey val cacheKey: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val totalCount: Int = 0
)

// ── Reading Progress Entity ───────────────────────────────────────────────────

@Entity(
    tableName = "reading_progress",
    primaryKeys = ["chapterId", "userId"]
)
data class ReadingProgressEntity(
    val chapterId: String,
    val userId: String,
    val mangaId: String,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val lastRead: Long = System.currentTimeMillis()
)

// ── Reading History Entity ────────────────────────────────────────────────────

@Entity(
    tableName = "reading_history",
    primaryKeys = ["mangaId", "userId"]
)
data class ReadingHistoryEntity(
    val mangaId: String,
    val userId: String,
    val title: String,
    val coverFileName: String,
    val lastChapterId: String = "",
    val lastChapterTitle: String = "",
    val totalChaptersRead: Int = 0,
    val lastReadAt: Long = System.currentTimeMillis()
)

// ── Review Reply Entity ───────────────────────────────────────────────────────

@Entity(tableName = "review_replies")
data class ReviewReplyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reviewId: Long,
    val mangaId: String,
    val userId: String,
    val username: String,
    val replyText: String,
    val timestamp: Long = System.currentTimeMillis()
)