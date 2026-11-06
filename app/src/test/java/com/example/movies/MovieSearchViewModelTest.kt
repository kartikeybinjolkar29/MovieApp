package com.example.movies

import com.example.movies.data.MovieRepository
import com.example.movies.data.model.Movie
import com.example.movies.data.model.MovieDetail
import com.example.movies.data.model.MoviePage
import com.example.movies.ui.search.MovieSearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 25 results per query, 10 per page, like OMDb. */
private class FakeMovieRepository(private val total: Int = 25) : MovieRepository {
    val queries = mutableListOf<Pair<String, Int>>()
    override suspend fun search(query: String, page: Int): Result<MoviePage> {
        queries += query to page
        val start = (page - 1) * 10
        val end = minOf(start + 10, total)
        val movies = (start until end).map { Movie("tt$query$it", "$query $it", "2000", "movie", null) }
        return Result.success(MoviePage(movies, total))
    }
    override suspend fun getDetail(imdbId: String): Result<MovieDetail> = Result.failure(NotImplementedError())
}

@OptIn(ExperimentalCoroutinesApi::class)
class MovieSearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `debounces typing into one request`() = runTest(dispatcher) {
        val repo = FakeMovieRepository()
        val vm = MovieSearchViewModel(repo)
        runCurrent()
        vm.onQueryChange("bat"); advanceTimeBy(100)
        vm.onQueryChange("batm"); advanceTimeBy(100)
        vm.onQueryChange("batman")
        advanceUntilIdle()
        assertEquals(listOf("batman" to 1), repo.queries)
        assertEquals(10, vm.state.value.movies.size)
    }

    @Test
    fun `short query does not hit the network`() = runTest(dispatcher) {
        val repo = FakeMovieRepository()
        val vm = MovieSearchViewModel(repo)
        vm.onQueryChange("ba")
        advanceUntilIdle()
        assertTrue(repo.queries.isEmpty())
    }

    @Test
    fun `paginates and stops at totalResults`() = runTest(dispatcher) {
        val repo = FakeMovieRepository(total = 25)
        val vm = MovieSearchViewModel(repo)
        vm.onQueryChange("batman"); advanceUntilIdle()
        vm.loadNextPage(); advanceUntilIdle()
        vm.loadNextPage(); advanceUntilIdle()
        vm.loadNextPage(); advanceUntilIdle()   // should be ignored
        assertEquals(25, vm.state.value.movies.size)
        assertTrue(vm.state.value.endReached)
        assertEquals(3, repo.queries.size)
    }

    @Test
    fun `new query resets results`() = runTest(dispatcher) {
        val repo = FakeMovieRepository()
        val vm = MovieSearchViewModel(repo)
        vm.onQueryChange("batman"); advanceUntilIdle()
        vm.loadNextPage(); advanceUntilIdle()
        vm.onQueryChange("superman"); advanceUntilIdle()
        assertEquals(10, vm.state.value.movies.size)
        assertTrue(vm.state.value.movies.all { it.title.startsWith("superman") })
    }
}
