package com.example.movies.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.movies.ui.detail.MovieDetailRoute
import com.example.movies.ui.search.MovieSearchRoute

object Routes {
    const val SEARCH = "search"
    const val ARG_ID = "imdbId"
    const val DETAIL = "detail/{$ARG_ID}"
    fun detail(imdbId: String) = "detail/$imdbId"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.SEARCH) {
        composable(Routes.SEARCH) {
            MovieSearchRoute(onMovieClick = { id -> navController.navigate(Routes.detail(id)) })
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType })
        ) {
            MovieDetailRoute(onBack = { navController.popBackStack() })
        }
    }
}
