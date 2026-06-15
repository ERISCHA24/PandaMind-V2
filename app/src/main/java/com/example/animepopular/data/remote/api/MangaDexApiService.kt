package com.example.animepopular.data.remote.api

import com.example.animepopular.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface MangaDexApiService {

    // ── Manga ─────────────────────────────────────────────────────────────────

    @GET("manga")
    suspend fun getMangaList(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("order[followedCount]") followedCount: String = "desc",
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author", "artist"),
        @Query("contentRating[]") contentRating: List<String> = listOf("safe", "suggestive"),
        @Query("availableTranslatedLanguage[]") lang: List<String> = listOf("en")
    ): Response<MangaDexListResponse<MangaDto>>

    @GET("manga")
    suspend fun searchManga(
        @Query("title") title: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author"),
        @Query("contentRating[]") contentRating: List<String> = listOf("safe", "suggestive"),
        @Query("order[relevance]") order: String = "desc"
    ): Response<MangaDexListResponse<MangaDto>>

    @GET("manga")
    suspend fun getMangaByGenre(
        @Query("includedTags[]") tagIds: List<String>,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author"),
        @Query("contentRating[]") contentRating: List<String> = listOf("safe", "suggestive"),
        @Query("order[followedCount]") order: String = "desc"
    ): Response<MangaDexListResponse<MangaDto>>

    @GET("manga/{id}")
    suspend fun getMangaById(
        @Path("id") id: String,
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author", "artist")
    ): Response<MangaDexSingleResponse<MangaDto>>

    // ── Top/Rated ─────────────────────────────────────────────────────────────

    @GET("manga")
    suspend fun getTopRatedManga(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("order[rating]") rating: String = "desc",
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author"),
        @Query("contentRating[]") contentRating: List<String> = listOf("safe", "suggestive")
    ): Response<MangaDexListResponse<MangaDto>>

    // ── Tags ──────────────────────────────────────────────────────────────────

    @GET("manga/tag")
    suspend fun getAllTags(): Response<MangaDexListResponse<TagDto>>

    // ── Cover Art ──────────────────────────────────────────────────────────────

    @GET("cover/{coverId}")
    suspend fun getCoverArt(
        @Path("coverId") coverId: String
    ): Response<MangaDexSingleResponse<CoverDto>>

    // ── Statistics ─────────────────────────────────────────────────────────────

    @GET("statistics/manga")
    suspend fun getMangaStatistics(
        @Query("manga[]") mangaIds: List<String>
    ): Response<StatisticsResponse>

    @GET("statistics/manga/{id}")
    suspend fun getSingleMangaStatistics(
        @Path("id") id: String
    ): Response<MangaDexSingleResponse<MangaStatistics>>

    // ── Chapters ───────────────────────────────────────────────────────────────

    @GET("manga/{id}/feed")
    suspend fun getMangaChapters(
        @Path("id") id: String,
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0,
        @Query("translatedLanguage[]") lang: List<String> = listOf("en"),
        @Query("order[chapter]") order: String = "desc"
    ): Response<MangaDexListResponse<ChapterDto>>

    // ── Recently Updated ───────────────────────────────────────────────────────

    @GET("manga")
    suspend fun getRecentlyUpdated(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("order[latestUploadedChapter]") order: String = "desc",
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author"),
        @Query("contentRating[]") contentRating: List<String> = listOf("safe", "suggestive")
    ): Response<MangaDexListResponse<MangaDto>>
}

interface MangaDexAuthService {

    @POST("realms/mangadex/protocol/openid-connect/token")
    @FormUrlEncoded
    suspend fun login(
        @Field("grant_type") grantType: String = "password",
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): Response<AuthTokenResponse>

    @POST("realms/mangadex/protocol/openid-connect/token")
    @FormUrlEncoded
    suspend fun refreshToken(
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): Response<AuthTokenResponse>
}

interface MangaDexAtHomeService {
    // Get image server URL + page list for a chapter
    @GET("at-home/server/{chapterId}")
    suspend fun getChapterPages(
        @Path("chapterId") chapterId: String,
        @Query("forcePort443") forcePort443: Boolean = false
    ): Response<AtHomeServerResponse>
}