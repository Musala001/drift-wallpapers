package com.driftwalls.app.data.remote.aic

import com.driftwalls.app.domain.model.Category
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AicQueryTest {
    private fun filters(params: String): List<JsonObject> =
        Json.parseToJsonElement(params).jsonObject["query"]!!.jsonObject["bool"]!!.jsonObject["filter"]!!
            .jsonArray.map { it.jsonObject }

    private fun requiresPublicDomainImage(params: String) {
        val f = filters(params)
        assertTrue(f.any { it["term"]?.jsonObject?.get("is_public_domain")?.jsonPrimitive?.boolean == true })
        assertTrue(f.any { it["exists"]?.jsonObject?.get("field")?.jsonPrimitive?.content == "image_id" })
    }

    @Test
    fun `text search filters to public domain with an image`() {
        val params = AicQuery.search("cats", page = 3, limit = 20)
        requiresPublicDomainImage(params)
        val obj = Json.parseToJsonElement(params).jsonObject
        assertEquals("cats", obj["q"]!!.jsonPrimitive.content)
        assertEquals(3, obj["page"]!!.jsonPrimitive.int)
        assertEquals(20, obj["limit"]!!.jsonPrimitive.int)
        assertTrue(obj["fields"]!!.jsonPrimitive.content.contains("is_public_domain"))
    }

    @Test
    fun `every category keeps the public-domain filter`() {
        Category.entries.filterNot { it.isGenerated }.forEach { category ->
            requiresPublicDomainImage(AicArtSource.categoryParams(category, page = 1, limit = 10)!!)
        }
    }

    @Test
    fun `generated category never queries the museum`() {
        assertNull(AicArtSource.categoryParams(Category.GENERATED, page = 1, limit = 10))
    }
}
