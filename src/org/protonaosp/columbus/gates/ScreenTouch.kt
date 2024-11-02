package org.protonaosp.columbus.gates

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.DisplayMetrics
import android.view.Choreographer
import android.view.InputEvent
import android.view.InputEventReceiver
import android.view.MotionEvent
import com.android.systemui.shared.system.InputChannelCompat
import com.android.systemui.shared.system.InputMonitorCompat
import org.protonaosp.columbus.TAG

class ScreenTouch(context: Context, val handler: Handler) : Gate(context, handler, 2) {
    private val clearBlocking =
        object : Runnable {
            override fun run() {
                setBlocking(false)
            }
        }
    private val powerManager: PowerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val powerReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                refreshStatus()
            }
        }

    private var inputEventReceiver: InputChannelCompat.InputEventReceiver? = null
    private var inputMonitor: InputMonitorCompat? = null
    private val inputEventListener =
        object : InputChannelCompat.InputEventListener {
            override fun onInputEvent(ev: InputEvent) {
                if (ev == null || ev !is MotionEvent) {
                    return
                }
                val motionEvent: MotionEvent = ev as MotionEvent
                if (isABlockingTouch(motionEvent)) {
                    handler.removeCallbacks(clearBlocking)
                    setBlocking(true)
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP && isBlocking()) {
                    handler.postDelayed(clearBlocking, GATE_DURATION)
                }
            }
        }

    private val displayMetrics: DisplayMetrics = context.getResources().getDisplayMetrics()
    private val density: Float = displayMetrics.density * 32f
    private val touchRegion: RectF =
        RectF(
            density,
            density,
            displayMetrics.widthPixels.toFloat() - density,
            displayMetrics.heightPixels.toFloat() - density,
        )

    private fun dispose() {
        if (inputEventReceiver != null) {
            inputEventReceiver!!.dispose()
        }
        if (inputMonitor != null) {
            inputMonitor!!.dispose()
        }
    }

    private fun isABlockingTouch(motionEvent: MotionEvent): Boolean {
        return motionEvent.getAction() == MotionEvent.ACTION_DOWN &&
            touchRegion.contains(motionEvent.getRawX(), motionEvent.getRawY())
    }

    fun startListeningForTouch() {
        if (inputEventReceiver != null) return
        inputMonitor = InputMonitorCompat(TAG, 0)
        inputEventReceiver =
            inputMonitor!!.getInputReceiver(
                Looper.getMainLooper(),
                Choreographer.getInstance(),
                inputEventListener,
            )
    }

    fun stopListeningForTouch() {
        dispose()
        inputEventReceiver = null
        inputMonitor = null
    }

    fun refreshStatus() {
        if (powerManager.isInteractive()) {
            startListeningForTouch()
        } else {
            stopListeningForTouch()
        }
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
        stopListeningForTouch()
        context.unregisterReceiver(powerReceiver)
    }
}
