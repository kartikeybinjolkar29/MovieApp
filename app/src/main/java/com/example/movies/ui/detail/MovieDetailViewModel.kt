package com.example.movies.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.movies.data.MovieRepository
import com.example.movies.data.MovieRepositoryImpl
import com.example.movies.data.model.MovieDetail
import com.example.movies.data.remote.NetworkModule
import com.example.movies.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MovieDetailUiState {
    data object Loading : MovieDetailUiState
    data class Success(val movie: MovieDetail) : MovieDetailUiState
    data class Error(val message: String) : MovieDetailUiState
}

class MovieDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository
) : ViewModel() {

    private val imdbId: String = checkNotNull(savedStateHandle[Routes.ARG_ID])

    private val _state = MutableStateFlow<MovieDetailUiState>(MovieDetailUiState.Loading)
    val state: StateFlow<MovieDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = MovieDetailUiState.Loading
        viewModelScope.launch {
            _state.value = repository.getDetail(imdbId).fold(
                onSuccess = { MovieDetailUiState.Success(it) },
                onFailure = { MovieDetailUiState.Error(it.message ?: "Could not load movie") }
            )
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                MovieDetailViewModel(createSavedStateHandle(), MovieRepositoryImpl(NetworkModule.omdbApi))
            }
        }
    }
}
