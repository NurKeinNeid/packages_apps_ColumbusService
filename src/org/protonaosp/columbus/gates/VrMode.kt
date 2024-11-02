package org.protonaosp.columbus.gates

import android.content.Context
import android.os.Handler
import android.os.RemoteException
import android.os.ServiceManager
import android.service.vr.IVrManager
import android.service.vr.IVrStateCallbacks
import android.util.Log
import org.protonaosp.columbus.TAG

class VrMode(context: Context, handler: Handler) : Gate(context, handler, 2) {
    private var inVrMode: Boolean = false
    private var vrManager: IVrManager? = null
    private val vrStateCallbacks =
        object : IVrStateCallbacks.Stub() {
            override fun onVrStateChanged(enabled: Boolean) {
                inVrMode = enabled
                updateBlocking()
            }
        }

    fun setVrManager() {
        val service = ServiceManager.getService(Context.VR_SERVICE) ?: return
        vrManager = IVrManager.Stub.asInterface(service)
    }

    init {
        setVrManager()
    }

    fun updateBlocking() {
        setBlocking(inVrMode)
    }

    override fun onActivate() {
        if (vrManager != null) {
            try {
                inVrMode = vrManager!!.getVrModeState()
                vrManager!!.registerListener(vrStateCallbacks)
            } catch (e: RemoteException) {
                Log.e(TAG, "Could not register IVrManager listener", e)
                inVrMode = false
            }
        }
        updateBlocking()
    }

    override fun onDeactivate() {
        try {
            if (vrManager != null) {
                vrManager!!.unregisterListener(vrStateCallbacks)
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "Could not unregister IVrManager listener", e)
        }
        inVrMode = false
    }
}
