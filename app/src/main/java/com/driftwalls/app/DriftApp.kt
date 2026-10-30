package com.driftwalls.app

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DriftApp : Application(), SingletonImageLoader.Factory {
    @Inject lateinit var imageLoader: Lazy<ImageLoader>

    // Compose's AsyncImage and our wallpaper code share one loader and one disk cache.
    override fun newImageLoader(context: Context): ImageLoader = imageLoader.get()
}
