package com.example.animepopular.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.animepopular.ui.components.*
import com.example.animepopular.ui.theme.*
import com.example.animepopular.viewmodel.SearchUiState
import com.example.animepopular.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToDetail: (String) -> Unit,
    language: String = "en"
) {
    val uiState     by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    // ✅ Real-time favorite IDs
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    if (language == "id") "🔍 Cari Manga" else "🔍 Search Manga",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = {
                        Text(
                            if (language == "id") "Judul, genre, penulis..." else "Title, genre, author...",
                            color = TextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentColor,
                        unfocusedBorderColor = DividerColor,
                        focusedContainerColor = SurfaceColor,
                        unfocusedContainerColor = SurfaceColor,
                        cursorColor = AccentColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is SearchUiState.Initial -> EmptyContent(
                    message = if (language == "id") "Ketik untuk mulai mencari" else "Type to start searching",
                    emoji = "🔍"
                )
                is SearchUiState.Loading -> LoadingContent()
                is SearchUiState.Error   -> ErrorContent(
                    message = state.message,
                    onRetry = { }
                )
                is SearchUiState.Success -> {
                    if (state.results.isEmpty()) {
                        EmptyContent(
                            message = if (language == "id") "Manga tidak ditemukan" else "No manga found",
                            emoji = "😅"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            item {
                                Text(
                                    text = if (language == "id") "${state.results.size} hasil ditemukan"
                                    else "${state.results.size} results found",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                            items(state.results, key = { it.id }) { manga ->
                                MangaCard(
                                    manga = manga,
                                    onDetailClick = { onNavigateToDetail(manga.id) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(manga) },
                                    isFavoriteOverride = manga.id in favoriteIds  // ✅
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}