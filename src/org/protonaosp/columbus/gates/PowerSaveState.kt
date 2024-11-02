package org.protonaosp.columbus.gates

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.PowerManager
import android.os.PowerManager.ServiceType

class PowerSaveState(context: Context, handler: Handler) : Gate(context, handler, 2) {
    private var batterySaverEnabled: Boolean = false
    private var isDeviceInteractive: Boolean = false
    private val powerManager: PowerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val powerReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                refreshStatus()
            }
        }

    fun refreshStatus() {
        val state: android.os.PowerSaveState =
            powerManager.getPowerSaveState(PowerManager.ServiceType.OPTIONAL_SENSORS)
        batterySaverEnabled =
            if (powerManager == null || state == null) {
                false
            } else {
                state.batterySaverEnabled
            }
        isDeviceInteractive =
            if (powerManager == null) {
                false
            } else {
                powerManager.isInteractive()
            }
        setBlocking(batterySaverEnabled && !isDeviceInteractive)
    }

    override fun onActivate() {
        val intentFilter: IntentFilter =
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED).apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        context.registerReceiver(powerReceiver, intentFilter)
        refreshStatus()
    }

    override fun onDeactivate() {
        context.unregisterReceiver(powerReceiver)
    }
}
