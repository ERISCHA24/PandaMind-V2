package com.example.animepopular.data.repository

import com.example.animepopular.data.local.entity.*
import com.example.animepopular.data.remote.dto.*
import com.example.animepopular.model.*
import com.example.animepopular.util.Constants
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val mapperJson = Json { ignoreUnknownKeys = true; isLenient = true }

// ── MangaDto → MangaEntity ─────────────────────────────────────────────────────

fun MangaDto.toCoverUrl(): String {
    val coverRel = relationships.firstOrNull { it.type == "cover_art" }
    val fileName = coverRel?.attributes?.fileName ?: return ""
    return "${Constants.MANGADEX_COVER_URL}$id/$fileName${Constants.COVER_THUMB_512}"
}

fun MangaDto.toAuthorName(): String {
    return relationships.firstOrNull { it.type == "author" || it.type == "artist" }
        ?.attributes?.name ?: "Unknown"
}

fun MangaDto.toEntity(stats: MangaStatistics? = null): MangaEntity {
    val title = attributes.title["en"]
        ?: attributes.title["ja-ro"]
        ?: attributes.title.values.firstOrNull()
        ?: "Unknown Title"
    val titleJa = attributes.title["ja"] ?: attributes.title["ja-ro"] ?: title
    val desc = attributes.description["en"]
        ?: attributes.description.values.firstOrNull()
        ?: ""
    val coverFileName = relationships.firstOrNull { it.type == "cover_art" }
        ?.attributes?.fileName ?: ""
    val tagNames = attributes.tags.map { it.attributes.name["en"] ?: "" }.filter { it.isNotBlank() }
    val authorName = toAuthorName()

    return MangaEntity(
        id = id,
        titleEn = title,
        titleJa = titleJa,
        descriptionEn = desc,
        status = attributes.status ?: "unknown",
        year = attributes.year,
        contentRating = attributes.contentRating ?: "safe",
        coverFileName = coverFileName,
        tags = mapperJson.encodeToString(tagNames),
        authorName = authorName,
        followsCount = stats?.follows ?: 0,
        ratingAverage = stats?.rating?.bayesian ?: stats?.rating?.average ?: 0.0
    )
}

// ── MangaEntity → Manga ────────────────────────────────────────────────────────

fun MangaEntity.toDomain(isFavorite: Boolean = false, isWatched: Boolean = false): Manga {
    val tagList = try {
        mapperJson.decodeFromString<List<String>>(tags)
    } catch (e: Exception) { emptyList() }
    val coverUrl = if (coverFileName.isNotBlank())
        "${Constants.MANGADEX_COVER_URL}$id/$coverFileName${Constants.COVER_THUMB_512}"
    else ""
    return Manga(
        id = id,
        title = titleEn,
        titleJa = titleJa,
        description = descriptionEn,
        status = status,
        year = year,
        contentRating = contentRating,
        coverUrl = coverUrl,
        tags = tagList,
        authorName = authorName,
        followsCount = followsCount,
        rating = ratingAverage,
        isFavorite = isFavorite,
        isWatched = isWatched
    )
}

// ── MangaEntity → FavoriteEntity ──────────────────────────────────────────────

fun MangaEntity.toFavoriteEntity() = FavoriteEntity(
    mangaId = id,
    title = titleEn,
    coverFileName = coverFileName,
    status = status,
    rating = ratingAverage
)

// ── FavoriteEntity → Manga ─────────────────────────────────────────────────────

fun FavoriteEntity.toManga() = Manga(
    id = mangaId,
    title = title,
    titleJa = title,
    description = "",
    status = status,
    year = null,
    contentRating = "safe",
    coverUrl = if (coverFileName.isNotBlank())
        "${Constants.MANGADEX_COVER_URL}$mangaId/$coverFileName${Constants.COVER_THUMB_512}"
    else "",
    tags = emptyList(),
    authorName = "",
    followsCount = 0,
    rating = rating,
    isFavorite = true
)

// ── ReviewEntity → Review ──────────────────────────────────────────────────────

fun ReviewEntity.toDomain(): Review {
    val paths = try {
        mapperJson.decodeFromString<List<String>>(imagePaths)
    } catch (e: Exception) { emptyList() }
    return Review(
        id = id,
        mangaId = mangaId,
        username = username,
        reviewText = reviewText,
        rating = rating,
        timestamp = timestamp,
        imagePaths = paths,
        gifPath = gifPath
    )
}

fun Review.toEntity() = ReviewEntity(
    id = id,
    mangaId = mangaId,
    username = username,
    reviewText = reviewText,
    rating = rating,
    timestamp = timestamp,
    imagePaths = mapperJson.encodeToString(imagePaths),
    gifPath = gifPath
)

// ── TagDto → Tag ───────────────────────────────────────────────────────────────

fun TagDto.toDomain() = Tag(
    id = id,
    name = attributes.name["en"] ?: attributes.name.values.firstOrNull() ?: "",
    group = attributes.group
)

// ── ChapterDto → Chapter ───────────────────────────────────────────────────────

fun ChapterDto.toDomain() = Chapter(
    id = id,
    title = attributes.title,
    chapter = attributes.chapter,
    volume = attributes.volume,
    pages = attributes.pages,
    publishedAt = attributes.publishAt.take(10)
)