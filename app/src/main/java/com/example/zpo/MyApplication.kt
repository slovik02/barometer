package com.example.zpo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MyApplication : Application() {
    /**
     * Custom [Application] class that initializes background workers and services when the app starts.
     *
     * This class handles:
     * - Creating notification channels for foreground services.
     * - Scheduling periodic background tasks (e.g., pressure and weather workers).
     * - Starting a foreground service for continuous pressure monitoring.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        Log.d("MyApplication", "App created. Scheduling PressureWorker...")
        createNotificationChannel()
        scheduleInitialPressureWorker()
        scheduleWeatherWorker()

        val serviceIntent = Intent(this, PressureService::class.java)
        startForegroundService(serviceIntent)
    }

    private fun createNotificationChannel() {
        /**
         * Creates a notification channel required for foreground services on Android O and above.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "pressure_upload_channel",
                "Pressure Upload Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun scheduleInitialPressureWorker() {
        /**
         * Schedules a periodic [PressureWorker] to run every 30 minutes.
         *
         * Uses [ExistingPeriodicWorkPolicy.KEEP] to avoid duplicating scheduled work.
         */
        val periodicRequest = PeriodicWorkRequestBuilder<PressureWorker>(
            30, TimeUnit.MINUTES
        )
            .addTag("PressureWorker")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PressureWorkerChain",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    private fun scheduleWeatherWorker() {
        /**
         * Schedules a periodic [WeatherWorker] to run every 30 minutes.
         *
         * Also uses [ExistingPeriodicWorkPolicy.KEEP] to ensure only one worker chain is active.
         */
        val periodicRequest = PeriodicWorkRequestBuilder<WeatherWorker>(30, TimeUnit.MINUTES)
            .addTag("WeatherWorker")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeatherWorkerChain",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )

        Log.d("MyApplication", "WeatherWorker scheduled.")
    }

}