package com.driftwalls.app.data.remote.met

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The Met Collection API (https://metmuseum.github.io/). No key needed; asks for at most 80 req/s.
 * `v1/search` was retired on 2026-10-01; `v1.1/search` pages with offset/limit (offset + limit <= 10,000).
 */
interface MetApi {
    @GET("v1.1/search")
    suspend fun search(
        @Query("q") q: String,
        @Query("departmentId") departmentId: Int? = null,
        @Query("hasImages") hasImages: Boolean = true,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20,
    ): MetSearchResponse

    @GET("v1/objects/{id}")
    suspend fun objectById(@Path("id") id: Long): MetObjectDto

    companion object {
        const val BASE_URL = "https://collectionapi.metmuseum.org/public/collection/"
        const val MAX_RESULTS = 10_000

        // Department ids from /departments.
        const val DEPT_ASIAN_ART = 6
        const val DEPT_DRAWINGS_AND_PRINTS = 9
        const val DEPT_EUROPEAN_PAINTINGS = 11
    }
}

@Serializable
data class MetSearchResponse(
    val total: Int = 0,
    val objectIDs: List<Long>? = null,
)

@Serializable
data class MetObjectDto(
    val objectID: Long,
    val isPublicDomain: Boolean = false,
    val primaryImage: String = "",
    val primaryImageSmall: String = "",
    val title: String = "",
    val artistDisplayName: String = "",
    val objectDate: String = "",
    val objectURL: String = "",
)
