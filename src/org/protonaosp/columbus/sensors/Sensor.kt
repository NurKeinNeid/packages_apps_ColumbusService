package org.protonaosp.columbus.sensors

interface Sensor {
    fun isListening(): Boolean

    fun startListening()

    fun stopListening()
}
