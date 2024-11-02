package org.protonaosp.columbus.gates

import android.content.Context
import android.os.Handler
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import java.util.concurrent.Executor

class TelephonyActivity(context: Context, handler: Handler) : Gate(context, handler, 2) {
    private var callBlocked: Boolean = false

    inner class PhoneCallback : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            callBlocked = isCallBlocked(state)
            updateBlocking()
        }
    }

    private val phoneCallback: PhoneCallback = PhoneCallback()
    private val executor: Executor = context.mainExecutor

    private val telephonyManager: TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    fun isCallBlocked(state: Int): Boolean {
        return state != null && state == TelephonyManager.CALL_STATE_OFFHOOK
    }

    fun updateBlocking() {
        setBlocking(callBlocked)
    }

    override fun onActivate() {
        callBlocked = isCallBlocked(telephonyManager.getCallState())
        telephonyManager.registerTelephonyCallback(executor, phoneCallback)
        updateBlocking()
    }

    override fun onDeactivate() {
        telephonyManager.unregisterTelephonyCallback(phoneCallback)
    }
}
