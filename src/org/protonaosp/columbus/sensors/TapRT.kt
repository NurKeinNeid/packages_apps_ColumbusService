package org.protonaosp.columbus.sensors

import android.content.Context
import android.hardware.Sensor
import java.util.ArrayDeque
import java.util.ArrayList
import java.util.Deque
import org.protonaosp.columbus.actions.*

open class TapRT(val context: Context, val sizeWindowNsUpdate: Long, val heuristicMode: Boolean) :
    EventIMURT() {

    private val minTimeGapNs: Long = 100000000L
    private val maxTimeGapNs: Long = 500000000L
    private val frameAlignPeak: Int = 12
    private val framePriorPeak: Int = 6
    var result: Int = 0
    val positivePeakDetector: PeakDetector = PeakDetector()
    val negativePeakDetector: PeakDetector = PeakDetector()
    val timestampsBackTap: Deque<Long> = ArrayDeque()
    var wasPeakApproaching: Boolean = true

    init {
        tflite = TfClassifier(context.getAssets(), getModelFileName(context))
        sizeWindowNs = sizeWindowNsUpdate
        sizeFeatureWindow = 50
        numberFeature = 50 * 6
        lowpassAcc.setPara(1.0f)
        lowpassGyro.setPara(1.0f)
        highpassAcc.setPara(0.05f)
        highpassGyro.setPara(0.05f)
    }

    fun addToFeatureVector(points: ArrayDeque<Float>, max: Int, index: Int) {
        var idx = index
        var pIt: MutableIterator<Float> = points.iterator()
        var i: Int = 0
        while (pIt.hasNext()) {
            if (i < max) {
                pIt.next()
            } else {
                if (i >= sizeFeatureWindow + max) {
                    return
                }
                featureVector.set(idx, pIt.next())
                idx++
            }
            i++
        }
    }

    fun checkTapTiming(timestamp: Long): Int {
        var timestampFirst: MutableIterator<Long> = timestampsBackTap.iterator()
        while (timestampFirst.hasNext()) {
            if (timestamp - timestampFirst.next() > maxTimeGapNs) {
                timestampFirst.remove()
            }
        }
        if (timestampsBackTap.isEmpty()) {
            return 0
        }

        var timestampSecond: MutableIterator<Long> = timestampsBackTap.iterator()
        while (timestampSecond.hasNext()) {
            if (timestampsBackTap.last() - timestampSecond.next() > minTimeGapNs) {
                timestampsBackTap.clear()
                return 2
            }
        }

        return 1
    }

    fun processKeySignalHeuristic() {
        var update: Point3f =
            highpassAcc.update(
                lowpassAcc.update(
                    slopeAcc.update(
                        resampleAcc.results.point,
                        2400000.0f / resampleAcc.interval.toFloat(),
                    )
                )
            )
        positivePeakDetector.update(update.z.toFloat(), resampleAcc.results.time)
        negativePeakDetector.update(-update.z.toFloat(), resampleAcc.results.time)
        accZs.add(update.z.toFloat())
        val interval: Int = (sizeWindowNs / resampleAcc.interval).toInt()
        while (accZs.size > interval) {
            accZs.removeFirst()
        }
        if (accZs.size == interval) {
            recognizeTapHeuristic()
        }
        if (result == TapClass.Back.ordinal) {
            timestampsBackTap.addLast(resampleAcc.results.time)
        }
    }

    fun recognizeTapHeuristic() {
        val positvePeakId: Int = positivePeakDetector.peakId
        val negativePeakId: Int = negativePeakDetector.peakId - positvePeakId
        if (positvePeakId == 4) {
            featureVector = ArrayList(accZs)
            result =
                if (negativePeakId <= 0 || negativePeakId >= 3) {
                        TapClass.Others
                    } else {
                        TapClass.Back
                    }
                    .ordinal
        }
    }

    fun recognizeTapML() {
        var resultT: Int =
            ((resampleAcc.results.time - resampleGyro.results.time) / resampleAcc.interval).toInt()
        var positvePeakMax: Int = Math.max(0, positivePeakDetector.peakId)
        var negativePeakMax: Int = Math.max(0, negativePeakDetector.peakId)
        if (positivePeakDetector.amplitude <= negativePeakDetector.amplitude) {
            positvePeakMax = negativePeakMax
        }
        if (positvePeakMax > frameAlignPeak) {
            wasPeakApproaching = true
        }
        var accTuned: Int = positvePeakMax - framePriorPeak
        var gyroTuned: Int = accTuned - resultT
        var accSizeZ: Int = accZs.size
        if (accTuned < 0 || gyroTuned < 0) {
            return
        }
        var sizeFW: Int = sizeFeatureWindow
        if (
            accTuned + sizeFW > accSizeZ ||
                sizeFW + gyroTuned > accSizeZ ||
                !wasPeakApproaching ||
                positvePeakMax > frameAlignPeak
        ) {
            return
        }
        wasPeakApproaching = false
        addToFeatureVector(accXs, accTuned, 0)
        addToFeatureVector(accYs, accTuned, sizeFeatureWindow)
        addToFeatureVector(accZs, accTuned, sizeFeatureWindow * 2)
        addToFeatureVector(gyroXs, gyroTuned, sizeFeatureWindow * 3)
        addToFeatureVector(gyroYs, gyroTuned, sizeFeatureWindow * 4)
        addToFeatureVector(gyroZs, gyroTuned, sizeFeatureWindow * 5)
        var scaleGyroData: ArrayList<Float> = scaleGyroData(featureVector, 10.0f)
        featureVector = scaleGyroData
        var predict: ArrayList<ArrayList<Float>>? = tflite?.predict(scaleGyroData, 7)
        if (predict == null || predict.isEmpty()) {
            return
        }
        result = Util.getMaxId(predict!!.get(0))
    }

    fun reset(clearFv: Boolean) {
        super.reset()
        if (clearFv) {
            featureVector.clear()
        } else {
            featureVector = arrayListOf<Float>()
            for (i in 0 until numberFeature) {
                featureVector.add(0.0f)
            }
        }
    }

    fun updateAccAndPeakDetectors() {
        updateAcc()
        positivePeakDetector.update(accZs.last().toFloat(), resampleAcc.results.time)
        negativePeakDetector.update(-accZs.last().toFloat(), resampleAcc.results.time)
    }

    fun updateData(
        sensorType: Int,
        rawLastX: Float,
        rawLastY: Float,
        rawLastZ: Float,
        rawLastT: Long,
        interval: Long,
    ) {
        result = TapClass.Others.ordinal
        if (heuristicMode) {
            updateHeuristic(sensorType, rawLastX, rawLastY, rawLastZ, rawLastT, interval)
        } else {
            updateML(sensorType, rawLastX, rawLastY, rawLastZ, rawLastT, interval)
        }
    }

    fun updateHeuristic(
        sensorType: Int,
        rawLastX: Float,
        rawLastY: Float,
        rawLastZ: Float,
        rawLastT: Long,
        interval: Long,
    ) {
        if (sensorType == Sensor.TYPE_GYROSCOPE) {
            return
        }
        if (0L != syncTime) {
            while (resampleAcc.update(rawLastX, rawLastY, rawLastZ, rawLastT)) {
                processKeySignalHeuristic()
            }
        } else {
            syncTime = rawLastT
            resampleAcc.init(rawLastX, rawLastY, rawLastZ, rawLastT, interval)
            resampleAcc.resampledLastT = syncTime
            slopeAcc.init(resampleAcc.results.point)
        }
    }

    fun updateML(
        sensorType: Int,
        rawLastX: Float,
        rawLastY: Float,
        rawLastZ: Float,
        rawLastT: Long,
        interval: Long,
    ) {
        when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> {
                gotAcc = true
                if (syncTime == 0L) {
                    resampleAcc.init(rawLastX, rawLastY, rawLastZ, rawLastT, interval)
                }
                if (!gotGyro) return
            }
            Sensor.TYPE_GYROSCOPE -> {
                gotGyro = true
                if (syncTime == 0L) {
                    resampleGyro.init(rawLastX, rawLastY, rawLastZ, rawLastT, interval)
                }
                if (!gotAcc) return
            }
        }

        if (syncTime == 0L) {
            syncTime = rawLastT
            resampleAcc.resampledLastT = rawLastT
            resampleGyro.resampledLastT = syncTime
            slopeAcc.init(resampleAcc.results.point)
            slopeGyro.init(resampleGyro.results.point)
            lowpassAcc.init(Point3f(0.0f, 0.0f, 0.0f))
            lowpassGyro.init(Point3f(0.0f, 0.0f, 0.0f))
            highpassAcc.init(Point3f(0.0f, 0.0f, 0.0f))
            highpassGyro.init(Point3f(0.0f, 0.0f, 0.0f))
            return
        }

        when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> {
                while (resampleAcc.update(rawLastX, rawLastY, rawLastZ, rawLastT)) {
                    updateAccAndPeakDetectors()
                }
                return
            }
            Sensor.TYPE_GYROSCOPE -> {
                while (resampleGyro.update(rawLastX, rawLastY, rawLastZ, rawLastT)) {
                    updateGyro()
                    recognizeTapML()
                }
                if (result == TapClass.Back.ordinal) {
                    timestampsBackTap.addLast(rawLastT)
                }
                return
            }
        }
    }
}
