package com.devekene.muzik.playback

import android.content.Context
import com.devekene.muzik.data.local.MuzikDatabase
import com.devekene.muzik.data.local.SettingsDataStore
import com.devekene.muzik.data.repository.MuzikRepository
import com.devekene.muzik.data.scanner.LocalAudioScanner

object AudioEngineLocator {

    @Volatile
    private var repository: MuzikRepository? = null

    @Volatile
    private var equalizerManager: EqualizerManager? = null

    @Volatile
    private var bluetoothAudioManager: BluetoothAudioManager? = null

    @Volatile
    var crossfadeDurationMs: Int = 2000

    fun getRepository(context: Context): MuzikRepository {
        return repository ?: synchronized(this) {
            repository ?: run {
                val appContext = context.applicationContext
                val db = MuzikDatabase.getInstance(appContext)
                val scanner = LocalAudioScanner(appContext)
                val settings = SettingsDataStore(appContext)
                MuzikRepository(db.muzikDao(), scanner, settings).also { repository = it }
            }
        }
    }

    fun getEqualizerManager(): EqualizerManager {
        return equalizerManager ?: synchronized(this) {
            equalizerManager ?: EqualizerManager().also { equalizerManager = it }
        }
    }

    fun getBluetoothAudioManager(context: Context): BluetoothAudioManager {
        return bluetoothAudioManager ?: synchronized(this) {
            bluetoothAudioManager ?: BluetoothAudioManager(context.applicationContext).also {
                bluetoothAudioManager = it
            }
        }
    }
}
