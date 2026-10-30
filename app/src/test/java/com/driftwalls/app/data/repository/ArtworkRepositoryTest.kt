package com.driftwalls.app.data.repository

import com.driftwalls.app.data.remote.ArtSource
import com.driftwalls.app.data.remote.SourcePage
import com.driftwalls.app.domain.model.Artwork
import com.driftwalls.app.domain.model.Museum
import com.driftwalls.app.domain.model.SearchQuery
import com.driftwalls.app.testing.FakeArtworkDao
import com.driftwalls.app.testing.FakeFavoriteDao
import com.driftwalls.app.testing.artwork
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ArtworkRepositoryTest {
    private class FakeSource(
        override val museum: Museum,
        var page: SourcePage = SourcePage.EMPTY,
        var error: Exception? = null,
        val byId: Map<String, Artwork> = emptyMap(),
    ) : ArtSource {
        var fetches = 0
        override suspend fun search(query: SearchQuery, page: Int, pageSize: Int): SourcePage {
            error?.let { throw it }
            return this.page
        }

        override suspend fun fetch(sourceId: String): Artwork? {
            fetches++
            return byId[sourceId]
        }
    }

    private val dao = FakeArtworkDao()
    private val aic = FakeSource(Museum.AIC)
    private val met = FakeSource(Museum.MET, byId = mapOf("7" to artwork(Museum.MET, "7")))
    private val repo = ArtworkRepository(listOf(aic, met), dao, FakeFavoriteDao(dao))
    private val query = SearchQuery.Text("x")

    @Test
    fun `interleaves museums and caches results`() = runTest {
        aic.page = SourcePage(listOf(artwork(Museum.AIC, "1"), artwork(Museum.AIC, "2"), artwork(Museum.AIC, "3")), hasMore = false)
        met.page = SourcePage(listOf(artwork(Museum.MET, "1")), hasMore = true)

        val page = repo.search(query, page = 1, pageSize = 4)

        assertEquals(listOf("aic:1", "met:1", "aic:2", "aic:3"), page.items.map { it.id })
        assertTrue(page.hasMore)
        assertTrue(page.failedSources.isEmpty())
        assertEquals(4, dao.artworks.size)
    }

    @Test
    fun `one museum failing still returns the other`() = runTest {
        aic.error = IOException("offline")
        met.page = SourcePage(listOf(artwork(Museum.MET, "1")), hasMore = false)

        val page = repo.search(query, page = 1, pageSize = 4)

        assertEquals(listOf("met:1"), page.items.map { it.id })
        assertEquals(listOf(Museum.AIC), page.failedSources)
    }

    @Test(expected = IOException::class)
    fun `all museums failing throws`() = runTest {
        aic.error = IOException("offline")
        met.error = IOException("offline")
        repo.search(query, page = 1, pageSize = 4)
    }

    @Test
    fun `empty results are not an error`() = runTest {
        val page = repo.search(query, page = 1, pageSize = 4)
        assertTrue(page.items.isEmpty())
        assertFalse(page.hasMore)
    }

    @Test
    fun `artwork lookup uses the cache before the network`() = runTest {
        assertEquals("met:7", repo.artwork("met:7")!!.id)
        assertEquals("met:7", repo.artwork("met:7")!!.id)
        assertEquals(1, met.fetches)
        assertNull(repo.artwork("gen:whatever"))
        assertNull(repo.artwork("met:404"))
    }

    @Test
    fun `favourites round-trip`() = runTest {
        repo.artwork("met:7")
        repo.setFavorite("met:7", true)
        assertTrue(repo.isFavorite("met:7").first())
        assertEquals(listOf("met:7"), repo.favorites().first().map { it.id })
        repo.setFavorite("met:7", false)
        assertFalse(repo.isFavorite("met:7").first())
    }

    @Test
    fun `interleave handles uneven lists`() {
        assertEquals(listOf(1, 10, 2, 3), ArtworkRepository.interleave(listOf(listOf(1, 2, 3), listOf(10))))
        assertEquals(emptyList<Int>(), ArtworkRepository.interleave(emptyList<List<Int>>()))
    }
}
