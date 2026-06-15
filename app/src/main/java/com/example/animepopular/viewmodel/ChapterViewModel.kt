package com.example.animepopular.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.animepopular.data.preferences.AppPreferences
import com.example.animepopular.data.remote.dto.ApiResult
import com.example.animepopular.data.repository.ChapterRepository
import com.example.animepopular.model.Chapter
import com.example.animepopular.model.ChapterPages
import com.example.animepopular.model.ReadingProgress
import com.example.animepopular.util.Constants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

// ── Chapter List VM ───────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class ChapterListViewModel(
    private val chapterRepository: ChapterRepository,
    private val mangaId: String,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChapterListUiState>(ChapterListUiState.Loading)
    val uiState: StateFlow<ChapterListUiState> = _uiState.asStateFlow()

    val userId: StateFlow<String> = preferences.userIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Constants.GUEST_USER_ID)

    val mangaProgress: StateFlow<List<ReadingProgress>> = userId.flatMapLatest { uid ->
        chapterRepository.getMangaProgress(mangaId, uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var offset = 0
    private val pageSize = 100
    private val allChapters = mutableListOf<Chapter>()

    private val _selectedLang = MutableStateFlow("en")
    val selectedLang: StateFlow<String> = _selectedLang.asStateFlow()

    init { loadChapters() }

    fun loadChapters(reset: Boolean = false) {
        if (reset) { offset = 0; allChapters.clear() }
        viewModelScope.launch {
            if (allChapters.isEmpty()) _uiState.value = ChapterListUiState.Loading
            val result = chapterRepository.getChapterList(
                mangaId = mangaId, limit = pageSize,
                offset = offset, lang = listOf(_selectedLang.value)
            )
            when (result) {
                is ApiResult.Success -> {
                    allChapters.addAll(result.data)
                    offset += result.data.size
                    _uiState.value = ChapterListUiState.Success(
                        chapters = allChapters.toList(),
                        hasMore  = result.data.size == pageSize
                    )
                }
                is ApiResult.Error -> {
                    if (allChapters.isEmpty())
                        _uiState.value = ChapterListUiState.Error(result.message)
                }
                else -> {}
            }
        }
    }

    fun setLanguage(lang: String) {
        _selectedLang.value = lang
        loadChapters(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state is ChapterListUiState.Success && state.hasMore) loadChapters()
    }

    class Factory(
        private val chapterRepository: ChapterRepository,
        private val mangaId: String,
        private val preferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            ChapterListViewModel(chapterRepository, mangaId, preferences) as T
    }
}

sealed class ChapterListUiState {
    data object Loading : ChapterListUiState()
    data class Success(val chapters: List<Chapter>, val hasMore: Boolean = false) : ChapterListUiState()
    data class Error(val message: String) : ChapterListUiState()
}

// ── Reader VM ─────────────────────────────────────────────────────────────────

class ReaderViewModel(
    private val chapterRepository: ChapterRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _pagesState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val pagesState: StateFlow<ReaderUiState> = _pagesState.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _dataSaver = MutableStateFlow(false)
    val dataSaver: StateFlow<Boolean> = _dataSaver.asStateFlow()

    private var currentChapterId: String = ""
    private var currentMangaId:   String = ""
    private var currentMangaTitle:   String = ""
    private var currentCoverFileName: String = ""
    private var currentChapterTitle: String = ""
    private var saveJob: Job? = null

    private suspend fun currentUserId() = preferences.getUserId()

    fun loadChapter(
        chapterId: String,
        mangaId: String,
        mangaTitle: String = "",
        coverFileName: String = "",
        chapterTitle: String = ""
    ) {
        if (currentChapterId == chapterId && _pagesState.value is ReaderUiState.Success) return

        currentChapterId     = chapterId
        currentMangaId       = mangaId
        currentMangaTitle    = mangaTitle
        currentCoverFileName = coverFileName
        currentChapterTitle  = chapterTitle

        viewModelScope.launch {
            _pagesState.value = ReaderUiState.Loading
            val uid = currentUserId()

            // Ambil progress tersimpan
            val savedProgress = chapterRepository.getProgress(chapterId, uid)
            val startPage     = savedProgress?.currentPage ?: 0

            when (val result = chapterRepository.getChapterPages(chapterId)) {
                is ApiResult.Success -> {
                    val safePage = startPage.coerceIn(0, (result.data.totalPages - 1).coerceAtLeast(0))
                    _currentPage.value = safePage
                    _pagesState.value  = ReaderUiState.Success(result.data)
                    Timber.d("[$uid] Chapter loaded: ${result.data.totalPages} pages, resume at $safePage")

                    // ✅ Simpan ke history (skip kalau guest)
                    chapterRepository.addToHistory(
                        mangaId      = mangaId,
                        mangaTitle   = mangaTitle,
                        coverFileName = coverFileName,
                        chapterId    = chapterId,
                        chapterTitle = chapterTitle,
                        userId       = uid
                    )
                }
                is ApiResult.Error -> _pagesState.value = ReaderUiState.Error(result.message)
                else -> {}
            }
        }
    }

    fun goToPage(page: Int) {
        val state    = _pagesState.value as? ReaderUiState.Success ?: return
        val safePage = page.coerceIn(0, (state.pages.totalPages - 1).coerceAtLeast(0))
        if (_currentPage.value == safePage) return
        _currentPage.value = safePage
        scheduleSaveProgress()
    }

    fun nextPage() {
        val state = _pagesState.value as? ReaderUiState.Success ?: return
        if (_currentPage.value < state.pages.totalPages - 1) {
            _currentPage.value++
            scheduleSaveProgress()
        }
    }

    fun prevPage() {
        if (_currentPage.value > 0) {
            _currentPage.value--
            scheduleSaveProgress()
        }
    }

    fun toggleDataSaver() { _dataSaver.value = !_dataSaver.value }

    private fun scheduleSaveProgress() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1500)
            val state = _pagesState.value as? ReaderUiState.Success ?: return@launch
            val uid = currentUserId()
            chapterRepository.saveProgress(
                chapterId   = currentChapterId,
                mangaId     = currentMangaId,
                currentPage = _currentPage.value,
                totalPages  = state.pages.totalPages,
                userId      = uid
            )
        }
    }

    fun saveProgressNow() {
        saveJob?.cancel()
        viewModelScope.launch {
            val state = _pagesState.value as? ReaderUiState.Success ?: return@launch
            val uid = currentUserId()
            chapterRepository.saveProgress(
                chapterId   = currentChapterId,
                mangaId     = currentMangaId,
                currentPage = _currentPage.value,
                totalPages  = state.pages.totalPages,
                userId      = uid
            )
        }
    }

    class Factory(
        private val chapterRepository: ChapterRepository,
        private val preferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            ReaderViewModel(chapterRepository, preferences) as T
    }
}

sealed class ReaderUiState {
    data object Loading : ReaderUiState()
    data class Success(val pages: ChapterPages) : ReaderUiState()
    data class Error(val message: String) : ReaderUiState()
}