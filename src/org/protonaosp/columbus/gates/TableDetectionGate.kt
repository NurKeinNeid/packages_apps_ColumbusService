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

// References:
// https://stackoverflow.com/questions/30948131/how-to-know-if-android-device-is-flat-on-table
// https://stackoverflow.com/questions/11175599/how-to-measure-the-tilt-of-the-phone-in-xy-plane-using-accelerometer-in-android/15149421#15149421
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
                var x = values[0].toDouble()
                var y = values[1].toDouble()
                var z = values[2].toDouble()
                val norm = sqrt(x * x + y * y + z * z)

                // Normalize the accelerometer vector
                z /= norm
                val inclination = Math.toDegrees(acos(z)).roundToInt()

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
