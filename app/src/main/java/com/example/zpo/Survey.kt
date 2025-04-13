package com.example.zpo

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.math.BigDecimal
import java.math.RoundingMode

class Survey : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null
    private lateinit var pressureTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.survey_layout)

        pressureTextView = findViewById(R.id.textViewPreasureShowing)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        if (pressureSensor == null) {
            pressureTextView.text = "Barometer not available"
        } else {
            sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            val pressure = event.values[0]
            val roundedPressure = BigDecimal(pressure.toDouble()).setScale(2, RoundingMode.HALF_UP)
            pressureTextView.text = "$roundedPressure hPa"
            sensorManager.unregisterListener(this)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // wymagane, nawet jeśli puste
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}
