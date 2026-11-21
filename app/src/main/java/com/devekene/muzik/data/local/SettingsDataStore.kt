package com.devekene.muzik.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.devekene.muzik.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "muzik_settings")

data class UserAudioPreferences(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val useDynamicColor: Boolean = false,
    val eqEnabled: Boolean = true,
    val activeEqPresetName: String = "Studio Flat",
    val customBandLevelsMb: List<Int> = listOf(0, 0, 0, 0, 0),
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val crossfadeDurationMs: Int = 2000,
    val gaplessPlayback: Boolean = true,
    val bluetoothAutoResume: Boolean = true,
    val bluetoothAutoEq: Boolean = true,
    val bluetoothEqPresetName: String = "Acoustic Clarity",
    val lastPlayedTrackId: Long? = null
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val ACTIVE_EQ_PRESET = stringPreferencesKey("active_eq_preset")
        val BAND_LEVELS_CSV = stringPreferencesKey("band_levels_csv")
        val BASS_BOOST = intPreferencesKey("bass_boost")
        val VIRTUALIZER = intPreferencesKey("virtualizer")
        val CROSSFADE_MS = intPreferencesKey("crossfade_ms")
        val GAPLESS = booleanPreferencesKey("gapless")
        val BT_AUTO_RESUME = booleanPreferencesKey("bt_auto_resume")
        val BT_AUTO_EQ = booleanPreferencesKey("bt_auto_eq")
        val BT_EQ_PRESET = stringPreferencesKey("bt_eq_preset")
        val LAST_PLAYED_ID = longPreferencesKey("last_played_id")
    }

    val preferencesFlow: Flow<UserAudioPreferences> = context.settingsDataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.LIGHT.name
        val themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.LIGHT)
        val levelsCsv = prefs[Keys.BAND_LEVELS_CSV] ?: "0,0,0,0,0"
        val levels = levelsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
            .ifEmpty { listOf(0, 0, 0, 0, 0) }
        UserAudioPreferences(
            themeMode = themeMode,
            useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: false,
            eqEnabled = prefs[Keys.EQ_ENABLED] ?: true,
            activeEqPresetName = prefs[Keys.ACTIVE_EQ_PRESET] ?: "Studio Flat",
            customBandLevelsMb = levels,
            bassBoostStrength = prefs[Keys.BASS_BOOST] ?: 0,
            virtualizerStrength = prefs[Keys.VIRTUALIZER] ?: 0,
            crossfadeDurationMs = prefs[Keys.CROSSFADE_MS] ?: 2000,
            gaplessPlayback = prefs[Keys.GAPLESS] ?: true,
            bluetoothAutoResume = prefs[Keys.BT_AUTO_RESUME] ?: true,
            bluetoothAutoEq = prefs[Keys.BT_AUTO_EQ] ?: true,
            bluetoothEqPresetName = prefs[Keys.BT_EQ_PRESET] ?: "Acoustic Clarity",
            lastPlayedTrackId = prefs[Keys.LAST_PLAYED_ID]
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setEqSettings(
        enabled: Boolean,
        presetName: String,
        bandLevelsMb: List<Int>,
        bassBoost: Int,
        virtualizer: Int
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.EQ_ENABLED] = enabled
            prefs[Keys.ACTIVE_EQ_PRESET] = presetName
            prefs[Keys.BAND_LEVELS_CSV] = bandLevelsMb.joinToString(",")
            prefs[Keys.BASS_BOOST] = bassBoost
            prefs[Keys.VIRTUALIZER] = virtualizer
        }
    }

    suspend fun setCrossfadeDurationMs(durationMs: Int) {
        context.settingsDataStore.edit { it[Keys.CROSSFADE_MS] = durationMs }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.GAPLESS] = enabled }
    }

    suspend fun setBluetoothAutoResume(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.BT_AUTO_RESUME] = enabled }
    }

    suspend fun setBluetoothAutoEq(enabled: Boolean, presetName: String) {
        context.settingsDataStore.edit {
            it[Keys.BT_AUTO_EQ] = enabled
            it[Keys.BT_EQ_PRESET] = presetName
        }
    }

    suspend fun setLastPlayedTrackId(trackId: Long) {
        context.settingsDataStore.edit { it[Keys.LAST_PLAYED_ID] = trackId }
    }
}
