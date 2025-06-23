package com.example.zpo

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

class main_page : AppCompatActivity(), SensorEventListener {
    /**
     * Main activity of the app displaying pressure and navigation options.
     * Handles sensor data, user authentication, and service management.
     */
    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null
    private lateinit var pressureTextView: TextView
    private lateinit var logoutButton: ImageView
    private val handler = Handler(Looper.getMainLooper())

    private val LOCATION_PERMISSIONS = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private val REQUEST_LOCATION_PERMISSIONS_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        /**
         * Initializes the activity: sets up the sensor, UI components, location permissions,
         * and starts background services.
         */
        scheduleDailyAlarm()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)

        logoutButton = findViewById<ImageView>(R.id.logout)
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

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

        checkAndRequestLocationPermissions()
    }

    private fun scheduleDailyAlarm() {
        /**
         * Schedules a daily alarm at 08:30 that triggers [PressureNotificationReceiver].
         */
        val intent = Intent(this, PressureNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

    }

    override fun onResume() {
        /**
         * Re-registers the pressure sensor listener when the activity resumes.
         */
        super.onResume()
        sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        /**
         * Callback for sensor data changes. Updates the pressure reading and stores it locally.
         */
        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            val pressure = event.values[0]
            val roundedPressure = BigDecimal(pressure.toDouble()).setScale(2, RoundingMode.HALF_UP)
            pressureTextView.text = "$roundedPressure hPa"

            getSharedPreferences("pressure_prefs", MODE_PRIVATE).edit()
                .putFloat("latest_pressure", roundedPressure.toFloat())
                .apply()

            sensorManager.unregisterListener(this)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nic nie robimy
    }

    private fun requestIgnoreBatteryOptimizations() {
        /**
         * Requests system to ignore battery optimization for this app (required for background tasks).
         */
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


    private fun checkAndRequestLocationPermissions() {
        /**
         * Checks location permissions. If granted, starts [PressureService] in foreground.
         */
        val hasFineLocation = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            ActivityCompat.requestPermissions(this, LOCATION_PERMISSIONS, REQUEST_LOCATION_PERMISSIONS_CODE)
        } else {
            startPressureService()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        /**
         * Handles location permission result. Starts pressure service if granted.
         */
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSIONS_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startPressureService()
            } else {
                Toast.makeText(this, "Brak uprawnień do lokalizacji", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startPressureService() {
        /**
         * Starts [PressureService] as a foreground service for continuous pressure monitoring.
         */
        val intent = Intent(this, PressureService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}
