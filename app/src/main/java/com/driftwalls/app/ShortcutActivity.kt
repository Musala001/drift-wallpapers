package com.driftwalls.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.driftwalls.app.data.rotation.RotationScheduler
import com.driftwalls.app.data.rotation.RotationSettingsStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Invisible target of the "Next wallpaper" launcher shortcut: queues one rotation and closes. */
@AndroidEntryPoint
class ShortcutActivity : ComponentActivity() {
    @Inject lateinit var scheduler: RotationScheduler
    @Inject lateinit var settingsStore: RotationSettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            scheduler.rotateOnce(settingsStore.current())
            Toast.makeText(applicationContext, "Changing wallpaper…", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    companion object {
        const val SHORTCUT_ID = "next_wallpaper"
    }
}
