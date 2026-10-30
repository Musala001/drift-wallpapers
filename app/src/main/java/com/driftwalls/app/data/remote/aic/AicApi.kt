package com.driftwalls.app.data.remote.aic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Art Institute of Chicago API (https://api.artic.edu/docs/). Anonymous clients get 60 req/min and
 * should send an AIC-User-Agent header. Search pages cap at limit=100 and 1,000 records per query.
 */
interface AicApi {
    /** [params] is the whole query as minified JSON, as the docs recommend for production use. */
    @GET("api/v1/artworks/search")
    suspend fun search(@Query("params") params: String): AicSearchResponse

    @GET("api/v1/artworks/{id}")
    suspend fun artwork(
        @Path("id") id: String,
        @Query("fields") fields: String = AicQuery.FIELDS,
    ): AicArtworkResponse

    companion object {
        const val BASE_URL = "https://api.artic.edu/"

        /** Only used if a response omits config.iiif_url, which the docs say not to hardcode. */
        const val FALLBACK_IIIF_URL = "https://www.artic.edu/iiif/2"
        const val MAX_LIMIT = 100
        const val MAX_RESULTS = 1_000
    }
}

@Serializable
data class AicSearchResponse(
    val pagination: AicPagination = AicPagination(),
    val data: List<AicArtworkDto> = emptyList(),
    val config: AicConfig? = null,
)

@Serializable
data class AicArtworkResponse(
    val data: AicArtworkDto,
    val config: AicConfig? = null,
)

@Serializable
data class AicPagination(
    val total: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 0,
    @SerialName("current_page") val currentPage: Int = 1,
)

@Serializable
data class AicConfig(
    @SerialName("iiif_url") val iiifUrl: String? = null,
)

@Serializable
data class AicArtworkDto(
    val id: Long,
    val title: String? = null,
    @SerialName("artist_title") val artistTitle: String? = null,
    @SerialName("artist_display") val artistDisplay: String? = null,
    @SerialName("date_display") val dateDisplay: String? = null,
    @SerialName("image_id") val imageId: String? = null,
    @SerialName("is_public_domain") val isPublicDomain: Boolean? = null,
    val thumbnail: AicThumbnailDto? = null,
)

@Serializable
data class AicThumbnailDto(
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("alt_text") val altText: String? = null,
)
