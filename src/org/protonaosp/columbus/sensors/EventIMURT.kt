package org.protonaosp.columbus.sensors

import java.util.ArrayDeque
import java.util.ArrayList
import org.protonaosp.columbus.actions.*

open class EventIMURT {
    var numberFeature: Int = 300
    var sizeFeatureWindow: Int = 50
    var sizeWindowNs: Long = 0L

    var featureVector: ArrayList<Float> = arrayListOf<Float>()
    val accXs: ArrayDeque<Float> = ArrayDeque<Float>()
    val accYs: ArrayDeque<Float> = ArrayDeque<Float>()
    val accZs: ArrayDeque<Float> = ArrayDeque<Float>()
    val gyroXs: ArrayDeque<Float> = ArrayDeque<Float>()
    val gyroYs: ArrayDeque<Float> = ArrayDeque<Float>()
    val gyroZs: ArrayDeque<Float> = ArrayDeque<Float>()
    var gotAcc: Boolean = false
    var gotGyro: Boolean = false
    var syncTime: Long = 0L
    val resampleAcc: Resample3C = Resample3C()
    val resampleGyro: Resample3C = Resample3C()
    val slopeAcc: Slope3C = Slope3C()
    val slopeGyro: Slope3C = Slope3C()
    val lowpassAcc: Lowpass3C = Lowpass3C()
    val lowpassGyro: Lowpass3C = Lowpass3C()
    val highpassAcc: Highpass3C = Highpass3C()
    val highpassGyro: Highpass3C = Highpass3C()
    var tflite: TfClassifier? = null

    fun updateAcc() {
        var update: Point3f =
            highpassAcc.update(
                lowpassAcc.update(
                    slopeAcc.update(
                        resampleAcc.results.point,
                        2400000.0f / resampleAcc.interval.toFloat(),
                    )
                )
            )
        accXs.add(update.x.toFloat())
        accYs.add(update.y.toFloat())
        accZs.add(update.z.toFloat())
        val interval: Int = (sizeWindowNs / resampleAcc.interval).toInt()
        while (accXs.size > interval) {
            accXs.removeFirst()
            accYs.removeFirst()
            accZs.removeFirst()
        }
    }

    fun updateGyro() {
        var update: Point3f =
            highpassGyro.update(
                lowpassGyro.update(
                    slopeGyro.update(
                        resampleGyro.results.point,
                        2400000.0f / resampleGyro.interval.toFloat(),
                    )
                )
            )
        gyroXs.add(update.x.toFloat())
        gyroYs.add(update.y.toFloat())
        gyroZs.add(update.z.toFloat())
        val interval: Int = (sizeWindowNs / resampleGyro.interval).toInt()
        while (gyroXs.size > interval) {
            gyroXs.removeFirst()
            gyroYs.removeFirst()
            gyroZs.removeFirst()
        }
    }

    fun reset() {
        accXs.clear()
        accYs.clear()
        accZs.clear()
        gyroXs.clear()
        gyroYs.clear()
        gyroZs.clear()
        gotAcc = false
        gotGyro = false
        syncTime = 0L
    }

    fun scaleGyroData(input: ArrayList<Float>, scale: Float): ArrayList<Float> {
        for (size in (input.size / 2) until input.size) {
            input.set(size, (input.get(size) * scale))
        }
        return input
    }
}
