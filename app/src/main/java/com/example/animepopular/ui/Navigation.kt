package com.example.animepopular.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Login       : Screen("login")
    data object Home        : Screen("home")
    data object Search      : Screen("search")
    data object Favorites   : Screen("favorites")
    data object TopRated    : Screen("top_rated")
    data object Profile     : Screen("profile")
    data object History     : Screen("history")                            // ✅ NEW
    data object Detail      : Screen("detail/{mangaId}") {
        fun createRoute(mangaId: String) = "detail/$mangaId"
    }
    data object Genre       : Screen("genre/{genre}") {
        fun createRoute(genre: String) = "genre/$genre"
    }
    data object News        : Screen("news")
    data object Schedule    : Screen("schedule")
    data object Settings    : Screen("settings")
    data object About       : Screen("about")
    data object Language    : Screen("language")
    data object ChapterList : Screen("chapter_list/{mangaId}/{mangaTitle}") {
        fun createRoute(mangaId: String, mangaTitle: String) =
            "chapter_list/$mangaId/${mangaTitle.encodeForRoute()}"
    }
    data object Reader      : Screen("reader/{chapterId}/{mangaId}/{chapterTitle}") {
        fun createRoute(chapterId: String, mangaId: String, chapterTitle: String) =
            "reader/$chapterId/$mangaId/${chapterTitle.encodeForRoute()}"
    }
}

fun String.encodeForRoute(): String = java.net.URLEncoder.encode(this, "UTF-8")
fun String.decodeFromRoute(): String = java.net.URLDecoder.decode(this, "UTF-8")

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val labelId: String
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,      "Home",     Icons.Filled.Home,         "home"),
    BottomNavItem(Screen.Search,    "Search",   Icons.Filled.Search,       "search"),
    BottomNavItem(Screen.Favorites, "Favorite", Icons.Filled.Favorite,     "favorites"),
    BottomNavItem(Screen.History,   "History",  Icons.Filled.History,      "history"),  // ✅ NEW
    BottomNavItem(Screen.Profile,   "Profile",  Icons.Filled.Person,       "profile")
)