package com.example.animepopular.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.animepopular.di.AppContainer
import com.example.animepopular.ui.screens.about.AboutScreen
import com.example.animepopular.ui.screens.chapter.ChapterListScreen
import com.example.animepopular.ui.screens.chapter.ReaderScreen
import com.example.animepopular.ui.screens.detail.DetailScreen
import com.example.animepopular.ui.screens.favorites.FavoritesScreen
import com.example.animepopular.ui.screens.genre.GenreScreen
import com.example.animepopular.ui.screens.history.HistoryScreen
import com.example.animepopular.ui.screens.home.HomeScreen
import com.example.animepopular.ui.screens.language.LanguageScreen
import com.example.animepopular.ui.screens.login.LoginScreen
import com.example.animepopular.ui.screens.news.NewsScreen
import com.example.animepopular.ui.screens.profile.ProfileScreen
import com.example.animepopular.ui.screens.schedule.ScheduleScreen
import com.example.animepopular.ui.screens.search.SearchScreen
import com.example.animepopular.ui.screens.settings.SettingsScreen
import com.example.animepopular.ui.screens.toprated.TopRatedScreen
import com.example.animepopular.ui.theme.*
import com.example.animepopular.viewmodel.*

@Composable
fun NavGraph(container: AppContainer) {
    val navController = rememberNavController()
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentRoute  = navBackStack?.destination?.route

    val profileVM: ProfileViewModel = viewModel(factory = container.profileViewModelFactory)
    val language   by profileVM.language.collectAsStateWithLifecycle()
    val isLoggedIn by profileVM.isLoggedIn.collectAsStateWithLifecycle()
    val isGuest    by profileVM.isGuest.collectAsStateWithLifecycle()

    // Helper navigate ke login + clear back stack
    fun navigateToLogin() {
        navController.navigate(Screen.Login.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    val topLevelRoutes = bottomNavItems.map { it.screen.route }
    val showBottomNav  = topLevelRoutes.any {
        currentRoute?.startsWith(it.substringBefore("/")) == true || currentRoute == it
    }

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            if (showBottomNav) {
                NavigationBar(containerColor = ColorPrimary, contentColor = AccentColor) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            icon     = { Icon(item.icon, contentDescription = item.label) },
                            label    = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                            selected = selected,
                            onClick  = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = AccentColor,
                                selectedTextColor   = AccentColor,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor      = AccentColor.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Login.route,
            modifier         = Modifier.padding(innerPadding),
            enterTransition  = { slideInHorizontally(tween(280)) { it } + fadeIn(tween(280)) },
            exitTransition   = { slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(280)) },
            popEnterTransition  = { slideInHorizontally(tween(280)) { -it } + fadeIn(tween(280)) },
            popExitTransition   = { slideOutHorizontally(tween(280)) { it } + fadeOut(tween(280)) }
        ) {

            // ── Login ─────────────────────────────────────────────────────────
            composable(
                route = Screen.Login.route,
                enterTransition = { fadeIn(tween(300)) },
                exitTransition  = { fadeOut(tween(300)) }
            ) {
                // Jika token tersimpan (USER mode) → skip Login
                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
                LoginScreen(
                    viewModel = profileVM,
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onSkip = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    language = language
                )
            }

            // ── Home ──────────────────────────────────────────────────────────
            composable(Screen.Home.route) {
                val vm: HomeViewModel = viewModel(factory = container.homeViewModelFactory)
                HomeScreen(
                    viewModel            = vm,
                    onNavigateToDetail   = { navController.navigate(Screen.Detail.createRoute(it)) },
                    onNavigateToGenre    = { navController.navigate(Screen.Genre.createRoute(it)) },
                    onNavigateToNews     = { navController.navigate(Screen.News.route) },
                    onNavigateToSchedule = { navController.navigate(Screen.Schedule.route) },
                    onNavigateToLanguage = { navController.navigate(Screen.Language.route) },
                    language             = language
                )
            }

            // ── Search ────────────────────────────────────────────────────────
            composable(Screen.Search.route) {
                val vm: SearchViewModel = viewModel(factory = container.searchViewModelFactory)
                SearchScreen(
                    viewModel          = vm,
                    onNavigateToDetail = { navController.navigate(Screen.Detail.createRoute(it)) },
                    language           = language
                )
            }

            // ── Favorites ─────────────────────────────────────────────────────
            composable(Screen.Favorites.route) {
                val vm: FavoritesViewModel = viewModel(factory = container.favoritesViewModelFactory)
                FavoritesScreen(
                    viewModel          = vm,
                    onNavigateToDetail = { navController.navigate(Screen.Detail.createRoute(it)) },
                    onNavigateToLogin  = { navigateToLogin() },
                    language           = language
                )
            }

            // ── History ───────────────────────────────────────────────────────
            composable(Screen.History.route) {
                val vm: HistoryViewModel = viewModel(factory = container.historyViewModelFactory)
                HistoryScreen(
                    viewModel          = vm,
                    onNavigateToDetail = { navController.navigate(Screen.Detail.createRoute(it)) },
                    onNavigateToLogin  = { navigateToLogin() },
                    language           = language
                )
            }

            // ── Profile ───────────────────────────────────────────────────────
            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel            = profileVM,
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToAbout    = { navController.navigate(Screen.About.route) },
                    onLogout             = { navigateToLogin() },
                    language             = language
                )
            }

            // ── Detail ────────────────────────────────────────────────────────
            composable(
                route     = Screen.Detail.route,
                arguments = listOf(navArgument("mangaId") { type = NavType.StringType })
            ) { backStack ->
                val mangaId = backStack.arguments?.getString("mangaId") ?: return@composable
                val vm: DetailViewModel = viewModel(factory = container.detailViewModelFactory)
                DetailScreen(
                    viewModel            = vm,
                    mangaId              = mangaId,
                    onBack               = { navController.popBackStack() },
                    onNavigateToChapters = { id, title ->
                        navController.navigate(Screen.ChapterList.createRoute(id, title))
                    },
                    language             = language
                )
            }

            // ── Genre ─────────────────────────────────────────────────────────
            composable(
                route     = Screen.Genre.route,
                arguments = listOf(navArgument("genre") { type = NavType.StringType })
            ) { backStack ->
                val genre = backStack.arguments?.getString("genre") ?: "Action"
                val vm: GenreViewModel = viewModel(factory = container.genreViewModelFactory)
                GenreScreen(
                    viewModel          = vm,
                    initialGenre       = genre,
                    onNavigateToDetail = { navController.navigate(Screen.Detail.createRoute(it)) },
                    language           = language
                )
            }

            // ── News ──────────────────────────────────────────────────────────
            composable(Screen.News.route) {
                val vm: NewsViewModel = viewModel()
                NewsScreen(viewModel = vm, onBack = { navController.popBackStack() }, language = language)
            }

            // ── Schedule ──────────────────────────────────────────────────────
            composable(Screen.Schedule.route) {
                val vm: ScheduleViewModel = viewModel()
                ScheduleScreen(viewModel = vm, onBack = { navController.popBackStack() }, language = language)
            }

            // ── Settings ──────────────────────────────────────────────────────
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = profileVM, onBack = { navController.popBackStack() })
            }

            // ── About ─────────────────────────────────────────────────────────
            composable(Screen.About.route) {
                AboutScreen(onBack = { navController.popBackStack() }, language = language)
            }

            // ── Language ──────────────────────────────────────────────────────
            composable(Screen.Language.route) {
                LanguageScreen(viewModel = profileVM, onBack = { navController.popBackStack() })
            }

            // ── Chapter List ──────────────────────────────────────────────────
            composable(
                route     = Screen.ChapterList.route,
                arguments = listOf(
                    navArgument("mangaId")    { type = NavType.StringType },
                    navArgument("mangaTitle") { type = NavType.StringType }
                )
            ) { backStack ->
                val mangaId    = backStack.arguments?.getString("mangaId")    ?: return@composable
                val mangaTitle = backStack.arguments?.getString("mangaTitle")?.decodeFromRoute() ?: ""
                val vm: ChapterListViewModel = viewModel(factory = container.chapterListFactory(mangaId))
                ChapterListScreen(
                    viewModel          = vm,
                    mangaTitle         = mangaTitle,
                    onNavigateToReader = { chapterId, chapterTitle ->
                        navController.navigate(
                            // ✅ Kirim mangaTitle + chapterTitle ke Reader untuk history
                            Screen.Reader.createRoute(chapterId, mangaId, chapterTitle)
                        )
                    },
                    onBack             = { navController.popBackStack() },
                    language           = language
                )
            }

            // ── Reader ────────────────────────────────────────────────────────
            composable(
                route     = Screen.Reader.route,
                arguments = listOf(
                    navArgument("chapterId")    { type = NavType.StringType },
                    navArgument("mangaId")      { type = NavType.StringType },
                    navArgument("chapterTitle") { type = NavType.StringType }
                ),
                enterTransition     = { fadeIn(tween(200)) },
                exitTransition      = { fadeOut(tween(200)) },
                popEnterTransition  = { fadeIn(tween(200)) },
                popExitTransition   = { fadeOut(tween(200)) }
            ) { backStack ->
                val chapterId    = backStack.arguments?.getString("chapterId")    ?: return@composable
                val mangaId      = backStack.arguments?.getString("mangaId")      ?: return@composable
                val chapterTitle = backStack.arguments?.getString("chapterTitle")?.decodeFromRoute() ?: ""
                val vm: ReaderViewModel = viewModel(factory = container.readerViewModelFactory)
                ReaderScreen(
                    viewModel      = vm,
                    chapterId      = chapterId,
                    mangaId        = mangaId,
                    chapterTitle   = chapterTitle,
                    onBack         = { navController.popBackStack() },
                    language       = language
                )
            }
        }
    }
}