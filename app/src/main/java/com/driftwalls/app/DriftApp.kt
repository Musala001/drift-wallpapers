package com.driftwalls.app

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
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

    override fun onCreate() {
        super.onCreate()
        publishShortcuts()
    }

    // Dynamic rather than static (shortcuts.xml), so it also works for the .debug application id.
    private fun publishShortcuts() {
        val next = ShortcutInfoCompat.Builder(this, ShortcutActivity.SHORTCUT_ID)
            .setShortLabel("Next wallpaper")
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_next))
            .setIntent(Intent(this, ShortcutActivity::class.java).setAction(Intent.ACTION_VIEW))
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(this, next)
    }

    // Compose's AsyncImage and our wallpaper code share one loader and one disk cache.
    override fun newImageLoader(context: Context): ImageLoader = imageLoader.get()

    // On-demand WorkManager init (the default initializer is removed in the manifest) so workers get Hilt injection.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
