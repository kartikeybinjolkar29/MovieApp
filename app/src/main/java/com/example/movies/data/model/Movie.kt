package com.example.movies.data.model

data class Movie(
    val imdbId: String,
    val title: String,
    val year: String,
    val type: String,
    val posterUrl: String?
)

data class MoviePage(
    val movies: List<Movie>,
    val totalResults: Int
)

data class MovieDetail(
    val imdbId: String,
    val title: String,
    val year: String,
    val rated: String?,
    val released: String?,
    val runtime: String?,
    val genre: String?,
    val director: String?,
    val writer: String?,
    val actors: String?,
    val plot: String?,
    val language: String?,
    val posterUrl: String?,
    val imdbRating: String?,
    val imdbVotes: String?,
    val ratings: List<Pair<String, String>>
)
