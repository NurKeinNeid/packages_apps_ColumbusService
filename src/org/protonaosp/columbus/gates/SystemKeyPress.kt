package org.protonaosp.columbus.gates

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.InputEvent
import android.view.InputEventReceiver
import android.view.KeyEvent
import com.android.systemui.shared.system.InputChannelCompat
import com.android.systemui.shared.system.InputMonitorCompat
import org.protonaosp.columbus.TAG

class SystemKeyPress(context: Context, handler: Handler) : TransientGate(context, handler) {
    private val blockingKeys =
        setOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_POWER)

    private var inputEventReceiver: InputChannelCompat.InputEventReceiver? = null
    private var inputMonitor: InputMonitorCompat? = null
    private val inputEventListener =
        object : InputChannelCompat.InputEventListener {
            override fun onInputEvent(ev: InputEvent) {
                if (ev == null || ev !is KeyEvent) {
                    return
                }
                if (isBlockingKeys(ev as KeyEvent)) {
                    blockForMillis(GATE_DURATION)
                }
            }
        }

    private fun dispose() {
        if (inputEventReceiver != null) {
            inputEventReceiver!!.dispose()
        }
        if (inputMonitor != null) {
            inputMonitor!!.dispose()
        }
    }

    private fun isBlockingKeys(keyEvent: KeyEvent): Boolean {
        return blockingKeys.contains(keyEvent.getKeyCode())
    }

    override fun onActivate() {
        if (inputEventReceiver != null) return
        inputMonitor = InputMonitorCompat(TAG, 0)
        inputEventReceiver =
            inputMonitor!!.getInputReceiver(
                Looper.getMainLooper(),
                Choreographer.getInstance(),
                inputEventListener,
            )
    }

    override fun onDeactivate() {
        dispose()
        inputEventReceiver = null
        inputMonitor = null
    }
}
