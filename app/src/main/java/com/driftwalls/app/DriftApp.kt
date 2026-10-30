package com.driftwalls.app

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DriftApp : Application(), SingletonImageLoader.Factory, Configuration.Provider {
    @Inject lateinit var imageLoader: Lazy<ImageLoader>
    @Inject lateinit var workerFactory: HiltWorkerFactory

    // Compose's AsyncImage and our wallpaper code share one loader and one disk cache.
    override fun newImageLoader(context: Context): ImageLoader = imageLoader.get()

    // On-demand WorkManager init (the default initializer is removed in the manifest) so workers get Hilt injection.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
