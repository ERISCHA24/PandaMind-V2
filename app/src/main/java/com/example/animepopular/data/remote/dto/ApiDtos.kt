@file:OptIn(
    kotlinx.serialization.InternalSerializationApi::class,
    kotlinx.serialization.ExperimentalSerializationApi::class
)

package com.example.animepopular.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Generic API Response Wrapper ──────────────────────────────────────────────

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String, val details: String? = null) : ApiResult<Nothing>()
    data object Loading : ApiResult<Nothing>()
    data object Empty : ApiResult<Nothing>()
}

fun <T> ApiResult<T>.isSuccess() = this is ApiResult.Success
fun <T> ApiResult<T>.isError() = this is ApiResult.Error
fun <T> ApiResult<T>.getOrNull() = (this as? ApiResult.Success)?.data
fun <T> ApiResult<T>.errorMessage() = (this as? ApiResult.Error)?.message ?: ""

// ── MangaDex Generic List Response ───────────────────────────────────────────

@Serializable
data class MangaDexListResponse<T>(
    val result: String = "ok",
    val response: String = "collection",
    val data: List<T> = emptyList(),
    val limit: Int = 20,
    val offset: Int = 0,
    val total: Int = 0
)

@Serializable
data class MangaDexSingleResponse<T>(
    val result: String = "ok",
    val response: String = "entity",
    val data: T
)

// ── Error Response ─────────────────────────────────────────────────────────────

@Serializable
data class MangaDexErrorResponse(
    val result: String = "error",
    val errors: List<MangaDexError> = emptyList()
)

@Serializable
data class MangaDexError(
    val id: String = "",
    val status: Int = 0,
    val title: String = "",
    val detail: String? = null,
    val context: String? = null
)

// ── Auth DTOs ─────────────────────────────────────────────────────────────────

@Serializable
data class AuthTokenRequest(
    @SerialName("grant_type") val grantType: String = "password",
    val username: String,
    val password: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String
)

@Serializable
data class AuthTokenResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("expires_in") val expiresIn: Int = 0,
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("token_type") val tokenType: String = "Bearer",
    val scope: String = "",
    @SerialName("session_token") val sessionToken: String? = null
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("grant_type") val grantType: String = "refresh_token",
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String
)

// ── Manga DTOs ────────────────────────────────────────────────────────────────

@Serializable
data class MangaDto(
    val id: String = "",
    val type: String = "manga",
    val attributes: MangaAttributes = MangaAttributes(),
    val relationships: List<Relationship> = emptyList()
)

@Serializable
data class MangaAttributes(
    val title: Map<String, String> = emptyMap(),
    val altTitles: List<Map<String, String>> = emptyList(),
    val description: Map<String, String> = emptyMap(),
    val status: String? = null,
    val year: Int? = null,
    val contentRating: String? = null,
    val tags: List<TagDto> = emptyList(),
    val state: String? = null,
    @SerialName("createdAt") val createdAt: String = "",
    @SerialName("updatedAt") val updatedAt: String = "",
    @SerialName("availableTranslatedLanguages") val availableTranslatedLanguages: List<String> = emptyList(),
    val latestUploadedChapter: String? = null
)

@Serializable
data class TagDto(
    val id: String = "",
    val type: String = "tag",
    val attributes: TagAttributes = TagAttributes()
)

@Serializable
data class TagAttributes(
    val name: Map<String, String> = emptyMap(),
    val group: String = ""
)

@Serializable
data class Relationship(
    val id: String = "",
    val type: String = "",
    val attributes: RelationshipAttributes? = null
)

@Serializable
data class RelationshipAttributes(
    val fileName: String? = null,
    val name: String? = null,
    val description: String? = null
)

// ── Cover DTO ──────────────────────────────────────────────────────────────────

@Serializable
data class CoverDto(
    val id: String = "",
    val type: String = "cover_art",
    val attributes: CoverAttributes = CoverAttributes()
)

@Serializable
data class CoverAttributes(
    val fileName: String = "",
    val volume: String? = null,
    @SerialName("createdAt") val createdAt: String = "",
    @SerialName("updatedAt") val updatedAt: String = ""
)

// ── Rating DTOs ──────────────────────────────────────────────────────────────

@Serializable
data class RatingResponse(
    val result: String = "ok",
    val ratings: Map<String, RatingInfo> = emptyMap()
)

@Serializable
data class RatingInfo(
    val rating: Double = 0.0,
    val bayesian: Double = 0.0
)

// ── Statistics Response ───────────────────────────────────────────────────────

@Serializable
data class StatisticsResponse(
    val result: String = "ok",
    val statistics: Map<String, MangaStatistics> = emptyMap()
)

@Serializable
data class MangaStatistics(
    val rating: RatingDistribution = RatingDistribution(),
    val follows: Int = 0,
    val comments: CommentStats? = null
)

@Serializable
data class RatingDistribution(
    val average: Double? = null,
    val bayesian: Double? = null,
    val distribution: Map<String, Int> = emptyMap()
)

@Serializable
data class CommentStats(
    val threadId: Int? = null,
    val repliesCount: Int = 0
)

// ── Chapter DTO ───────────────────────────────────────────────────────────────

@Serializable
data class ChapterDto(
    val id: String = "",
    val type: String = "chapter",
    val attributes: ChapterAttributes = ChapterAttributes()
)

@Serializable
data class ChapterAttributes(
    val title: String? = null,
    val volume: String? = null,
    val chapter: String? = null,
    val translatedLanguage: String = "en",
    val pages: Int = 0,
    @SerialName("publishAt") val publishAt: String = "",
    @SerialName("createdAt") val createdAt: String = ""
)

// ── Chapter Pages / AtHome ────────────────────────────────────────────────────

@Serializable
data class AtHomeServerResponse(
    val result: String = "ok",
    val baseUrl: String = "",
    val chapter: AtHomeChapterData = AtHomeChapterData()
)

@Serializable
data class AtHomeChapterData(
    val hash: String = "",
    val data: List<String> = emptyList(),         // high quality
    val dataSaver: List<String> = emptyList()     // compressed
)

// ── Reading Progress Entity ────────────────────────────────────────────────────

@Serializable
data class ReadingProgressDto(
    val mangaId: String,
    val chapterId: String,
    val currentPage: Int,
    val totalPages: Int,
    val lastRead: Long = System.currentTimeMillis()
)