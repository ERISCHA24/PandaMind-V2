package com.example.animepopular.data.local.db

import android.content.Context
import androidx.room.*
import com.example.animepopular.data.local.dao.*
import com.example.animepopular.data.local.entity.*

@Database(
    entities = [
        MangaEntity::class,
        FavoriteEntity::class,
        WatchedEntity::class,
        ReviewEntity::class,
        CacheMetadataEntity::class,
        ReadingProgressEntity::class,
        ReadingHistoryEntity::class,
        ReviewReplyEntity::class
    ],
    version = 6,   // ✅ bump: MangaEntity.availableLanguages ditambahkan
    exportSchema = false
)
abstract class AnimeDatabase : RoomDatabase() {

    abstract fun mangaDao(): MangaDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun watchedDao(): WatchedDao
    abstract fun reviewDao(): ReviewDao
    abstract fun cacheMetadataDao(): CacheMetadataDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun readingHistoryDao(): ReadingHistoryDao
    abstract fun reviewReplyDao(): ReviewReplyDao

    companion object {
        @Volatile private var INSTANCE: AnimeDatabase? = null

        fun getInstance(context: Context): AnimeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnimeDatabase::class.java,
                    "anime_popular_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}