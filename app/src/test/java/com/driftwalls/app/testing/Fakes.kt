package com.driftwalls.app.testing

import com.driftwalls.app.data.local.ArtworkDao
import com.driftwalls.app.data.local.ArtworkEntity
import com.driftwalls.app.data.local.FavoriteDao
import com.driftwalls.app.data.local.FavoriteEntity
import com.driftwalls.app.data.local.MetRejectedEntity
import com.driftwalls.app.domain.model.Artwork
import com.driftwalls.app.domain.model.Museum
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

val testJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

inline fun <reified T> MockWebServer.api(client: OkHttpClient = OkHttpClient()): T = Retrofit.Builder()
    .baseUrl(url("/"))
    .client(client)
    .addConverterFactory(testJson.asConverterFactory("application/json".toMediaType()))
    .build()
    .create(T::class.java)

fun artwork(museum: Museum = Museum.AIC, id: String = "1", width: Int? = null, height: Int? = null) = Artwork(
    museum = museum,
    sourceId = id,
    title = "Title $id",
    artist = "Artist",
    date = "1900",
    thumbnailUrl = "https://example.org/$id/thumb.jpg",
    imageUrl = "https://example.org/$id/full.jpg",
    width = width,
    height = height,
)

class FakeArtworkDao : ArtworkDao {
    val artworks = linkedMapOf<String, ArtworkEntity>()
    val rejected = linkedMapOf<Long, MetRejectedEntity>()

    override suspend fun upsert(items: List<ArtworkEntity>) = items.forEach { artworks[it.id] = it }
    override suspend fun byId(id: String) = artworks[id]
    override suspend fun byIds(ids: List<String>) = ids.mapNotNull { artworks[it] }
    override suspend fun markMetRejected(items: List<MetRejectedEntity>) = items.forEach { rejected[it.objectId] = it }
    override suspend fun metRejectedAmong(ids: List<Long>, since: Long) =
        ids.filter { id -> rejected[id]?.let { it.checkedAt >= since } == true }
}

class FakeFavoriteDao(private val artworkDao: FakeArtworkDao) : FavoriteDao {
    private val favorites = MutableStateFlow<List<FavoriteEntity>>(emptyList())

    override fun observeFavoriteArtworks(): Flow<List<ArtworkEntity>> = favorites.map { favs ->
        favs.sortedByDescending { it.addedAt }.mapNotNull { artworkDao.artworks[it.wallpaperId] }
    }

    override fun observeIsFavorite(id: String) = favorites.map { list -> list.any { it.wallpaperId == id } }

    override suspend fun add(favorite: FavoriteEntity) {
        if (favorites.value.none { it.wallpaperId == favorite.wallpaperId }) favorites.value += favorite
    }

    override suspend fun remove(id: String) {
        favorites.value = favorites.value.filterNot { it.wallpaperId == id }
    }
}
