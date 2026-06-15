package com.example.animepopular.model

// ── Manga Domain Model ─────────────────────────────────────────────────────────

data class Manga(
    val id: String,
    val title: String,
    val titleJa: String,
    val description: String,
    val status: String,
    val year: Int?,
    val contentRating: String,
    val coverUrl: String,
    val tags: List<String>,
    val authorName: String,
    val followsCount: Int,
    val rating: Double,
    val isFavorite: Boolean = false,
    val isWatched: Boolean = false
)

// ── Review Domain Model ────────────────────────────────────────────────────────

data class Review(
    val id: Long = 0,
    val mangaId: String,
    val userId: String = "",       // ✅ NEW
    val username: String,
    val reviewText: String,
    val rating: Float,
    val timestamp: Long,
    val imagePaths: List<String> = emptyList(),
    val gifPath: String? = null
)

// ── Tag Domain Model ───────────────────────────────────────────────────────────

data class Tag(
    val id: String,
    val name: String,
    val group: String
)

// ── News ───────────────────────────────────────────────────────────────────────

data class NewsItem(
    val id: String,
    val titleEn: String,
    val titleId: String,
    val contentEn: String,
    val contentId: String,
    val updateEn: String,
    val updateId: String
)

// ── Schedule ───────────────────────────────────────────────────────────────────

data class ScheduleItem(
    val dayEn: String,
    val dayId: String,
    val animeListEn: String,
    val animeListId: String
)

// ── Auth ───────────────────────────────────────────────────────────────────────

data class AuthState(
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val accessToken: String = ""
)

// ── Chapter ────────────────────────────────────────────────────────────────────

data class Chapter(
    val id: String,
    val title: String?,
    val chapter: String?,
    val volume: String?,
    val pages: Int,
    val publishedAt: String
)

// ── Chapter Page Model ─────────────────────────────────────────────────────────

data class ChapterPages(
    val chapterId: String,
    val baseUrl: String,
    val hash: String,
    val pageUrls: List<String>,
    val pageUrlsSaver: List<String>,
    val totalPages: Int
) {
    fun getPageUrl(index: Int, dataSaver: Boolean = false): String {
        val list = if (dataSaver) pageUrlsSaver else pageUrls
        if (list.isEmpty() || index >= list.size) return ""
        val quality = if (dataSaver) "data-saver" else "data"
        return "$baseUrl/$quality/$hash/${list[index]}"
    }
}

// ── Reading Progress ───────────────────────────────────────────────────────────

data class ReadingProgress(
    val chapterId: String,
    val mangaId: String,
    val currentPage: Int,
    val totalPages: Int,
    val lastRead: Long
)

// ── Reading History ────────────────────────────────────────────────────────────
// ✅ NEW

data class ReadingHistory(
    val mangaId: String,
    val title: String,
    val coverUrl: String,
    val lastChapterId: String,
    val lastChapterTitle: String,
    val totalChaptersRead: Int,
    val lastReadAt: Long
)

// ── Reply (linked to Review) ──────────────────────────────────────────────────

data class ReviewReply(
    val id: Long = 0,
    val reviewId: Long,
    val mangaId: String,
    val userId: String = "",       // ✅ NEW
    val username: String,
    val replyText: String,
    val timestamp: Long = System.currentTimeMillis()
)