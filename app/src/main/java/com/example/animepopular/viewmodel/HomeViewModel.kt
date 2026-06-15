package com.example.animepopular.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.animepopular.data.preferences.AppPreferences
import com.example.animepopular.data.remote.dto.ApiResult
import com.example.animepopular.data.repository.FavoritesRepository
import com.example.animepopular.data.repository.MangaRepository
import com.example.animepopular.model.Manga
import com.example.animepopular.util.Constants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val mangaRepository: MangaRepository,
    private val favoritesRepository: FavoritesRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** userId aktif — berubah saat login/logout */
    val userId: StateFlow<String> = preferences.userIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Constants.GUEST_USER_ID)

    val isGuest: StateFlow<Boolean> = preferences.userModeFlow
        .map { it == Constants.USER_MODE_GUEST }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    /** favoriteIds otomatis switch saat userId berubah */
    val favoriteIds: StateFlow<Set<String>> = userId.flatMapLatest { uid ->
        favoritesRepository.getAllFavoriteIds(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteCount: StateFlow<Int> = userId.flatMapLatest { uid ->
        favoritesRepository.getFavoriteCount(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init { loadManga() }

    fun loadManga() {
        viewModelScope.launch {
            mangaRepository.getPopularManga()
                .collect { result ->
                    _uiState.value = when (result) {
                        is ApiResult.Loading -> HomeUiState.Loading
                        is ApiResult.Success -> {
                            Timber.d("Loaded ${result.data.size} manga")
                            HomeUiState.Success(result.data)
                        }
                        is ApiResult.Error -> HomeUiState.Error(result.message)
                        is ApiResult.Empty -> HomeUiState.Error("No manga found")
                    }
                }
        }
    }

    fun toggleFavorite(manga: Manga) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(manga, userId.value)
        }
    }

    class Factory(
        private val mangaRepository: MangaRepository,
        private val favoritesRepository: FavoritesRepository,
        private val preferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            HomeViewModel(mangaRepository, favoritesRepository, preferences) as T
    }
}

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val mangaList: List<Manga>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}