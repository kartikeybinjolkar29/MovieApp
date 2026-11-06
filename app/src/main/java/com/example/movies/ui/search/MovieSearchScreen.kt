package com.example.movies.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.movies.data.model.Movie
import com.example.movies.ui.common.ErrorWithRetry
import com.example.movies.ui.common.FullScreenLoading

@Composable
fun MovieSearchRoute(
    onMovieClick: (String) -> Unit,
    viewModel: MovieSearchViewModel = viewModel(factory = MovieSearchViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MovieSearchScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onMovieClick = onMovieClick,
        onLoadMore = viewModel::loadNextPage,
        onRetry = viewModel::loadNextPage
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieSearchScreen(
    state: MovieSearchUiState,
    onQueryChange: (String) -> Unit,
    onMovieClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Movie Search") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text("Search movies, e.g. Batman") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    state.isLoading -> FullScreenLoading()
                    state.movies.isEmpty() && state.error != null -> ErrorWithRetry(state.error, onRetry)
                    state.movies.isEmpty() && state.hasSearched -> Text("No movies found")
                    state.movies.isEmpty() -> Text("Type at least 3 letters to search")
                    else -> MovieList(state, onMovieClick, onLoadMore, onRetry)
                }
            }
        }
    }
}

@Composable
private fun MovieList(
    state: MovieSearchUiState,
    onMovieClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit
) {

    val listState = rememberLazyListState()
    LoadMoreWhenNearEnd(listState, onLoadMore)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(state.movies, key = { it.imdbId }) { movie ->
            MovieRow(movie, onClick = { onMovieClick(movie.imdbId) })
        }
        item(key = "footer") {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                when {
                    state.isLoadingMore -> CircularProgressIndicator(Modifier.size(32.dp))
                    state.error != null -> ErrorWithRetry(state.error, onRetry)
                    state.endReached -> Text("End of results")
                }
            }
        }
    }
}

@Composable
private fun LoadMoreWhenNearEnd(listState: LazyListState, onLoadMore: () -> Unit, threshold: Int = 3) {
    val shouldLoadMore by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - threshold
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }
}

@Composable
private fun MovieRow(movie: Movie, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.size(width = 60.dp, height = 90.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(movie.title, style = MaterialTheme.typography.titleMedium)
                Text("${movie.year} · ${movie.type}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
