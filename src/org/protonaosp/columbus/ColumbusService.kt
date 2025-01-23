/*
 * Copyright (C) 2020 The Proton AOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package org.protonaosp.columbus

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import org.protonaosp.columbus.actions.*
import org.protonaosp.columbus.gates.*
import org.protonaosp.columbus.sensors.APSensor
import org.protonaosp.columbus.sensors.CHRESensor
import org.protonaosp.columbus.sensors.ColumbusController
import org.protonaosp.columbus.sensors.ColumbusSensor
import org.protonaosp.columbus.sensors.useApSensor

class ColumbusService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {
    // Services
    private lateinit var vibrator: Vibrator
    private lateinit var prefs: SharedPreferences
    private lateinit var action: Action
    private lateinit var sensor: ColumbusSensor
    private lateinit var controller: ColumbusController
    private lateinit var handler: Handler
    private lateinit var wakelock: PowerManager.WakeLock
    private var gates = setOf<Gate>()
    var isSettingsActivityOnTop: Boolean = false
    private val binder = Binder()

    // State
    private var screenRegistered = false

    // Settings
    private var enabled = true
    private var sensitivity = 0.03f
        set(value) {
            field = value
            if (enabled) {
                sendNewSensitivity()
            }
        }

    inner class Binder : android.os.Binder() {
        fun getService(): ColumbusService = this@ColumbusService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibrator = vibratorManager.defaultVibrator
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
        handler = Handler(Looper.getMainLooper())
        prefs = getDePrefs()

        Log.d(TAG, "Initializing Quick Tap gesture")

        if (useApSensor(this)) {
            Log.d(TAG, "Initializing AP Sensor")
            sensor = APSensor(this, sensitivity, handler)
        } else {
            Log.d(TAG, "Initializing CHRE Sensor")
            sensor = CHRESensor(this, sensitivity, handler)
        }
        controller = ColumbusController(this, sensor, handler)
        controller.setGestureListener(columbusControllerListener)
        gates =
            setOf(
                TelephonyActivity(this, handler),
                VrMode(this, handler),
                PocketDetectionGate(this, handler),
                TableDetectionGate(this, handler),
            )

        updateAction()
        updateSensitivity()
        updateEnabled()
        updateScreenCallback()

        // Only register for changes after initial pref updates
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        unregisterReceiver(screenCallback)
    }

    private fun createAction(key: String): Action {
        return when (key) {
            "screenshot" -> ScreenshotAction(this)
            "assistant" -> AssistantAction(this)
            "media" -> PlayPauseAction(this)
            "notifications" -> NotificationAction(this)
            "overview" -> RecentsAction(this)
            "camera" -> CameraAction(this)
            "power_menu" -> PowerMenuAction(this)
            "mute" -> MuteAction(this)
            "flashlight" -> FlashlightAction(this)
            "screen" -> ScreenAction(this)
            "launch" -> LaunchAction(this)

            else -> DummyAction(this)
        }
    }

    private fun updateSensitivity() {
        val value = prefs.getSensitivity(this)
        sensitivity =
            if (value <= 5) {
                value.toFloat() / 100f
            } else {
                (value - 5).toFloat() * 0.15f
            }
        Log.d(TAG, "Setting sensitivity to $sensitivity")
    }

    private fun updateAction() {
        val key = prefs.getAction(this)
        Log.d(TAG, "Setting action to $key")
        action = createAction(key)

        // For settings
        prefs
            .edit()
            .putBoolean(
                getString(R.string.pref_key_allow_screen_off_action_forced),
                !action.canRunWhenScreenOff(),
            )
            .apply()
    }

    private fun updateEnabled() {
        enabled = prefs.getEnabled(this)
        if (!enabled) {
            Log.d(TAG, "Disabling gesture by pref")
            deactivateGates()
            disableGesture()
            return
        }
        Log.d(TAG, "Enabling gesture by pref")
        activateGates()
        if (blockingGate()) {
            disableGesture()
        } else {
            enableGesture()
        }
    }

    private fun updateScreenCallback() {
        val allowScreenOff = prefs.getAllowScreenOff(this)

        // Listen if either condition *can't* run when screen is off
        if (!allowScreenOff || !action.canRunWhenScreenOff()) {
            val filter =
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_SCREEN_OFF)
                }

            if (!screenRegistered) {
                Log.d(TAG, "Listening to screen on/off events")
                registerReceiver(screenCallback, filter)
                screenRegistered = true
            }
        } else if (screenRegistered) {
            Log.d(TAG, "Stopped listening to screen on/off events")
            unregisterReceiver(screenCallback)
            screenRegistered = false
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        if (key == null) return
        when (key) {
            getString(R.string.pref_key_enabled) -> updateEnabled()
            getString(R.string.pref_key_sensitivity) -> updateSensitivity()
            // Action might change screen callback behavior
            getString(R.string.pref_key_action) -> {
                updateAction()
                updateScreenCallback()
            }
            getString(R.string.pref_key_allow_screen_off) -> updateScreenCallback()
        }
    }

    private fun enableGesture() {
        controller.startListening()
    }

    private fun disableGesture() {
        controller.stopListening()
    }

    private fun sendNewSensitivity() {
        controller.updateSensitivity(sensitivity)
    }

    private fun onGestureDetected(msg: Int) {
        if (msg != 1) return

        wakelock.acquire(2000L)

        if (isSettingsActivityOnTop) {
            vibrator.vibrate(vibDoubleTap, sonicAudioAttr)
            Toast.makeText(this, R.string.gesture_detected, Toast.LENGTH_SHORT).show()
            return
        }

        if (!action.canRun()) return

        vibrator.vibrate(vibDoubleTap, sonicAudioAttr)

        action.run()
    }

    private val columbusControllerListener =
        object : ColumbusController.GestureListener {
            override fun onGestureDetected(sensor: ColumbusSensor, msg: Int) {
                onGestureDetected(msg)
            }
        }

    private fun activateGates() {
        gates.forEach { it.registerListener(gateListener) }
    }

    private fun deactivateGates() {
        gates.forEach { it.unregisterListener(gateListener) }
    }

    private fun blockingGate(): Boolean {
        for (it in gates) {
            if (it.isBlocking()) {
                Log.d(TAG, "Blocked by gate")
                return true
            }
        }
        return false
    }

    private val gateListener =
        object : Gate.Listener {
            override fun onGateChanged(gate: Gate) {
                updateEnabled()
            }
        }

    private val screenCallback =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (enabled) {
                    when (intent?.action) {
                        Intent.ACTION_SCREEN_ON -> {
                            Log.d(TAG, "Enabling gesture due to screen on")
                            updateEnabled()
                        }
                        // Disable gesture entirely to save power
                        Intent.ACTION_SCREEN_OFF -> {
                            Log.d(TAG, "Disabling gesture due to screen off")
                            deactivateGates()
                            disableGesture()
                        }
                    }
                }
            }
        }

    companion object {
        // Vibration effects from HapticFeedbackConstants
        // Duplicated because we can't use performHapticFeedback in a background service
        private val vibDoubleTap: VibrationEffect =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            } else {
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            }

        private val sonicAudioAttr: AudioAttributes =
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
    }
}
