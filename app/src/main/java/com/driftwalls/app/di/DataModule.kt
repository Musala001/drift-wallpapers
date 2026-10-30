package com.driftwalls.app.di

import android.content.Context
import androidx.room.Room
import com.driftwalls.app.BuildConfig
import com.driftwalls.app.data.local.ArtworkDao
import com.driftwalls.app.data.local.DriftDatabase
import com.driftwalls.app.data.local.FavoriteDao
import com.driftwalls.app.data.local.RotationPickDao
import com.driftwalls.app.data.remote.ArtSource
import com.driftwalls.app.data.remote.aic.AicApi
import com.driftwalls.app.data.remote.aic.AicArtSource
import com.driftwalls.app.data.remote.met.MetApi
import com.driftwalls.app.data.remote.met.MetArtSource
import com.driftwalls.app.data.remote.net.AicUserAgentInterceptor
import com.driftwalls.app.data.remote.net.ApiCacheInterceptor
import com.driftwalls.app.data.remote.net.OfflineFallbackInterceptor
import com.driftwalls.app.data.remote.net.RateLimitInterceptor
import com.driftwalls.app.data.remote.net.RateLimiter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val AIC_API_HOST = "api.artic.edu"
    private const val MET_API_HOST = "collectionapi.metmuseum.org"

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun okHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val limiters = mapOf(
            // AIC allows 60 req/min anonymously; one per second never exceeds it.
            AIC_API_HOST to RateLimiter(permitsPerSecond = 1.0),
            // The Met allows 80 req/s; stay far below it.
            MET_API_HOST to RateLimiter(permitsPerSecond = 10.0, burst = 4),
        )
        return OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "http"), 20L * 1024 * 1024))
            .addInterceptor(OfflineFallbackInterceptor())
            .addInterceptor(AicUserAgentInterceptor(BuildConfig.API_USER_AGENT))
            .addNetworkInterceptor(RateLimitInterceptor(limiters))
            .addNetworkInterceptor(ApiCacheInterceptor(setOf(AIC_API_HOST, MET_API_HOST), maxAgeSeconds = 60 * 60))
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun metApi(client: OkHttpClient, json: Json): MetApi = retrofit(MetApi.BASE_URL, client, json).create(MetApi::class.java)

    @Provides
    @Singleton
    fun aicApi(client: OkHttpClient, json: Json): AicApi = retrofit(AicApi.BASE_URL, client, json).create(AicApi::class.java)

    /** Order sets how results interleave in the grid. */
    @Provides
    fun artSources(aic: AicArtSource, met: MetArtSource): List<@JvmSuppressWildcards ArtSource> = listOf(aic, met)

    private fun retrofit(baseUrl: String, client: OkHttpClient, json: Json) = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): DriftDatabase =
        Room.databaseBuilder(context, DriftDatabase::class.java, "drift.db").build()

    @Provides
    fun artworkDao(db: DriftDatabase): ArtworkDao = db.artworkDao()

    @Provides
    fun favoriteDao(db: DriftDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun rotationPickDao(db: DriftDatabase): RotationPickDao = db.rotationPickDao()
}
