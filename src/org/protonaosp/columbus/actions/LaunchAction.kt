package org.protonaosp.columbus.actions

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import org.protonaosp.columbus.R
import org.protonaosp.columbus.getDePrefs
import org.protonaosp.columbus.getLaunchActionApp
import org.protonaosp.columbus.getLaunchActionAppShortcut

class LaunchAction(context: Context) : Action(context) {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val app_default = context.getString(R.string.launch_app_default)

    override fun canRun() = pm.isInteractive()

    override fun canRunWhenScreenOff() = false

    private fun launchApp() {
        val prefs = context!!.getDePrefs()

        val packageName: String = prefs.getLaunchActionApp(context)
        val activity: String = prefs.getLaunchActionAppShortcut(context)
        val launchActivity: Boolean = activity != app_default

        if (packageName == app_default) return

        val intent: Intent? =
            if (launchActivity) {
                    Intent(Intent.ACTION_MAIN)?.apply { setClassName(packageName, activity) }
                } else {
                    context.packageManager.getLaunchIntentForPackage(packageName)
                }
                ?.apply { (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP) }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {}
    }

    override fun run() {
        launchApp()
    }
}
