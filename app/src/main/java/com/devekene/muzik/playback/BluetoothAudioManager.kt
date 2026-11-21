package com.devekene.muzik.playback

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.devekene.muzik.data.model.AudioOutputDevice
import com.devekene.muzik.data.model.AudioOutputType
import com.devekene.muzik.data.model.BluetoothSyncState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BluetoothAudioManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(BluetoothSyncState())
    val state: StateFlow<BluetoothSyncState> = _state.asStateFlow()

    private val _bluetoothConnectedEvents = MutableSharedFlow<AudioOutputDevice>(extraBufferCapacity = 4)
    val bluetoothConnectedEvents: SharedFlow<AudioOutputDevice> = _bluetoothConnectedEvents.asSharedFlow()

    private var wasBluetoothConnected = false

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshDevices(emitConnectEvent = true)
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshDevices(emitConnectEvent = false)
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(deviceCallback, mainHandler)
        refreshDevices(emitConnectEvent = false)
    }

    fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun updateSyncPreferences(
        autoResumeOnConnect: Boolean,
        autoEqOnBluetooth: Boolean,
        bluetoothEqPresetName: String
    ) {
        _state.update {
            it.copy(
                autoResumeOnConnect = autoResumeOnConnect,
                autoEqOnBluetooth = autoEqOnBluetooth,
                bluetoothEqPresetName = bluetoothEqPresetName
            )
        }
    }

    fun refreshDevices(emitConnectEvent: Boolean = false) {
        val hasBtPerm = hasBluetoothConnectPermission()
        val rawOutputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val mapped = rawOutputs.mapNotNull { info -> mapAudioDevice(info) }
            .distinctBy { "${it.type}_${it.name}" }

        val activeDevice = selectPrimaryDevice(mapped)
        val normalized = mapped.map { device ->
            device.copy(isActive = activeDevice != null && device.id == activeDevice.id)
        }
        val btConnected = normalized.any { it.isBluetooth }
        val activeBt = normalized.firstOrNull { it.isBluetooth && it.isActive }
            ?: normalized.firstOrNull { it.isBluetooth }

        val eventMessage = when {
            emitConnectEvent && !wasBluetoothConnected && activeBt != null ->
                "Synced audio output to ${activeBt.name}"
            wasBluetoothConnected && !btConnected ->
                "Bluetooth disconnected • Playback paused safely"
            else -> _state.value.lastSyncEventMessage
        }

        if (emitConnectEvent && !wasBluetoothConnected && activeBt != null) {
            _bluetoothConnectedEvents.tryEmit(activeBt)
        }
        wasBluetoothConnected = btConnected

        _state.update {
            it.copy(
                activeDevice = activeDevice,
                connectedDevices = normalized,
                isBluetoothConnected = btConnected,
                hasBluetoothConnectPermission = hasBtPerm,
                lastSyncEventMessage = eventMessage
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getBondedBluetoothFallbackName(): String? {
        if (!hasBluetoothConnectPermission()) return null
        return runCatching {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            btManager?.adapter?.bondedDevices?.firstOrNull()?.name
        }.getOrNull()
    }

    private fun mapAudioDevice(info: AudioDeviceInfo): AudioOutputDevice? {
        val type = when (info.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> AudioOutputType.BLUETOOTH_A2DP
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_BLE_BROADCAST -> AudioOutputType.BLUETOOTH_LE
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET -> AudioOutputType.USB_DAC
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AudioOutputType.WIRED
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioOutputType.SPEAKER
            else -> return null
        }
        val isBt = type == AudioOutputType.BLUETOOTH_A2DP || type == AudioOutputType.BLUETOOTH_LE
        val rawName = info.productName?.toString()?.trim().orEmpty()
        val displayName = when {
            rawName.isNotBlank() && !rawName.equals(Build.MODEL, ignoreCase = true) -> rawName
            isBt -> getBondedBluetoothFallbackName() ?: type.label
            else -> type.label
        }

        val rates = info.sampleRates.toList().filter { it > 0 }.sorted()
            .ifEmpty { listOf(44100, 48000) }

        return AudioOutputDevice(
            id = info.id,
            name = displayName,
            type = type,
            isBluetooth = isBt,
            supportedSampleRates = rates,
            isActive = false
        )
    }

    private fun selectPrimaryDevice(devices: List<AudioOutputDevice>): AudioOutputDevice? {
        return devices.firstOrNull { it.type == AudioOutputType.BLUETOOTH_LE }
            ?: devices.firstOrNull { it.type == AudioOutputType.BLUETOOTH_A2DP }
            ?: devices.firstOrNull { it.type == AudioOutputType.USB_DAC }
            ?: devices.firstOrNull { it.type == AudioOutputType.WIRED }
            ?: devices.firstOrNull { it.type == AudioOutputType.SPEAKER }
            ?: devices.firstOrNull()
    }

    fun release() {
        runCatching { audioManager.unregisterAudioDeviceCallback(deviceCallback) }
    }
}
