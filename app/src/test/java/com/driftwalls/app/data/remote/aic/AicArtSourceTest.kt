package com.driftwalls.app.data.remote.aic

import com.driftwalls.app.data.remote.net.AicUserAgentInterceptor
import com.driftwalls.app.domain.model.SearchQuery
import com.driftwalls.app.testing.api
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AicArtSourceTest {
    private val server = MockWebServer().apply { start() }

    @After
    fun tearDown() = server.shutdown()

    private fun source() = AicArtSource(server.api<AicApi>())

    private val searchBody = """
        {"pagination":{"total":3,"limit":3,"offset":0,"total_pages":5,"current_page":1},
         "data":[
           {"id":1,"title":"Public","artist_title":"A","date_display":"1890","image_id":"img-1","is_public_domain":true,
            "thumbnail":{"width":1000,"height":1500,"alt_text":"x"}},
           {"id":2,"title":"Copyrighted","image_id":"img-2","is_public_domain":false},
           {"id":3,"title":"No image","image_id":null,"is_public_domain":true}
         ],
         "config":{"iiif_url":"https://iiif.test/2","website_url":"https://www.artic.edu"}}
    """.trimIndent()

    @Test
    fun `filters out non-public-domain and imageless results`() = runTest {
        server.enqueue(MockResponse().setBody(searchBody))
        val page = source().search(SearchQuery.Text("cats"), page = 1, pageSize = 3)
        assertEquals(listOf("aic:1"), page.items.map { it.id })
        assertEquals("https://iiif.test/2/img-1/full/843,/0/default.jpg", page.items.single().thumbnailUrl)
        assertTrue(page.hasMore)
    }

    @Test
    fun `sends the query as params json with the public-domain filter`() = runTest {
        server.enqueue(MockResponse().setBody(searchBody))
        source().search(SearchQuery.Text("cats"), page = 2, pageSize = 3)
        val params = server.takeRequest().requestUrl!!.queryParameter("params")!!
        assertTrue(params, params.contains("\"is_public_domain\":true"))
        assertTrue(params, params.contains("\"page\":2"))
    }

    @Test
    fun `stops at the 1,000 record pagination cap`() = runTest {
        val page = source().search(SearchQuery.Text("cats"), page = 11, pageSize = 100)
        assertTrue(page.items.isEmpty())
        assertFalse(page.hasMore)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `clamps page size to 100`() = runTest {
        server.enqueue(MockResponse().setBody(searchBody))
        source().search(SearchQuery.Text("cats"), page = 1, pageSize = 500)
        assertTrue(server.takeRequest().requestUrl!!.queryParameter("params")!!.contains("\"limit\":100"))
    }

    @Test
    fun `fetch returns null for 404 and for non-public-domain works`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        assertNull(source().fetch("123"))
        server.enqueue(MockResponse().setBody("""{"data":{"id":2,"image_id":"x","is_public_domain":false}}"""))
        assertNull(source().fetch("2"))
    }

    @Test
    fun `user agent header is added for artic hosts only`() {
        // MockWebServer acts as an HTTP proxy, so requests to any host land on it.
        val client = OkHttpClient.Builder()
            .proxy(server.toProxyAddress())
            .addInterceptor(AicUserAgentInterceptor("Drift test"))
            .build()
        server.enqueue(MockResponse())
        server.enqueue(MockResponse())

        client.newCall(Request.Builder().url("http://api.artic.edu/api/v1/artworks".toHttpUrl()).build()).execute().close()
        assertEquals("Drift test", server.takeRequest().getHeader("AIC-User-Agent"))

        client.newCall(Request.Builder().url("http://collectionapi.metmuseum.org/x".toHttpUrl()).build()).execute().close()
        assertNull(server.takeRequest().getHeader("AIC-User-Agent"))
    }
}
