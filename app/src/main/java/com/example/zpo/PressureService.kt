package com.example.zpo

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.*
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class PressureService : Service(), SensorEventListener {
    /**
     * A [Service] that continuously listens to atmospheric pressure changes using the pressure sensor.
     * It runs as a foreground service to ensure it stays alive in the background and persists across system restarts.
     *
     * Pressure readings are saved locally using SharedPreferences.
     */
    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null

    override fun onCreate() {
        /**
         * Called when the service is first created. Initializes the sensor and starts the foreground notification.
         */
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        startForegroundService()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        /**
         * Called if the system kills the service and it's removed from recent tasks.
         * This ensures the service is restarted automatically.
         *
         * @param rootIntent the last intent that was delivered to onStartCommand.
         */
        val restartServiceIntent = Intent(applicationContext, PressureService::class.java)
        restartServiceIntent.setPackage(packageName)
        applicationContext.startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }


    private fun startForegroundService() {
        /**
         * Starts the service in the foreground with a persistent notification.
         * Required to keep long-running services active in modern Android versions.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "pressure_service_channel",
                "Pressure Measurement",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "pressure_service_channel")
            .setContentTitle("Measuring Pressure")
            .setContentText("Running...")
            .setSmallIcon(R.drawable.add)
            .build()

        startForeground(2, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        /**
         * Called when the service is started. Registers the pressure sensor listener.
         *
         * @return [START_STICKY] to indicate that the system should restart the service if it's killed.
         */
        pressureSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        /**
         * Called when the service is destroyed. Unregisters the sensor listener to avoid memory leaks.
         */
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        /**
         * Called whenever new sensor data is available.
         * Saves the current pressure reading and timestamp to SharedPreferences.
         *
         * @param event the new sensor event.
         */
        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            val pressure = event.values[0]
            // save pressure + timestamp
            val prefs = getSharedPreferences("pressure_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putFloat("latest_pressure", pressure)
                .putLong("pressure_timestamp", System.currentTimeMillis())
                .apply()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        /**
         * Required override but not used. Called when sensor accuracy changes.
         */
        // Do nothing
    }
}