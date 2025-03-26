package com.example.zpo

import android.annotation.SuppressLint
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.math.BigDecimal
import java.math.RoundingMode

class main_page : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null
    private lateinit var pressureTextView: TextView
    private val handler = Handler(Looper.getMainLooper()) // Handler for scheduling updates
    private val updateInterval: Long = 60 * 1000 // every minute update

    private val updatePressureRunnable = object : Runnable {
        override fun run() {
            pressureSensor?.let {
                sensorManager.registerListener(
                    this@main_page, it, SensorManager.SENSOR_DELAY_NORMAL
                )
            }
            handler.postDelayed(this, updateInterval) // Schedule the next update
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)

        // Initialize UI components
        pressureTextView = findViewById(R.id.atm_pres)

        // Initialize SensorManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        if (pressureSensor == null) {
            pressureTextView.text = "Barometer not available"
        } else {
            handler.post(updatePressureRunnable) // Start periodic updates
        }

        val items = mutableListOf(
            Item("Trends", "Monitor pressure trends", R.drawable.trend),
            Item("Well-being", "How do you feel at this moment? " +
                    "Monitor your response to the specific atmospheric pressure", R.drawable.well),
            Item("Information", "Gain information on how to deal " +
                    "with different pressure conditions", R.drawable.info)
        )

        val recyclerView = findViewById<RecyclerView>(R.id.RV)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = ItemAdapter(items) { selectedItem ->
            if (selectedItem.title == "Trends") {
                val intent = Intent(this, trends::class.java)
                startActivity(intent)
            }
        }

        recyclerView.adapter = adapter
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            val pressure = event.values[0] // Get pressure in hPa
            val roundedPressure = BigDecimal(pressure.toDouble()).setScale(2, RoundingMode.HALF_UP)
            pressureTextView.text = "$roundedPressure hPa"
            sensorManager.unregisterListener(this) // Stop listening after getting a value
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(updatePressureRunnable) // Resume updates when activity is visible
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updatePressureRunnable) // Stop updates when activity is paused
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //it has to be here, do not touch
    }
}