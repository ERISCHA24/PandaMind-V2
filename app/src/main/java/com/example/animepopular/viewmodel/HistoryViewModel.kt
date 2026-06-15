package com.example.animepopular.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.animepopular.data.preferences.AppPreferences
import com.example.animepopular.data.repository.ChapterRepository
import com.example.animepopular.model.ReadingHistory
import com.example.animepopular.util.Constants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val chapterRepository: ChapterRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    val userId: StateFlow<String> = preferences.userIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Constants.GUEST_USER_ID)

    val isGuest: StateFlow<Boolean> = preferences.userModeFlow
        .map { it == Constants.USER_MODE_GUEST }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    /** History list — otomatis kosong saat guest */
    val history: StateFlow<List<ReadingHistory>> = userId.flatMapLatest { uid ->
        chapterRepository.getHistory(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeFromHistory(mangaId: String) {
        viewModelScope.launch {
            chapterRepository.removeFromHistory(mangaId, userId.value)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            chapterRepository.clearHistory(userId.value)
        }
    }

    class Factory(
        private val chapterRepository: ChapterRepository,
        private val preferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            HistoryViewModel(chapterRepository, preferences) as T
    }
}