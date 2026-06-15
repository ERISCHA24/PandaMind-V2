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
        ReadingHistoryEntity::class,      // ✅ NEW
        ReviewReplyEntity::class
    ],
    version = 4,                          // ✅ bump version (schema berubah)
    exportSchema = false
)
abstract class AnimeDatabase : RoomDatabase() {

    abstract fun mangaDao(): MangaDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun watchedDao(): WatchedDao
    abstract fun reviewDao(): ReviewDao
    abstract fun cacheMetadataDao(): CacheMetadataDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun readingHistoryDao(): ReadingHistoryDao   // ✅ NEW
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
                    .fallbackToDestructiveMigration()  // hapus & rebuild saat version naik
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}