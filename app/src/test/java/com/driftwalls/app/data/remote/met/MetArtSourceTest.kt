package com.driftwalls.app.data.remote.met

import com.driftwalls.app.domain.model.Category
import com.driftwalls.app.domain.model.SearchQuery
import com.driftwalls.app.testing.FakeArtworkDao
import com.driftwalls.app.testing.api
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class MetArtSourceTest {
    private val server = MockWebServer()
    private val dao = FakeArtworkDao()
    private val objects = mutableMapOf<Long, String>()
    private var searchBody = """{"total":4,"objectIDs":[1,2,3,4]}"""
    private val requests = mutableListOf<String>()

    private fun obj(id: Long, publicDomain: Boolean, image: String = "https://img/$id.jpg") =
        """{"objectID":$id,"isPublicDomain":$publicDomain,"primaryImage":"$image","primaryImageSmall":"",
           "title":"Object $id","artistDisplayName":null,"objectDate":"1850","rightsAndReproduction":""}"""

    @Before
    fun setUp() {
        objects[1] = obj(1, publicDomain = true)
        objects[2] = obj(2, publicDomain = false)
        objects[3] = obj(3, publicDomain = true, image = "")
        objects[4] = obj(4, publicDomain = true)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path!!
                synchronized(requests) { requests += path }
                return when {
                    path.startsWith("/v1.1/search") -> MockResponse().setBody(searchBody)
                    path.startsWith("/v1/objects/") -> objects[path.substringAfterLast('/').toLong()]
                        ?.let { MockResponse().setBody(it) }
                        ?: MockResponse().setResponseCode(404).setBody("""{"message":"Not a valid object"}""")
                    else -> MockResponse().setResponseCode(500)
                }
            }
        }
        server.start()
    }

    @After
    fun tearDown() = server.shutdown()

    private fun source() = MetArtSource(server.api<MetApi>(), dao)

    @Test
    fun `keeps only public-domain objects with images, in search order`() = runTest {
        val page = source().search(SearchQuery.Text("sunflowers"), page = 1, pageSize = 4)
        assertEquals(listOf("met:1", "met:4"), page.items.map { it.id })
        assertFalse(page.hasMore)
        assertEquals(setOf(2L, 3L), dao.rejected.keys)
        assertEquals(setOf("met:1", "met:4"), dao.artworks.keys)
    }

    @Test
    fun `uses v1_1 search with category department and paging`() = runTest {
        searchBody = """{"total":100,"objectIDs":[1]}"""
        val page = source().search(SearchQuery.InCategory(Category.JAPANESE_PRINTS), page = 3, pageSize = 10)
        val search = requests.first { it.startsWith("/v1.1/search") }
        assertTrue(search, search.contains("q=ukiyo-e"))
        assertTrue(search, search.contains("departmentId=${MetApi.DEPT_ASIAN_ART}"))
        assertTrue(search, search.contains("hasImages=true"))
        assertTrue(search, search.contains("offset=20") && search.contains("limit=10"))
        assertTrue(page.hasMore)
    }

    @Test
    fun `second search uses the cache instead of refetching objects`() = runTest {
        source().search(SearchQuery.Text("x"), page = 1, pageSize = 4)
        requests.clear()
        val page = source().search(SearchQuery.Text("x"), page = 1, pageSize = 4)
        assertEquals(listOf("met:1", "met:4"), page.items.map { it.id })
        assertEquals(listOf("/v1.1/search"), requests.map { it.substringBefore('?') })
    }

    @Test
    fun `missing objects are treated as unusable, not errors`() = runTest {
        searchBody = """{"total":2,"objectIDs":[1,99]}"""
        val page = source().search(SearchQuery.Text("x"), page = 1, pageSize = 2)
        assertEquals(listOf("met:1"), page.items.map { it.id })
        assertTrue(99L in dao.rejected)
    }

    @Test
    fun `empty search is an empty page`() = runTest {
        searchBody = """{"total":0,"objectIDs":null}"""
        val page = source().search(SearchQuery.Text("zzzz"), page = 1, pageSize = 10)
        assertTrue(page.items.isEmpty())
        assertFalse(page.hasMore)
    }

    @Test
    fun `stops before the 10,000 result window`() = runTest {
        val page = source().search(SearchQuery.Text("x"), page = 501, pageSize = 20)
        assertTrue(page.items.isEmpty())
        assertTrue(requests.isEmpty())
    }

    @Test(expected = IOException::class)
    fun `throws when the network is down`() = runTest {
        server.shutdown()
        source().search(SearchQuery.Text("x"), page = 1, pageSize = 4)
    }

    @Test
    fun `generated category never calls the api`() = runTest {
        val page = source().search(SearchQuery.InCategory(Category.GENERATED), page = 1, pageSize = 4)
        assertTrue(page.items.isEmpty())
        assertTrue(requests.isEmpty())
    }
}
