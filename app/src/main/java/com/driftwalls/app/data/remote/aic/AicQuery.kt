package com.driftwalls.app.data.remote.aic

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** Builds the JSON `params` for AIC artwork searches. Every query filters to public domain with an image. */
object AicQuery {
    const val FIELDS = "id,title,artist_title,artist_display,date_display,image_id,is_public_domain,thumbnail"

    /** `q` only ranks results, so categories add a filter to keep the long tail on topic. */
    fun search(text: String, page: Int, limit: Int, extraFilters: List<JsonObject> = emptyList()): String =
        buildJsonObject {
            put("q", text)
            putJsonObject("query") {
                putJsonObject("bool") {
                    put(
                        "filter",
                        buildJsonArray {
                            add(term("is_public_domain", true))
                            add(buildJsonObject { putJsonObject("exists") { put("field", "image_id") } })
                            extraFilters.forEach { add(it) }
                        },
                    )
                }
            }
            put("fields", FIELDS)
            put("limit", limit)
            put("page", page)
        }.toString()

    fun term(field: String, value: Boolean) = buildJsonObject { putJsonObject("term") { put(field, value) } }

    fun term(field: String, value: String) = buildJsonObject { putJsonObject("term") { put(field, value) } }

    fun match(field: String, value: String) = buildJsonObject { putJsonObject("match") { put(field, value) } }
}
