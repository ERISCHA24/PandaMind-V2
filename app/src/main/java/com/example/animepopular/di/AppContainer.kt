package com.example.animepopular.di

import android.content.Context
import com.example.animepopular.data.local.db.AnimeDatabase
import com.example.animepopular.data.preferences.AppPreferences
import com.example.animepopular.data.remote.api.NetworkModule
import com.example.animepopular.data.repository.*
import com.example.animepopular.viewmodel.*

class AppContainer(context: Context) {

    // ── Preferences ───────────────────────────────────────────────────────────
    val preferences = AppPreferences(context)

    // ── Database ──────────────────────────────────────────────────────────────
    private val database = AnimeDatabase.getInstance(context)

    // ── Network ───────────────────────────────────────────────────────────────
    private val apiService    = NetworkModule.provideMangaDexApiService(preferences)
    private val authService   = NetworkModule.provideMangaDexAuthService()
    private val atHomeService = NetworkModule.provideMangaDexAtHomeService(preferences)

    // ── Repositories ──────────────────────────────────────────────────────────
    val mangaRepository = MangaRepository(
        apiService       = apiService,
        mangaDao         = database.mangaDao(),
        favoriteDao      = database.favoriteDao(),
        watchedDao       = database.watchedDao(),
        cacheMetadataDao = database.cacheMetadataDao(),
        preferences      = preferences               // ✅ NEW — required for userId in DAO calls
    )

    val favoritesRepository = FavoritesRepository(
        favoriteDao    = database.favoriteDao(),
        watchedDao     = database.watchedDao(),
        reviewDao      = database.reviewDao(),
        reviewReplyDao = database.reviewReplyDao()
    )

    val authRepository = AuthRepository(
        authService = authService,
        preferences = preferences
    )

    val chapterRepository = ChapterRepository(
        apiService         = apiService,
        atHomeService      = atHomeService,
        readingProgressDao = database.readingProgressDao(),
        readingHistoryDao  = database.readingHistoryDao()
    )

    // ── ViewModel Factories ───────────────────────────────────────────────────
    val homeViewModelFactory      = HomeViewModel.Factory(mangaRepository, favoritesRepository, preferences)
    val searchViewModelFactory    = SearchViewModel.Factory(mangaRepository, favoritesRepository, preferences)
    val detailViewModelFactory    = DetailViewModel.Factory(mangaRepository, favoritesRepository, preferences)
    val favoritesViewModelFactory = FavoritesViewModel.Factory(favoritesRepository, preferences)
    val topRatedViewModelFactory  = TopRatedViewModel.Factory(mangaRepository, favoritesRepository, preferences)
    val genreViewModelFactory     = GenreViewModel.Factory(mangaRepository, favoritesRepository, preferences)
    val profileViewModelFactory   = ProfileViewModel.Factory(preferences, favoritesRepository, authRepository, chapterRepository)
    val readerViewModelFactory    = ReaderViewModel.Factory(chapterRepository, preferences)
    val historyViewModelFactory   = HistoryViewModel.Factory(chapterRepository, preferences)

    fun chapterListFactory(mangaId: String) =
        ChapterListViewModel.Factory(chapterRepository, mangaId, preferences)
}