package com.example.animepopular.data.repository

import com.example.animepopular.data.local.dao.ReadingHistoryDao
import com.example.animepopular.data.local.dao.ReadingProgressDao
import com.example.animepopular.data.local.entity.ReadingHistoryEntity
import com.example.animepopular.data.local.entity.ReadingProgressEntity
import com.example.animepopular.data.remote.api.MangaDexApiService
import com.example.animepopular.data.remote.api.MangaDexAtHomeService
import com.example.animepopular.data.remote.dto.ApiResult
import com.example.animepopular.model.Chapter
import com.example.animepopular.model.ChapterPages
import com.example.animepopular.model.ReadingHistory
import com.example.animepopular.model.ReadingProgress
import com.example.animepopular.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

class ChapterRepository(
    private val apiService: MangaDexApiService,
    private val atHomeService: MangaDexAtHomeService,
    private val readingProgressDao: ReadingProgressDao,
    private val readingHistoryDao: ReadingHistoryDao    // ✅ NEW
) : BaseRepository() {

    // ── Chapter List ──────────────────────────────────────────────────────────

    suspend fun getChapterList(
        mangaId: String,
        limit: Int = 100,
        offset: Int = 0,
        lang: List<String> = listOf("en")
    ): ApiResult<List<Chapter>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMangaChapters(
                id = mangaId, limit = limit, offset = offset,
                lang = lang, order = "asc"
            )
            if (response.isSuccessful) {
                val chapters = response.body()?.data?.map { it.toDomain() } ?: emptyList()
                ApiResult.Success(chapters)
            } else {
                ApiResult.Error(response.code(), "Failed to load chapters (${response.code()})")
            }
        } catch (e: Exception) {
            ApiResult.Error(-1, e.message ?: "Network error")
        }
    }

    // ── Chapter Pages ─────────────────────────────────────────────────────────

    suspend fun getChapterPages(chapterId: String): ApiResult<ChapterPages> =
        withContext(Dispatchers.IO) {
            try {
                val response = atHomeService.getChapterPages(chapterId)
                if (response.isSuccessful) {
                    val body = response.body()
                        ?: return@withContext ApiResult.Error(-1, "Empty response")
                    val pages = ChapterPages(
                        chapterId = chapterId, baseUrl = body.baseUrl,
                        hash = body.chapter.hash, pageUrls = body.chapter.data,
                        pageUrlsSaver = body.chapter.dataSaver,
                        totalPages = body.chapter.data.size
                    )
                    ApiResult.Success(pages)
                } else {
                    ApiResult.Error(response.code(), "Failed to load chapter pages")
                }
            } catch (e: Exception) {
                ApiResult.Error(-1, e.message ?: "Network error")
            }
        }

    // ── Reading Progress ──────────────────────────────────────────────────────

    suspend fun saveProgress(
        chapterId: String,
        mangaId: String,
        currentPage: Int,
        totalPages: Int,
        userId: String = Constants.GUEST_USER_ID
    ) = withContext(Dispatchers.IO) {
        readingProgressDao.saveProgress(
            ReadingProgressEntity(
                chapterId = chapterId, userId = userId,
                mangaId = mangaId, currentPage = currentPage,
                totalPages = totalPages, lastRead = System.currentTimeMillis()
            )
        )
    }

    suspend fun getProgress(
        chapterId: String,
        userId: String = Constants.GUEST_USER_ID
    ): ReadingProgress? = withContext(Dispatchers.IO) {
        readingProgressDao.getProgress(chapterId, userId)?.let {
            ReadingProgress(
                chapterId = it.chapterId, mangaId = it.mangaId,
                currentPage = it.currentPage, totalPages = it.totalPages,
                lastRead = it.lastRead
            )
        }
    }

    fun getMangaProgress(
        mangaId: String,
        userId: String = Constants.GUEST_USER_ID
    ): Flow<List<ReadingProgress>> =
        readingProgressDao.getMangaProgress(mangaId, userId).map { list ->
            list.map {
                ReadingProgress(
                    chapterId = it.chapterId, mangaId = it.mangaId,
                    currentPage = it.currentPage, totalPages = it.totalPages,
                    lastRead = it.lastRead
                )
            }
        }

    // ── Reading History ───────────────────────────────────────────────────────

    /**
     * Tambahkan / update entry history ketika user membuka sebuah chapter.
     * Jika guest → tidak simpan history (data akan hilang).
     */
    suspend fun addToHistory(
        mangaId: String,
        mangaTitle: String,
        coverFileName: String,
        chapterId: String,
        chapterTitle: String,
        userId: String
    ) = withContext(Dispatchers.IO) {
        // Guest tidak punya history
        if (userId == Constants.GUEST_USER_ID) return@withContext

        val existing = readingHistoryDao.getHistoryEntry(mangaId, userId)
        val newCount = (existing?.totalChaptersRead ?: 0) + 1

        readingHistoryDao.insertOrUpdate(
            ReadingHistoryEntity(
                mangaId = mangaId, userId = userId,
                title = mangaTitle, coverFileName = coverFileName,
                lastChapterId = chapterId, lastChapterTitle = chapterTitle,
                totalChaptersRead = newCount,
                lastReadAt = System.currentTimeMillis()
            )
        )
        Timber.d("[$userId] History updated: $mangaTitle → $chapterTitle")
    }

    fun getHistory(userId: String): Flow<List<ReadingHistory>> =
        readingHistoryDao.getHistory(userId).map { list ->
            list.map { h ->
                ReadingHistory(
                    mangaId = h.mangaId,
                    title = h.title,
                    coverUrl = buildCoverUrl(h.mangaId, h.coverFileName),
                    lastChapterId = h.lastChapterId,
                    lastChapterTitle = h.lastChapterTitle,
                    totalChaptersRead = h.totalChaptersRead,
                    lastReadAt = h.lastReadAt
                )
            }
        }

    fun getHistoryCount(userId: String): Flow<Int> =
        readingHistoryDao.getHistoryCount(userId)

    suspend fun removeFromHistory(mangaId: String, userId: String) =
        withContext(Dispatchers.IO) {
            readingHistoryDao.delete(mangaId, userId)
        }

    suspend fun clearHistory(userId: String) = withContext(Dispatchers.IO) {
        readingHistoryDao.deleteAllForUser(userId)
        readingProgressDao.deleteAllForUser(userId)
        Timber.d("[$userId] History & progress cleared")
    }

    /** Hapus semua data guest (dipanggil saat keluar guest / login) */
    suspend fun clearGuestData() = withContext(Dispatchers.IO) {
        val guestId = Constants.GUEST_USER_ID
        readingHistoryDao.deleteAllForUser(guestId)
        readingProgressDao.deleteAllForUser(guestId)
        Timber.d("Guest chapter data cleared")
    }

    private fun buildCoverUrl(mangaId: String, fileName: String): String {
        if (fileName.isBlank()) return ""
        return "https://uploads.mangadex.org/covers/$mangaId/$fileName.512.jpg"
    }
}