# Gson reads DTO fields by reflection, so R8 must not rename or remove them.
-keep class com.example.movies.data.remote.dto.** { *; }
-keep,allowobfuscation interface com.example.movies.data.remote.OmdbApi
