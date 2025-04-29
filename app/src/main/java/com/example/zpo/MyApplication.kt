package com.example.zpo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MyApplication : Application() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        // Schedule the first PressureWorker when the app starts
        Log.d("MyApplication", "App created. Scheduling PressureWorker...")
        createNotificationChannel()
        scheduleInitialPressureWorker()

        val serviceIntent = Intent(this, PressureService::class.java)
        startForegroundService(serviceIntent)
    }

    private fun createNotificationChannel() {
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
        val periodicRequest = PeriodicWorkRequestBuilder<PressureWorker>(
            15, TimeUnit.MINUTES // 15 min is minimum for PeriodicWorker
        )
            .addTag("PressureWorker")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PressureWorkerChain",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }
}