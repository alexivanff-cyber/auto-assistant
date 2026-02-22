package com.autoassistant.receiver
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.autoassistant.service.AssistantForegroundService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED, BluetoothAdapter.ACTION_STATE_CHANGED -> {
                // Логика запуска сервиса при подключении
            }
        }
    }
}
