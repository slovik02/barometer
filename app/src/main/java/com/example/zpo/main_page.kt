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
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

class main_page : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null
    private lateinit var pressureTextView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval: Long = 60 * 1000 // 1 minute

    private val updatePressureRunnable = object : Runnable {
        override fun run() {
            pressureSensor?.let {
                sensorManager.registerListener(this@main_page, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            handler.postDelayed(this, updateInterval)
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)
        schedulePressureUploadWorker()
        pressureTextView = findViewById(R.id.atm_pres)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        if (pressureSensor == null) {
            pressureTextView.text = "Barometer not available"
        } else {
            handler.post(updatePressureRunnable)
        }

        // Setup list of options
        val items = mutableListOf(
            Item("Trends", "Monitor pressure trends", R.drawable.trend),
            Item("Well-being", "How do you feel at this moment? Monitor your response to the specific atmospheric pressure", R.drawable.well),
            Item("Information", "Gain information on how to deal with different pressure conditions", R.drawable.info)
        )

        val recyclerView = findViewById<RecyclerView>(R.id.RV)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ItemAdapter(items) { selectedItem ->
            if (selectedItem.title == "Trends") {
                startActivity(Intent(this, trends::class.java))
            }
        }

//        // Schedule WorkManager for background Firestore upload every 30 minutes
//        val workRequest = PeriodicWorkRequestBuilder<PressureWorker>(15, TimeUnit.MINUTES).build()
//        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
//            "PressureUploader",
//            ExistingPeriodicWorkPolicy.KEEP,
//            workRequest
//        )
    }

    private fun schedulePressureUploadWorker() {
        val request = OneTimeWorkRequestBuilder<PressureWorker>()
            .setInitialDelay(2, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueue(request)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            val pressure = event.values[0]
            val roundedPressure = BigDecimal(pressure.toDouble()).setScale(2, RoundingMode.HALF_UP)
            pressureTextView.text = "$roundedPressure hPa"

            // Save pressure to SharedPreferences for Firestore upload
            getSharedPreferences("pressure_prefs", MODE_PRIVATE).edit()
                .putFloat("latest_pressure", roundedPressure.toFloat())
                .apply()

            // Unregister to avoid excessive reads
            sensorManager.unregisterListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(updatePressureRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updatePressureRunnable)
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Required override — leave empty
    }
}