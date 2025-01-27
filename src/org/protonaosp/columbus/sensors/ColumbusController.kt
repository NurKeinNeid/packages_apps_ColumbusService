package org.protonaosp.columbus.sensors

import android.content.Context
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.util.SparseLongArray
import org.protonaosp.columbus.TAG
import org.protonaosp.columbus.actions.*
import org.protonaosp.columbus.gates.*

class ColumbusController(val context: Context, val sensor: ColumbusSensor, val handler: Handler) {
    interface GestureListener {
        fun onGestureDetected(sensor: ColumbusSensor, msg: Int)
    }

    private val lastTimestampMap = SparseLongArray()
    private var gestureListener: GestureListener? = null
    private val softGates =
        setOf(
            ChargingState(context, handler),
            UsbState(context, handler),
            ScreenTouch(context, handler),
            SystemKeyPress(context, handler),
        )
    private val softGateListener =
        object : Gate.Listener {
            override fun onGateChanged(gate: Gate) {}
        }
    private val gestureSensorListener =
        object : ColumbusSensor.Listener {
            override fun onGestureDetected(sensor: ColumbusSensor, msg: Int) {
                if (isThrottled(msg)) {
                    // Log.w(TAG, "Gesture $msg throttled")
                    return
                }
                if (blockingGate()) return
                gestureListener?.onGestureDetected(sensor, msg)
            }
        }

    init {
        sensor.setGestureListener(gestureSensorListener)
    }

    fun isThrottled(tapTiming: Int): Boolean {
        val throttleMs: Long =
            if (useApSensor(context)) {
                getApSensorThrottleMs(context)
            } else {
                500L
            }

        if (throttleMs == 0L) return false

        var currentMs: Long = SystemClock.uptimeMillis()
        var lastMs: Long = lastTimestampMap.get(tapTiming)
        lastTimestampMap.put(tapTiming, currentMs)
        return currentMs - lastMs <= throttleMs
    }

    fun setGestureListener(gestureListener: GestureListener) {
        this.gestureListener = gestureListener
    }

    fun startListening(): Boolean {
        if (!sensor.isListening()) {
            activateGates()
            sensor.startListening()
            return true
        }
        return false
    }

    fun stopListening(): Boolean {
        if (sensor.isListening()) {
            deactivateGates()
            sensor.stopListening()
            return true
        }
        return false
    }

    fun updateSensitivity(sensitivity: Float): Boolean {
        if (sensor.isListening()) {
            sensor.updateSensitivity(sensitivity)
            return true
        }
        return false
    }

    private fun activateGates() {
        softGates.forEach { it.registerListener(softGateListener) }
    }

    private fun deactivateGates() {
        softGates.forEach { it.unregisterListener(softGateListener) }
    }

    private fun blockingGate(): Boolean {
        for (it in softGates) {
            if (it.isBlocking()) {
                Log.d(TAG, "Blocked by gate")
                return true
            }
        }
        return false
    }
}
