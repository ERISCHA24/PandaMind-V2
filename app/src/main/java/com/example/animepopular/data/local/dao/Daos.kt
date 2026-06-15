package com.example.animepopular.data.local.dao

import androidx.room.*
import com.example.animepopular.data.local.entity.*
import kotlinx.coroutines.flow.Flow

// ── Manga Cache DAO ───────────────────────────────────────────────────────────

@Dao
interface MangaDao {
    @Query("SELECT * FROM manga_cache ORDER BY followsCount DESC")
    fun getAllManga(): Flow<List<MangaEntity>>

    @Query("SELECT * FROM manga_cache WHERE id = :id")
    suspend fun getMangaById(id: String): MangaEntity?

    @Query("SELECT * FROM manga_cache ORDER BY ratingAverage DESC LIMIT :limit")
    fun getTopRated(limit: Int = 20): Flow<List<MangaEntity>>

    @Query("SELECT * FROM manga_cache WHERE titleEn LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    fun searchManga(query: String): Flow<List<MangaEntity>>

    @Query("SELECT * FROM manga_cache WHERE tags LIKE '%' || :tag || '%'")
    fun getMangaByTag(tag: String): Flow<List<MangaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(manga: List<MangaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(manga: MangaEntity)

    @Query("DELETE FROM manga_cache WHERE cachedAt < :threshold")
    suspend fun deleteOldCache(threshold: Long)

    @Query("DELETE FROM manga_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM manga_cache")
    suspend fun count(): Int
}

// ── Favorites DAO ─────────────────────────────────────────────────────────────

@Dao
interface FavoriteDao {

    /** Semua favorit milik userId tertentu */
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY addedAt DESC")
    fun getAllFavorites(userId: String): Flow<List<FavoriteEntity>>

    /** Set mangaId yang difavoritkan userId — untuk tombol ❤ real-time */
    @Query("SELECT mangaId FROM favorites WHERE userId = :userId")
    fun getFavoriteIds(userId: String): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mangaId = :mangaId AND userId = :userId)")
    fun isFavoriteFlow(mangaId: String, userId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mangaId = :mangaId AND userId = :userId)")
    suspend fun isFavorite(mangaId: String, userId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mangaId = :mangaId AND userId = :userId")
    suspend fun delete(mangaId: String, userId: String)

    /** Hapus SEMUA data guest saat keluar guest mode */
    @Query("DELETE FROM favorites WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT COUNT(*) FROM favorites WHERE userId = :userId")
    fun getFavoriteCount(userId: String): Flow<Int>
}

// ── Watched DAO ───────────────────────────────────────────────────────────────

@Dao
interface WatchedDao {

    @Query("SELECT * FROM watched WHERE userId = :userId ORDER BY markedAt DESC")
    fun getAllWatched(userId: String): Flow<List<WatchedEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watched WHERE mangaId = :mangaId AND userId = :userId)")
    fun isWatchedFlow(mangaId: String, userId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM watched WHERE mangaId = :mangaId AND userId = :userId)")
    suspend fun isWatched(mangaId: String, userId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(watched: WatchedEntity)

    @Query("DELETE FROM watched WHERE mangaId = :mangaId AND userId = :userId")
    suspend fun delete(mangaId: String, userId: String)

    @Query("DELETE FROM watched WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT COUNT(*) FROM watched WHERE userId = :userId")
    fun getWatchedCount(userId: String): Flow<Int>
}

// ── Reviews DAO ───────────────────────────────────────────────────────────────

@Dao
interface ReviewDao {

    /** Semua review untuk manga tertentu — tampil untuk semua user (publik) */
    @Query("SELECT * FROM reviews WHERE mangaId = :mangaId ORDER BY timestamp DESC")
    fun getReviewsForManga(mangaId: String): Flow<List<ReviewEntity>>

    @Query("SELECT COUNT(*) FROM reviews WHERE mangaId = :mangaId")
    fun getReviewCount(mangaId: String): Flow<Int>

    @Insert
    suspend fun insert(review: ReviewEntity): Long

    @Query("SELECT * FROM reviews WHERE id = :id")
    suspend fun getById(id: Long): ReviewEntity?

    @Update
    suspend fun update(review: ReviewEntity)

    @Delete
    suspend fun delete(review: ReviewEntity)

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM reviews WHERE userId = :userId")
    fun getTotalReviewCount(userId: String): Flow<Int>

    @Query("DELETE FROM reviews WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

// ── Cache Metadata DAO ────────────────────────────────────────────────────────

@Dao
interface CacheMetadataDao {
    @Query("SELECT * FROM cache_metadata WHERE cacheKey = :key")
    suspend fun getMetadata(key: String): CacheMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(metadata: CacheMetadataEntity)

    @Query("DELETE FROM cache_metadata WHERE cacheKey = :key")
    suspend fun delete(key: String)
}

// ── Reading Progress DAO ──────────────────────────────────────────────────────

@Dao
interface ReadingProgressDao {

    @Query("SELECT * FROM reading_progress WHERE chapterId = :chapterId AND userId = :userId")
    suspend fun getProgress(chapterId: String, userId: String): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress WHERE mangaId = :mangaId AND userId = :userId ORDER BY lastRead DESC")
    fun getMangaProgress(mangaId: String, userId: String): Flow<List<ReadingProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE chapterId = :chapterId AND userId = :userId")
    suspend fun deleteProgress(chapterId: String, userId: String)

    @Query("DELETE FROM reading_progress WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

// ── Reading History DAO ───────────────────────────────────────────────────────
// ✅ NEW

@Dao
interface ReadingHistoryDao {

    /** Semua history baca user, diurutkan terbaru */
    @Query("SELECT * FROM reading_history WHERE userId = :userId ORDER BY lastReadAt DESC")
    fun getHistory(userId: String): Flow<List<ReadingHistoryEntity>>

    @Query("SELECT * FROM reading_history WHERE mangaId = :mangaId AND userId = :userId")
    suspend fun getHistoryEntry(mangaId: String, userId: String): ReadingHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: ReadingHistoryEntity)

    @Query("DELETE FROM reading_history WHERE mangaId = :mangaId AND userId = :userId")
    suspend fun delete(mangaId: String, userId: String)

    /** Hapus semua history satu user (untuk clear history atau hapus guest data) */
    @Query("DELETE FROM reading_history WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT COUNT(*) FROM reading_history WHERE userId = :userId")
    fun getHistoryCount(userId: String): Flow<Int>
}

// ── Review Reply DAO ──────────────────────────────────────────────────────────

@Dao
interface ReviewReplyDao {

    @Query("SELECT * FROM review_replies WHERE reviewId = :reviewId ORDER BY timestamp ASC")
    fun getRepliesForReview(reviewId: Long): Flow<List<ReviewReplyEntity>>

    @Insert
    suspend fun insert(reply: ReviewReplyEntity): Long

    @Query("DELETE FROM review_replies WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM review_replies WHERE reviewId = :reviewId")
    suspend fun deleteByReviewId(reviewId: Long)

    @Query("DELETE FROM review_replies WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}