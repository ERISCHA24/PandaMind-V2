package com.example.animepopular.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.animepopular.data.preferences.AppPreferences
import com.example.animepopular.data.remote.dto.ApiResult
import com.example.animepopular.data.repository.*
import com.example.animepopular.model.*
import com.example.animepopular.util.Constants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Favorites ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    val userId: StateFlow<String> = preferences.userIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Constants.GUEST_USER_ID)

    val isGuest: StateFlow<Boolean> = preferences.userModeFlow
        .map { it == Constants.USER_MODE_GUEST }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val favorites: StateFlow<List<Manga>> = userId.flatMapLatest { uid ->
        favoritesRepository.getAllFavorites(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeFavorite(manga: Manga) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(manga, userId.value)
        }
    }

    class Factory(
        private val repo: FavoritesRepository,
        private val preferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            FavoritesViewModel(repo, preferences) as T
    }
}

// ── Top Rated ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class TopRatedViewModel(
    private val mangaRepository: MangaRepository,
    private val favoritesRepository: FavoritesRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<TopRatedUiState>(TopRatedUiState.Loading)
    val uiState: StateFlow<TopRatedUiState> = _uiState.asStateFlow()

    val userId: StateFlow<String> = preferences.userIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Constants.GUEST_USER_ID)

    val favoriteIds: StateFlow<Set<String>> = userId.flatMapLatest { uid ->
        favoritesRepository.getAllFavoriteIds(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // ✅ Event snackbar
    private val _favoriteAddedEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val favoriteAddedEvent: SharedFlow<String> = _favoriteAddedEvent.asSharedFlow()

    init { loadTopRated() }

    fun loadTopRated() {
        viewModelScope.launch {
            mangaRepository.getTopRatedManga().collect { result ->
                _uiState.value = when (result) {
                    is ApiResult.Loading -> TopRatedUiState.Loading
                    is ApiResult.Success -> TopRatedUiState.Success(result.data)
                    is ApiResult.Error   -> TopRatedUiState.Error(result.message)
                    is ApiResult.Empty   -> TopRatedUiState.Success(emptyList())
                }
            }
        }
    }

    fun toggleFavorite(manga: Manga) {
        viewModelScope.launch {
            val addedTitle = favoritesRepository.toggleFavorite(manga, userId.value)
            if (addedTitle != null) _favoriteAddedEvent.emit(addedTitle)
        }
    }

    class Factory(
        private val mangaRepository: MangaRepository,
        private val favoritesRepository: FavoritesRepository,
        private val preferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            TopRatedViewModel(mangaRepository, favoritesRepository, preferences) as T
    }
}

sealed class TopRatedUiState {
    data object Loading : TopRatedUiState()
    data class Success(val list: List<Manga>) : TopRatedUiState()
    data class Error(val message: String) : TopRatedUiState()
}

// ── Genre ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class GenreViewModel(
    private val mangaRepository: MangaRepository,
    private val favoritesRepository: FavoritesRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<GenreUiState>(GenreUiState.Loading)
    val uiState: StateFlow<GenreUiState> = _uiState.asStateFlow()

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    private val _selectedGenre = MutableStateFlow("Action")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    val userId: StateFlow<String> = preferences.userIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Constants.GUEST_USER_ID)

    val favoriteIds: StateFlow<Set<String>> = userId.flatMapLatest { uid ->
        favoritesRepository.getAllFavoriteIds(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // ✅ Event snackbar
    private val _favoriteAddedEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val favoriteAddedEvent: SharedFlow<String> = _favoriteAddedEvent.asSharedFlow()

    init {
        loadTags()
        loadByGenre("Action")
    }

    fun loadByGenre(genre: String) {
        _selectedGenre.value = genre
        viewModelScope.launch {
            mangaRepository.getMangaByGenre(genre).collect { result ->
                _uiState.value = when (result) {
                    is ApiResult.Loading -> GenreUiState.Loading
                    is ApiResult.Success -> GenreUiState.Success(result.data)
                    is ApiResult.Error   -> GenreUiState.Error(result.message)
                    is ApiResult.Empty   -> GenreUiState.Success(emptyList())
                }
            }
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            val result = mangaRepository.getAllTags()
            _tags.value = if (result is ApiResult.Success) {
                result.data.filter { it.group == "genre" }
            } else {
                listOf("Action","Adventure","Comedy","Drama","Fantasy","Horror",
                    "Mystery","Romance","Sci-Fi","Slice of Life","Sports","Supernatural","Thriller")
                    .mapIndexed { i, name -> Tag(i.toString(), name, "genre") }
            }
        }
    }

    fun toggleFavorite(manga: Manga) {
        viewModelScope.launch {
            val addedTitle = favoritesRepository.toggleFavorite(manga, userId.value)
            if (addedTitle != null) _favoriteAddedEvent.emit(addedTitle)
        }
    }

    class Factory(
        private val mangaRepository: MangaRepository,
        private val favoritesRepository: FavoritesRepository,
        private val preferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            GenreViewModel(mangaRepository, favoritesRepository, preferences) as T
    }
}

sealed class GenreUiState {
    data object Loading : GenreUiState()
    data class Success(val list: List<Manga>) : GenreUiState()
    data class Error(val message: String) : GenreUiState()
}

// ── Profile / Settings ─────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val preferences: AppPreferences,
    private val favoritesRepository: FavoritesRepository,
    private val authRepository: AuthRepository,
    private val chapterRepository: ChapterRepository
) : ViewModel() {

    val username    = preferences.usernameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Guest")
    val language    = preferences.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")
    val darkMode    = preferences.darkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val isLoggedIn  = preferences.isLoggedInFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val userMode    = preferences.userModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Constants.USER_MODE_GUEST)
    val userId      = preferences.userIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Constants.GUEST_USER_ID)
    val isGuest: StateFlow<Boolean> = userMode
        .map { it == Constants.USER_MODE_GUEST }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val favoriteCount = userId.flatMapLatest { uid ->
        favoritesRepository.getFavoriteCount(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val watchedCount = userId.flatMapLatest { uid ->
        favoritesRepository.getWatchedCount(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val reviewCount = userId.flatMapLatest { uid ->
        favoritesRepository.getTotalReviewCount(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val historyCount = userId.flatMapLatest { uid ->
        chapterRepository.getHistoryCount(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun setLanguage(lang: String) {
        viewModelScope.launch { preferences.setLanguage(lang) }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferences.setDarkMode(enabled) }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val result = authRepository.login(username = username, password = password)
            _loginState.value = when (result) {
                is ApiResult.Success -> LoginState.Success
                is ApiResult.Error   -> LoginState.Error(result.message.ifBlank { "Login gagal" })
                else                 -> LoginState.Error("Login gagal")
            }
        }
    }

    fun loginAsGuest() {
        viewModelScope.launch {
            authRepository.loginAsGuest()
            _loginState.value = LoginState.GuestMode
        }
    }

    fun logout() {
        viewModelScope.launch {
            favoritesRepository.clearGuestData()
            chapterRepository.clearGuestData()
            authRepository.logout()
            _loginState.value = LoginState.Idle
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    class Factory(
        private val preferences: AppPreferences,
        private val favoritesRepository: FavoritesRepository,
        private val authRepository: AuthRepository,
        private val chapterRepository: ChapterRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            ProfileViewModel(preferences, favoritesRepository, authRepository, chapterRepository) as T
    }
}

sealed class LoginState {
    data object Idle       : LoginState()
    data object Loading    : LoginState()
    data object Success    : LoginState()
    data object GuestMode  : LoginState()
    data class Error(val message: String) : LoginState()
}

// ── News ───────────────────────────────────────────────────────────────────────

class NewsViewModel : ViewModel() {
    val newsList: StateFlow<List<NewsItem>> = MutableStateFlow(NewsRepository.getNewsList())
}

// ── Schedule ───────────────────────────────────────────────────────────────────

class ScheduleViewModel : ViewModel() {
    val scheduleList: StateFlow<List<ScheduleItem>> = MutableStateFlow(ScheduleRepository.getScheduleList())
}