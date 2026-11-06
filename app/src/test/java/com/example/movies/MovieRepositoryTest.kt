package com.example.movies

import com.example.movies.data.MovieRepositoryImpl
import com.example.movies.data.remote.OmdbApi
import com.example.movies.data.remote.dto.MovieDetailDto
import com.example.movies.data.remote.dto.MovieSummaryDto
import com.example.movies.data.remote.dto.SearchResponseDto
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class MovieRepositoryTest {

    /** Fake API: each test decides what search() returns or throws. */
    private class FakeOmdbApi(private val onSearch: () -> SearchResponseDto) : OmdbApi {
        override suspend fun search(query: String, page: Int) = onSearch()
        override suspend fun getDetail(imdbId: String, plot: String): MovieDetailDto =
            throw UnsupportedOperationException()
    }

    private fun repo(onSearch: () -> SearchResponseDto) = MovieRepositoryImpl(FakeOmdbApi(onSearch))

    @Test fun successMapsMoviesAndTotalAndNaPosterBecomesNull() = runTest {
        val result = repo {
            SearchResponseDto(
                search = listOf(MovieSummaryDto("tt1", "Batman", "1989", "movie", "N/A")),
                totalResults = "523", response = "True", error = null
            )
        }.search("batman", 1)

        val page = result.getOrThrow()
        assertEquals(523, page.totalResults)
        assertEquals("Batman", page.movies.single().title)
        assertNull(page.movies.single().posterUrl)
    }

    @Test fun movieNotFoundIsAnEmptyResultNotAnError() = runTest {
        val result = repo {
            SearchResponseDto(null, null, response = "False", error = "Movie not found!")
        }.search("zzzzqqq", 1)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().movies.isEmpty())
    }

    @Test fun tooManyResultsIsAnError() = runTest {
        val result = repo {
            SearchResponseDto(null, null, response = "False", error = "Too many results.")
        }.search("ab", 1)

        assertEquals("Too many results.", result.exceptionOrNull()?.message)
    }

    @Test fun http401ShowsOmdbMessageNotHttpCode() = runTest {
        val body = """{"Response":"False","Error":"Invalid API key!"}""".toResponseBody()
        val result = repo { throw HttpException(Response.error<Any>(401, body)) }.search("batman", 1)

        assertEquals("Invalid API key!", result.exceptionOrNull()?.message)
    }

    @Test fun networkFailureShowsReadableMessage() = runTest {
        val result = repo { throw IOException("timeout") }.search("batman", 1)

        assertEquals("Couldn't reach the server. Check your connection.", result.exceptionOrNull()?.message)
    }
}
