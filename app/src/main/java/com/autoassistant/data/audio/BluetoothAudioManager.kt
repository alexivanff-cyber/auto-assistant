package com.autoassistant.data.audio
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothAudioManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    fun isBluetoothHeadsetConnected(): Boolean {
        return try {
            val method = bluetoothAdapter?.javaClass?.getMethod("getProfileConnectionState", Int::class.java)
            (method?.invoke(bluetoothAdapter, BluetoothProfile.HEADSET) as? Int) == BluetoothProfile.STATE_CONNECTED
        } catch (e: Exception) { false }
    }
    fun enableSCO(): Boolean {
        if (!isBluetoothHeadsetConnected()) return false
        audioManager.startBluetoothSco()
        audioManager.isBluetoothScoOn = true
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        return true
    }
    fun disableSCO() {
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        audioManager.mode = AudioManager.MODE_NORMAL
    }
}
