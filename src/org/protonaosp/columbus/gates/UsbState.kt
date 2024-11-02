package org.protonaosp.columbus.gates

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Handler

class UsbState(context: Context, handler: Handler) : TransientGate(context, handler) {
    private var usbConnected: Boolean = false
    private val usbReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent == null) {
                    return
                }
                val connected: Boolean = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false)
                if (connected == usbConnected) return
                usbConnected = connected
                blockForMillis(GATE_DURATION)
            }
        }

    override fun onActivate() {
        val intentFilter: IntentFilter = IntentFilter(UsbManager.ACTION_USB_STATE)
        val receiver: Intent? = context.registerReceiver(null, intentFilter)
        if (receiver != null) {
            usbConnected = receiver!!.getBooleanExtra(UsbManager.USB_CONNECTED, false)
        }
        context.registerReceiver(usbReceiver, intentFilter)
    }

    override fun onDeactivate() {
        context.unregisterReceiver(usbReceiver)
    }
}
