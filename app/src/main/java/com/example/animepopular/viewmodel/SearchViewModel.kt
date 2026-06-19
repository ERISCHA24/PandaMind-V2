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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val mangaRepository: MangaRepository,
    private val favoritesRepository: FavoritesRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Initial)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val userId: StateFlow<String> = preferences.userIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Constants.GUEST_USER_ID)

    val isGuest: StateFlow<Boolean> = preferences.userModeFlow
        .map { it == Constants.USER_MODE_GUEST }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val favoriteIds: StateFlow<Set<String>> = userId.flatMapLatest { uid ->
        favoritesRepository.getAllFavoriteIds(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // ✅ Event snackbar — emit judul manga yang baru ditambahkan
    private val _favoriteAddedEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val favoriteAddedEvent: SharedFlow<String> = _favoriteAddedEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(400)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        _uiState.value = SearchUiState.Initial
                    } else {
                        mangaRepository.searchManga(query).collect { result ->
                            _uiState.value = when (result) {
                                is ApiResult.Loading -> SearchUiState.Loading
                                is ApiResult.Success -> SearchUiState.Success(result.data)
                                is ApiResult.Error   -> SearchUiState.Error(result.message)
                                is ApiResult.Empty   -> SearchUiState.Success(emptyList())
                            }
                        }
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

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
            SearchViewModel(mangaRepository, favoritesRepository, preferences) as T
    }
}

sealed class SearchUiState {
    data object Initial : SearchUiState()
    data object Loading : SearchUiState()
    data class Success(val results: List<Manga>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}