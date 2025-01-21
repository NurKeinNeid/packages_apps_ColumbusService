package org.protonaosp.columbus.gates

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventCallback
import android.hardware.SensorManager
import android.os.Handler
import android.os.PowerManager

/*
   This one is slightly odd as a sensor listener doesn't stay running in the background to allow for asynchronous listening.
   We therefore use a little bit of a hacky way of detecting - attach a listener on a background thread and then immediately block the main thread waiting for the event (which is almost instantaneous)
*/

class PocketDetectionGate(context: Context, val handler: Handler) : Gate(context, handler, 2) {
    private val powerManager: PowerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val powerReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                refreshStatus()
            }
        }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensorListener =
        object : SensorEventCallback() {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event!!.values[0] < proximityMax) {
                    setBlocking(true)
                } else {
                    setBlocking(false)
                }
            }
        }
    private val proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val proximityMax: Float = proximity!!.getMaximumRange()

    fun refreshStatus() {
        if (!powerManager.isInteractive()) {
            startListeningForPocket()
        } else {
            stopListeningForPocket()
        }
    }

    fun startListeningForPocket() {
        sensorManager.registerListener(
            sensorListener,
            proximity,
            SensorManager.SENSOR_DELAY_NORMAL,
            handler,
        )
    }

    fun stopListeningForPocket() {
        sensorManager.unregisterListener(sensorListener)
    }

    override fun onActivate() {
        val filter: IntentFilter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        context.registerReceiver(powerReceiver, filter)
        refreshStatus()
        setBlocking(false)
    }

    override fun onDeactivate() {
        stopListeningForPocket()
        context.unregisterReceiver(powerReceiver)
    }
}
