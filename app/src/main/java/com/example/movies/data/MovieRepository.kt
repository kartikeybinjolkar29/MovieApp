package com.example.movies.data

import com.example.movies.data.model.Movie
import com.example.movies.data.model.MovieDetail
import com.example.movies.data.model.MoviePage
import com.example.movies.data.remote.OmdbApi
import com.example.movies.data.remote.dto.MovieDetailDto
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

interface MovieRepository {
    suspend fun search(query: String, page: Int): Result<MoviePage>
    suspend fun getDetail(imdbId: String): Result<MovieDetail>
}


class OmdbApiException(message: String) : Exception(message)

class MovieRepositoryImpl(private val api: OmdbApi) : MovieRepository {

    override suspend fun search(query: String, page: Int): Result<MoviePage> = safeCall {
        val dto = api.search(query = query, page = page)
        if (dto.response == "True") {
            MoviePage(
                movies = dto.search.orEmpty().map {
                    Movie(it.imdbId, it.title, it.year, it.type, it.poster.posterOrNull())
                },
                totalResults = dto.totalResults?.toIntOrNull() ?: 0
            )
        } else if (dto.error == NOT_FOUND) {
            MoviePage(emptyList(), totalResults = 0)       // empty result is not an error
        } else {
            throw OmdbApiException(dto.error ?: "Unknown error")   // e.g. "Too many results.", "Invalid API key!"
        }
    }

    override suspend fun getDetail(imdbId: String): Result<MovieDetail> = safeCall {
        val dto = api.getDetail(imdbId)
        if (dto.response != "True") throw OmdbApiException(dto.error ?: "Movie not found")
        dto.toDomain()
    }

    private inline fun <T> safeCall(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e                                         // never swallow cancellation
        } catch (e: HttpException) {
            // A bad/missing key comes back as HTTP 401 with {"Error":"Invalid API key!"} in the body.
            Result.failure(OmdbApiException(e.omdbErrorMessage() ?: "Server error (${e.code()})"))
        } catch (e: IOException) {
            Result.failure(OmdbApiException("Couldn't reach the server. Check your connection."))
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun HttpException.omdbErrorMessage(): String? = try {
        val body = response()?.errorBody()?.string()
        Gson().fromJson(body, ErrorBodyDto::class.java)?.error
    } catch (e: Exception) {
        null
    }

    private data class ErrorBodyDto(@SerializedName("Error") val error: String?)

    private fun MovieDetailDto.toDomain() = MovieDetail(
        imdbId = imdbId.orEmpty(),
        title = title.orEmpty(),
        year = year.orEmpty(),
        rated = rated.clean(),
        released = released.clean(),
        runtime = runtime.clean(),
        genre = genre.clean(),
        director = director.clean(),
        writer = writer.clean(),
        actors = actors.clean(),
        plot = plot.clean(),
        language = language.clean(),
        posterUrl = poster.posterOrNull(),
        imdbRating = imdbRating.clean(),
        imdbVotes = imdbVotes.clean(),
        ratings = ratings.orEmpty().map { it.source to it.value }
    )

    private fun String?.clean(): String? = this?.takeIf { it.isNotBlank() && it != "N/A" }
    private fun String?.posterOrNull(): String? = clean()

    companion object {
        const val NOT_FOUND = "Movie not found!"
    }
}
