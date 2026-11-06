package com.example.movies.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.movies.data.model.MovieDetail
import com.example.movies.ui.common.ErrorWithRetry
import com.example.movies.ui.common.FullScreenLoading

@Composable
fun MovieDetailRoute(
    onBack: () -> Unit,
    viewModel: MovieDetailViewModel = viewModel(factory = MovieDetailViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MovieDetailScreen(state, onBack, onRetry = viewModel::load)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(state: MovieDetailUiState, onBack: () -> Unit, onRetry: () -> Unit) {
    val title = (state as? MovieDetailUiState.Success)?.movie?.title ?: ""
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            when (state) {
                MovieDetailUiState.Loading -> FullScreenLoading()
                is MovieDetailUiState.Error -> ErrorWithRetry(state.message, onRetry)
                is MovieDetailUiState.Success -> MovieDetailContent(state.movie)
            }
        }
    }
}

@Composable
private fun MovieDetailContent(movie: MovieDetail) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.width(130.dp).height(195.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(movie.title, style = MaterialTheme.typography.titleLarge)
                Text(listOfNotNull(movie.year, movie.rated, movie.runtime).joinToString(" · "))
                movie.genre?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                movie.imdbRating?.let { rating ->
                    Text("IMDb $rating/10" + (movie.imdbVotes?.let { " ($it votes)" } ?: ""),
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }
        movie.plot?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        InfoLine("Director", movie.director)
        InfoLine("Writer", movie.writer)
        InfoLine("Cast", movie.actors)
        InfoLine("Released", movie.released)
        InfoLine("Language", movie.language)
        if (movie.ratings.isNotEmpty()) {
            Text("Ratings", style = MaterialTheme.typography.titleMedium)
            movie.ratings.forEach { (source, value) -> InfoLine(source, value) }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String?) {
    if (value == null) return
    Row(Modifier.fillMaxWidth()) {
        Text("$label: ", fontWeight = FontWeight.SemiBold)
        Text(value)
    }
}
