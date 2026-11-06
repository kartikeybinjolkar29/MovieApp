package com.example.movies.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.movies.data.MovieRepository
import com.example.movies.data.MovieRepositoryImpl
import com.example.movies.data.model.Movie
import com.example.movies.data.remote.NetworkModule
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MovieSearchUiState(
    val query: String = "",
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val endReached: Boolean = false,
    val hasSearched: Boolean = false
)

@OptIn(FlowPreview::class)
class MovieSearchViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MovieSearchUiState())
    val state: StateFlow<MovieSearchUiState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private var activeQuery = ""
    private var nextPage = 1
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(DEBOUNCE_MS)
                .map { it.trim() }
                .distinctUntilChanged()
                .collectLatest { startNewSearch(it) }
        }
    }


    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        queryFlow.value = query
    }

    private fun startNewSearch(query: String) {
        loadJob?.cancel()
        activeQuery = query
        nextPage = 1
        _state.update {
            it.copy(movies = emptyList(), error = null, endReached = false,
                isLoading = false, isLoadingMore = false, hasSearched = false)
        }
        if (query.length >= MIN_QUERY_LENGTH) loadNextPage()
    }


    fun loadNextPage() {
        val current = _state.value
        if (loadJob?.isActive == true || current.endReached || activeQuery.length < MIN_QUERY_LENGTH) return

        val query = activeQuery
        val page = nextPage
        val firstPage = page == 1
        _state.update { it.copy(isLoading = firstPage, isLoadingMore = !firstPage, error = null) }

        loadJob = viewModelScope.launch {
            val result = repository.search(query, page)
            if (query != activeQuery) return@launch

            result
                .onSuccess { moviePage ->
                    nextPage = page + 1
                    _state.update { s ->

                        val merged = (s.movies + moviePage.movies).distinctBy { it.imdbId }
                        s.copy(
                            movies = merged,
                            isLoading = false,
                            isLoadingMore = false,
                            hasSearched = true,
                            endReached = moviePage.movies.isEmpty() || merged.size >= moviePage.totalResults
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, isLoadingMore = false, hasSearched = true,
                            error = e.message ?: "Something went wrong")
                    }
                }
        }
    }

    companion object {
        const val DEBOUNCE_MS = 400L
        const val MIN_QUERY_LENGTH = 3

        val Factory = viewModelFactory {
            initializer { MovieSearchViewModel(MovieRepositoryImpl(NetworkModule.omdbApi)) }
        }
    }
}
