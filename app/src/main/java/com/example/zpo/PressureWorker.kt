package com.example.zpo


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume



class PressureWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "PressureWorker"
        private const val CHANNEL_ID = "pressure_upload_channel"
    }

    override suspend fun doWork(): Result {
        createNotificationChannelIfNeeded()
        setForeground(createForegroundInfo())

        Log.d(TAG, "Worker started at: ${getCurrentFormattedTime()}")

        val pressure = readSensorDirectly()
        if (pressure != null) {
            savePressureLocally(pressure)
            uploadPressureData(pressure)
        } else {
            Log.w(TAG, "Failed to get fresh pressure reading.")
        }

        return Result.success()
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pressure Upload Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Pressure Upload")
            .setContentText("Uploading atmospheric pressure data...")
            .setSmallIcon(R.drawable.add)
            .build()

        return ForegroundInfo(1, notification)
    }

    private suspend fun readSensorDirectly(): Float? = suspendCancellableCoroutine { cont ->
        val sensorManager = applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        if (pressureSensor == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_PRESSURE) {
                    cont.resume(event.values[0])
                    sensorManager.unregisterListener(this)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL)

        // Fallback: timeout in 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            sensorManager.unregisterListener(listener)
            if (cont.isActive) cont.resume(null)
        }, 3000)
    }

    private fun savePressureLocally(pressure: Float) {
        val prefs = applicationContext.getSharedPreferences("pressure_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("latest_pressure", pressure)
            .putLong("pressure_timestamp", System.currentTimeMillis())
            .apply()
    }

    private fun uploadPressureData(pressure: Float) {
        Log.d(TAG, "Uploading pressure: $pressure hPa")

        val firestore = FirebaseFirestore.getInstance()
        val userEmail = FirebaseAuth.getInstance().currentUser?.email

        if (userEmail == null) {
            Log.e(TAG, "User not authenticated. Skipping upload.")
            return
        }

        val now = Calendar.getInstance()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)

        val data = mapOf(
            "pressure" to pressure,
            "time" to timeStr
        )

        val docId = "$dateStr-$timeStr"

        firestore.collection(userEmail)
            .document("pressure")
            .collection(dateStr)
            .document(docId)
            .set(data)
            .addOnSuccessListener {
                Log.d(TAG, "Pressure data uploaded successfully: $docId")
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to upload pressure data.", it)
            }
    }

    private fun getCurrentFormattedTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().time)
    }
}