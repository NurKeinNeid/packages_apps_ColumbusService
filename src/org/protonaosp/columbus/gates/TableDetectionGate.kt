package org.protonaosp.columbus.gates

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import kotlin.math.acos
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Loosely based on
// https://stackoverflow.com/questions/30948131/how-to-know-if-android-device-is-flat-on-table
class TableDetectionGate(context: Context, val handler: Handler) : Gate(context, handler, 2) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val sensorListener =
        object : SensorEventListener {
            private var isFlat: Boolean = false

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

            override fun onSensorChanged(event: SensorEvent?) {
                val values = event?.values ?: return

                // Movement
                var x = values[0]
                var y = values[1]
                var z = values[2]
                val norm = sqrt(x * x + y * y + (z * z).toDouble()).toFloat()

                // Normalize the accelerometer vector
                x /= norm
                y /= norm
                z /= norm
                val inclination = Math.toDegrees(acos(z.toDouble())).roundToInt()

                val isFlat = inclination < 3 || inclination > 177
                if (isFlat != this.isFlat) {
                    this.isFlat = isFlat
                    setBlocking(isFlat)
                }
            }
        }

    override fun onActivate() {
        sensorManager.registerListener(
            sensorListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL,
            handler,
        )
        setBlocking(false)
    }

    override fun onDeactivate() {
        sensorManager.unregisterListener(sensorListener)
    }
}
