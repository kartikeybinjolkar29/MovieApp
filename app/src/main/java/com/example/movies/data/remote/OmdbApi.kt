package com.example.movies.data.remote

import com.example.movies.data.remote.dto.MovieDetailDto
import com.example.movies.data.remote.dto.SearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OmdbApi {

    @GET(".")
    suspend fun search(
        @Query("s") query: String,
        @Query("page") page: Int
    ): SearchResponseDto

    @GET(".")
    suspend fun getDetail(
        @Query("i") imdbId: String,
        @Query("plot") plot: String = "full"
    ): MovieDetailDto
}
