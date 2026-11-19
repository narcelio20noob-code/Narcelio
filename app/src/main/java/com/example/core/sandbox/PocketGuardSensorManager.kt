package com.example.core.sandbox

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.core.model.PocketSensorReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.sqrt

class PocketGuardSensorManager(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val proximitySensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private val accelerometerSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _reading = MutableStateFlow(
        PocketSensorReading(
            hasHardwareProximity = proximitySensor != null,
        ),
    )
    val reading: StateFlow<PocketSensorReading> = _reading.asStateFlow()

    private var isListening = false

    fun start() {
        if (isListening) return
        isListening = true
        proximitySensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        accelerometerSensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        if (!isListening) return
        isListening = false
        sensorManager?.unregisterListener(this)
    }

    fun setSimulatedPocket(inPocket: Boolean) {
        _reading.update { it.copy(simulatedNear = inPocket) }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val sensorEvent = event ?: return
        when (sensorEvent.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val distance = sensorEvent.values.firstOrNull() ?: return
                val maxRange = proximitySensor?.maximumRange ?: 5f
                val isNear = distance < maxRange && distance < 3.0f
                _reading.update { it.copy(hardwareNear = isNear) }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                if (sensorEvent.values.size >= 3) {
                    val x = sensorEvent.values[0]
                    val y = sensorEvent.values[1]
                    val z = sensorEvent.values[2]
                    val magnitude = sqrt(x * x + y * y + z * z)
                    _reading.update { it.copy(lastAccelMagnitude = magnitude) }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
