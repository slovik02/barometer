package com.example.zpo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class main_page : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null
    private lateinit var pressureTextView: TextView
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)

        requestIgnoreBatteryOptimizations()
        pressureTextView = findViewById(R.id.atm_pres)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        if (pressureSensor == null) {
            pressureTextView.text = "Barometer not available"
        } else {
            sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_UI)
        }

        val items = mutableListOf(
            Item("Trends", "Monitor pressure trends", R.drawable.trend),
            Item("Well-being", "How do you feel at this moment? Monitor your response to the specific atmospheric pressure", R.drawable.well),
            Item("Information", "Gain information on how to deal with different pressure conditions", R.drawable.info)
        )

        val recyclerView = findViewById<RecyclerView>(R.id.RV)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = ItemAdapter(items) { selectedItem ->
            when (selectedItem.title) {
                "Trends" -> {
                    val intent = Intent(this, trends::class.java)
                    startActivity(intent)
                }
                "Well-being" -> {
                    val intent = Intent(this, Survey::class.java)
                    startActivity(intent)
                }
            }
        }

        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            val pressure = event.values[0]
            val roundedPressure = BigDecimal(pressure.toDouble()).setScale(2, RoundingMode.HALF_UP)
            pressureTextView.text = "$roundedPressure hPa"

            // Save the pressure value locally (SharedPreferences)
            getSharedPreferences("pressure_prefs", MODE_PRIVATE).edit()
                .putFloat("latest_pressure", roundedPressure.toFloat())
                .apply()

            sensorManager.unregisterListener(this)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Required override - do nothing
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = packageName
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }
}