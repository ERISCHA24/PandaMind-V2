package com.example.animepopular.data.repository

import com.example.animepopular.data.local.dao.*
import com.example.animepopular.data.local.entity.*
import com.example.animepopular.data.preferences.AppPreferences
import com.example.animepopular.data.remote.api.MangaDexApiService
import com.example.animepopular.data.remote.dto.*
import com.example.animepopular.model.Manga
import com.example.animepopular.model.Chapter
import com.example.animepopular.model.Tag
import com.example.animepopular.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import timber.log.Timber

class MangaRepository(
    private val apiService: MangaDexApiService,
    private val mangaDao: MangaDao,
    private val favoriteDao: FavoriteDao,
    private val watchedDao: WatchedDao,
    private val cacheMetadataDao: CacheMetadataDao,
    private val preferences: AppPreferences          // ✅ NEW — needed for userId
) : BaseRepository() {

    // ── Caching Strategy ──────────────────────────────────────────────────────

    private suspend fun isCacheValid(key: String): Boolean {
        val meta = cacheMetadataDao.getMetadata(key)
        return meta != null && (System.currentTimeMillis() - meta.lastUpdated) < Constants.CACHE_DURATION_MS
    }

    private suspend fun updateCacheMetadata(key: String, total: Int) {
        cacheMetadataDao.insertOrUpdate(CacheMetadataEntity(key, totalCount = total))
    }

    // ── Popular / Home ─────────────────────────────────────────────────────────

    fun getPopularManga(): Flow<ApiResult<List<Manga>>> = flow {
        emit(ApiResult.Loading)
        val userId = preferences.getUserId()          // ✅

        // 1. Emit from cache
        val cached = mangaDao.getAllManga().first()
        if (cached.isNotEmpty()) {
            val favIds     = favoriteDao.getAllFavorites(userId).first().map { it.mangaId }.toSet()   // ✅
            val watchedIds = watchedDao.getAllWatched(userId).first().map { it.mangaId }.toSet()      // ✅
            emit(ApiResult.Success(cached.map { it.toDomain(it.id in favIds, it.id in watchedIds) }))
        }

        // 2. Fetch from API if stale
        if (!isCacheValid("home_list") || cached.isEmpty()) {
            try {
                val response = apiService.getMangaList(
                    limit    = Constants.MANGA_PAGE_SIZE,
                    includes = listOf("cover_art", "author")
                )
                if (response.isSuccessful) {
                    val mangaList = response.body()?.data ?: emptyList()
                    val ids       = mangaList.map { it.id }
                    val statsMap  = fetchStatistics(ids)
                    val entities  = mangaList.map { dto -> dto.toEntity(statsMap[dto.id]) }
                    mangaDao.insertAll(entities)
                    updateCacheMetadata("home_list", response.body()?.total ?: 0)
                    Timber.d("Fetched ${entities.size} popular manga from API")

                    val favIds     = favoriteDao.getAllFavorites(userId).first().map { it.mangaId }.toSet()  // ✅
                    val watchedIds = watchedDao.getAllWatched(userId).first().map { it.mangaId }.toSet()     // ✅
                    emit(ApiResult.Success(entities.map { it.toDomain(it.id in favIds, it.id in watchedIds) }))
                } else {
                    if (cached.isEmpty()) {
                        emit(ApiResult.Error(response.code(), "Failed to load manga"))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching popular manga")
                if (cached.isEmpty()) {
                    emit(ApiResult.Error(-1, e.message ?: "Network error"))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    // ── Top Rated ──────────────────────────────────────────────────────────────

    fun getTopRatedManga(): Flow<ApiResult<List<Manga>>> = flow {
        emit(ApiResult.Loading)
        val userId = preferences.getUserId()          // ✅

        val cached = mangaDao.getTopRated(20).first()
        if (cached.isNotEmpty()) {
            val favIds = favoriteDao.getAllFavorites(userId).first().map { it.mangaId }.toSet()  // ✅
            emit(ApiResult.Success(cached.map { it.toDomain(it.id in favIds) }))
        }

        if (!isCacheValid("top_rated_list") || cached.isEmpty()) {
            try {
                val response = apiService.getTopRatedManga(
                    limit    = Constants.MANGA_PAGE_SIZE,
                    includes = listOf("cover_art", "author")
                )
                if (response.isSuccessful) {
                    val mangaList = response.body()?.data ?: emptyList()
                    val statsMap  = fetchStatistics(mangaList.map { it.id })
                    val entities  = mangaList.map { it.toEntity(statsMap[it.id]) }
                    mangaDao.insertAll(entities)
                    updateCacheMetadata("top_rated_list", response.body()?.total ?: 0)

                    val favIds = favoriteDao.getAllFavorites(userId).first().map { it.mangaId }.toSet()  // ✅
                    emit(ApiResult.Success(entities.map { it.toDomain(it.id in favIds) }))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching top rated")
                if (cached.isEmpty()) emit(ApiResult.Error(-1, e.message ?: "Error"))
            }
        }
    }.flowOn(Dispatchers.IO)

    // ── Search ─────────────────────────────────────────────────────────────────

    fun searchManga(query: String): Flow<ApiResult<List<Manga>>> = flow {
        if (query.isBlank()) {
            emit(ApiResult.Success(emptyList()))
            return@flow
        }

        emit(ApiResult.Loading)
        val userId = preferences.getUserId()          // ✅

        // Local search first
        val localResults = mangaDao.searchManga(query).first()
        if (localResults.isNotEmpty()) {
            val favIds = favoriteDao.getAllFavorites(userId).first().map { it.mangaId }.toSet()  // ✅
            emit(ApiResult.Success(localResults.map { it.toDomain(it.id in favIds) }))
        }

        // Remote search
        try {
            val response = apiService.searchManga(
                title    = query,
                includes = listOf("cover_art", "author")
            )
            if (response.isSuccessful) {
                val mangaList = response.body()?.data ?: emptyList()
                val entities  = mangaList.map { it.toEntity() }
                mangaDao.insertAll(entities)

                val favIds = favoriteDao.getAllFavorites(userId).first().map { it.mangaId }.toSet()  // ✅
                emit(ApiResult.Success(entities.map { it.toDomain(it.id in favIds) }))
            } else {
                if (localResults.isEmpty()) emit(ApiResult.Error(response.code(), "Search failed"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Search error")
            if (localResults.isEmpty()) emit(ApiResult.Error(-1, e.message ?: "Error"))
        }
    }.flowOn(Dispatchers.IO)

    // ── By Genre/Tag ───────────────────────────────────────────────────────────

    fun getMangaByGenre(tagName: String): Flow<ApiResult<List<Manga>>> = flow {
        emit(ApiResult.Loading)
        val userId = preferences.getUserId()          // ✅

        val localResults = mangaDao.getMangaByTag(tagName).first()
        if (localResults.isNotEmpty()) {
            val favIds = favoriteDao.getAllFavorites(userId).first().map { it.mangaId }.toSet()  // ✅
            emit(ApiResult.Success(localResults.map { it.toDomain(it.id in favIds) }))
        }

        try {
            val tagsResponse = apiService.getAllTags()
            if (tagsResponse.isSuccessful) {
                val tagId = tagsResponse.body()?.data
                    ?.firstOrNull { it.attributes.name["en"]?.equals(tagName, true) == true }
                    ?.id

                if (tagId != null) {
                    val response = apiService.getMangaByGenre(
                        tagIds   = listOf(tagId),
                        includes = listOf("cover_art", "author")
                    )
                    if (response.isSuccessful) {
                        val mangaList = response.body()?.data ?: emptyList()
                        val entities  = mangaList.map { it.toEntity() }
                        mangaDao.insertAll(entities)
                        val favIds = favoriteDao.getAllFavorites(userId).first().map { it.mangaId }.toSet()  // ✅
                        emit(ApiResult.Success(entities.map { it.toDomain(it.id in favIds) }))
                    }
                } else {
                    if (localResults.isEmpty()) {
                        emit(ApiResult.Error(-1, "Tag not found"))
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Genre fetch error")
            if (localResults.isEmpty()) emit(ApiResult.Error(-1, e.message ?: "Error"))
        }
    }.flowOn(Dispatchers.IO)

    // ── Detail ─────────────────────────────────────────────────────────────────

    suspend fun getMangaById(id: String): ApiResult<Manga> = withContext(Dispatchers.IO) {
        val userId = preferences.getUserId()          // ✅
        val cached = mangaDao.getMangaById(id)
        if (cached != null) {
            val isFav     = favoriteDao.isFavorite(id, userId)   // ✅
            val isWatched = watchedDao.isWatched(id, userId)     // ✅
            return@withContext ApiResult.Success(cached.toDomain(isFav, isWatched))
        }
        try {
            val response = apiService.getMangaById(id, listOf("cover_art", "author", "artist"))
            if (response.isSuccessful) {
                val dto       = response.body()?.data ?: return@withContext ApiResult.Empty
                val entity    = dto.toEntity()
                mangaDao.insert(entity)
                val isFav     = favoriteDao.isFavorite(id, userId)   // ✅
                val isWatched = watchedDao.isWatched(id, userId)     // ✅
                ApiResult.Success(entity.toDomain(isFav, isWatched))
            } else {
                ApiResult.Error(response.code(), "Failed to fetch manga")
            }
        } catch (e: Exception) {
            ApiResult.Error(-1, e.message ?: "Error")
        }
    }

    // ── Tags ───────────────────────────────────────────────────────────────────

    suspend fun getAllTags(): ApiResult<List<Tag>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAllTags()
            if (response.isSuccessful) {
                val tags = response.body()?.data?.map { it.toDomain() } ?: emptyList()
                ApiResult.Success(tags)
            } else {
                ApiResult.Error(response.code(), "Failed to fetch tags")
            }
        } catch (e: Exception) {
            ApiResult.Error(-1, e.message ?: "Error")
        }
    }

    // ── Chapters ───────────────────────────────────────────────────────────────

    suspend fun getMangaChapters(mangaId: String): ApiResult<List<Chapter>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMangaChapters(mangaId, limit = 10)
            if (response.isSuccessful) {
                val chapters = response.body()?.data?.map { it.toDomain() } ?: emptyList()
                ApiResult.Success(chapters)
            } else {
                ApiResult.Error(response.code(), "Failed to fetch chapters")
            }
        } catch (e: Exception) {
            ApiResult.Error(-1, e.message ?: "Error")
        }
    }

    // ── Statistics Helper ──────────────────────────────────────────────────────

    private suspend fun fetchStatistics(ids: List<String>): Map<String, MangaStatistics> {
        return try {
            val response = apiService.getMangaStatistics(ids)
            if (response.isSuccessful) response.body()?.statistics ?: emptyMap()
            else emptyMap()
        } catch (e: Exception) {
            Timber.e(e, "Stats fetch error")
            emptyMap()
        }
    }
}