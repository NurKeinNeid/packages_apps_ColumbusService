package org.protonaosp.columbus.actions

import android.content.Context
import android.os.PowerManager
import android.os.ServiceManager
import com.android.internal.statusbar.IStatusBarService

class NotificationAction(context: Context) : Action(context) {
    val service =
        IStatusBarService.Stub.asInterface(ServiceManager.getService(Context.STATUS_BAR_SERVICE))
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    override fun canRun() = pm.isInteractive()

    override fun canRunWhenScreenOff() = false

    override fun run() {
        service.togglePanel()
    }
}
