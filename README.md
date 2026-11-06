# Movie Search (Swiggy assignment)

Search movies with the [OMDb API](https://www.omdbapi.com/), page through results, and open a detail screen.

| Task | Where |
| --- | --- |
| T1 Search + list | `ui/search/MovieSearchScreen.kt`, `MovieSearchViewModel.kt` |
| T2 Detail screen | `ui/detail/MovieDetailScreen.kt`, `MovieDetailViewModel.kt` |
| T3 Kotlin | Whole project |
| T4 Coroutines | `viewModelScope`, Retrofit `suspend` functions, Flow `debounce` pipeline |
| Pagination | `loadNextPage()` + `derivedStateOf` trigger in the screen |

## Architecture: MVVM, unidirectional data flow

```
Screen (Compose) --events--> ViewModel --> MovieRepository --> Retrofit (OmdbApi)
       ^                         |
       +---- StateFlow<UiState> --+
```

- **UI**: Jetpack Compose + Material 3, single Activity, Navigation Compose (`search`, `detail/{imdbId}`). `MainActivity` has no logic.
- **ViewModels** expose `StateFlow` (private `MutableStateFlow`, public read-only). Search state is one data class; detail state is a sealed interface `Loading / Success / Error`. The detail ViewModel reads `imdbId` from `SavedStateHandle`.
- **Search pipeline**: `query -> debounce(400) -> trim -> distinctUntilChanged -> collectLatest`. Needs 3+ letters (OMDb says "Too many results." for 1-2). A new query cancels the old request and resets to page 1; stale responses are ignored.
- **Pagination**: OMDb returns 10 per page; stops when loaded count >= `totalResults` (a String in the JSON). Duplicate `imdbID`s across pages are removed (LazyColumn keys must be unique).
- **Repository** returns `Result<T>` and rethrows `CancellationException`. OMDb quirks handled:
  - errors arrive as HTTP 200 with `"Response":"False"`; `"Movie not found!"` becomes an empty list, anything else an error;
  - bad/missing API key arrives as HTTP 401 with the same JSON body; the message is shown, not "HTTP 401";
  - `"N/A"` values become `null` and are hidden.
- **Network**: `ApiKeyInterceptor` appends `apikey` to every request; logging only in debug.
- **No Hilt**: manual `viewModelFactory { initializer { } }`. Hilt would replace this in production.
- **Release build**: R8 minify + resource shrinking, keep rule for Gson DTOs (`proguard-rules.pro`).

## Setup

1. Get a free key at http://www.omdbapi.com/apikey.aspx and click the activation link in the email.
2. Add it to `local.properties` (never committed):
   ```
   OMDB_API_KEY=yourkey
   ```
3. Build and run:
   ```bash
   ./gradlew installDebug
   adb shell am start -n com.example.movies/.MainActivity
   ```

## Tests

```bash
./gradlew testDebugUnitTest
```

- `MovieSearchViewModelTest`: debounce makes one request, 2-letter query makes none, pagination stops at `totalResults`, new query resets results.
- `MovieRepositoryTest`: mapping and `N/A` poster, "Movie not found!" is empty, "Too many results." is an error, HTTP 401 shows OMDb's message, network failure shows a readable message.
