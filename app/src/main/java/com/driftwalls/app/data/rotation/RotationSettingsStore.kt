package com.driftwalls.app.data.rotation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RotationSettingsStore @Inject constructor(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<RotationSettings> = dataStore.data.map { it.toSettings() }.distinctUntilChanged()

    suspend fun current(): RotationSettings = settings.first()

    /** Applies [transform] atomically and returns the new settings. */
    suspend fun update(transform: (RotationSettings) -> RotationSettings): RotationSettings {
        var updated = RotationSettings()
        dataStore.edit { prefs ->
            updated = transform(prefs.toSettings())
            prefs.write(updated)
        }
        return updated
    }

    private fun Preferences.toSettings(): RotationSettings {
        val defaults = RotationSettings()
        return RotationSettings(
            source = enumOr(this[SOURCE], defaults.source),
            category = enumOr(this[CATEGORY], defaults.category),
            order = enumOr(this[ORDER], defaults.order),
            target = enumOr(this[TARGET], defaults.target),
            enabled = this[ENABLED] ?: defaults.enabled,
            interval = enumOr(this[INTERVAL], defaults.interval),
            wifiOnly = this[WIFI_ONLY] ?: defaults.wifiOnly,
        )
    }

    private fun MutablePreferences.write(s: RotationSettings) {
        this[SOURCE] = s.source.name
        this[CATEGORY] = s.category.name
        this[ORDER] = s.order.name
        this[TARGET] = s.target.name
        this[ENABLED] = s.enabled
        this[INTERVAL] = s.interval.name
        this[WIFI_ONLY] = s.wifiOnly
    }

    private companion object {
        val SOURCE = stringPreferencesKey("source")
        val CATEGORY = stringPreferencesKey("category")
        val ORDER = stringPreferencesKey("order")
        val TARGET = stringPreferencesKey("target")
        val ENABLED = booleanPreferencesKey("enabled")
        val INTERVAL = stringPreferencesKey("interval")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")

        /** Unknown names (e.g. an enum entry removed in an update) fall back to the default. */
        inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
            enumValues<E>().firstOrNull { it.name == name } ?: default
    }
}
