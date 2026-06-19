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
    data object History     : Screen("history")
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

    // ✅ NEW — coverUrl ditambahkan agar bisa diteruskan ke Reader
    data object ChapterList : Screen("chapter_list/{mangaId}/{mangaTitle}/{coverUrl}") {
        fun createRoute(mangaId: String, mangaTitle: String, coverUrl: String) =
            "chapter_list/$mangaId/${mangaTitle.encodeForRoute()}/${coverUrl.encodeCoverForRoute()}"
    }

    // ✅ NEW — mangaTitle & coverUrl ditambahkan agar History bisa terisi
    data object Reader      : Screen("reader/{chapterId}/{mangaId}/{chapterTitle}/{mangaTitle}/{coverUrl}") {
        fun createRoute(
            chapterId: String, mangaId: String, chapterTitle: String,
            mangaTitle: String, coverUrl: String
        ) = "reader/$chapterId/$mangaId/${chapterTitle.encodeForRoute()}/" +
                "${mangaTitle.encodeForRoute()}/${coverUrl.encodeCoverForRoute()}"
    }
}

fun String.encodeForRoute(): String = java.net.URLEncoder.encode(this, "UTF-8")
fun String.decodeFromRoute(): String = java.net.URLDecoder.decode(this, "UTF-8")

// ✅ NEW — coverUrl bisa kosong; pakai sentinel "none" agar tidak membuat
//    segmen path kosong ("//") yang berisiko gagal dicocokkan oleh NavHost
fun String.encodeCoverForRoute(): String = if (isBlank()) "none" else encodeForRoute()
fun String.decodeCoverFromRoute(): String = if (this == "none") "" else decodeFromRoute()

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val labelId: String
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,      "Home",     Icons.Filled.Home,     "home"),
    BottomNavItem(Screen.Search,    "Search",   Icons.Filled.Search,   "search"),
    BottomNavItem(Screen.Favorites, "Favorite", Icons.Filled.Favorite, "favorites"),
    BottomNavItem(Screen.TopRated,  "Top",      Icons.Filled.Star,     "top_rated"),
    BottomNavItem(Screen.Profile,   "Profile",  Icons.Filled.Person,   "profile")
)