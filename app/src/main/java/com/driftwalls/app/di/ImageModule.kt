package com.driftwalls.app.di

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageModule {
    @Provides
    @Singleton
    fun imageLoader(@ApplicationContext context: Context, client: OkHttpClient): ImageLoader {
        // Same client (AIC header, offline fallback) minus the HTTP cache: Coil keeps its own disk cache.
        val imageClient = client.newBuilder().cache(null).build()
        return ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { imageClient })) }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("images").toOkioPath())
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
