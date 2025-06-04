package com.example.zpo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class WeatherWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {


    companion object {
        private const val CHANNEL_ID = "weather_upload_channel"
        private const val NOTIF_ID = 3
        private const val API_KEY = "d3d49b927b20dd11a7ee9d0926d830aa"
        private const val LAT = 52.23
        private const val LON = 21.01
        private const val TAG = "WeatherWorker"
    }

    override suspend fun doWork(): Result {
        createNotificationChannelIfNeeded()
        setForeground(createForegroundInfo())

        Log.d(TAG, "===== WeatherWorker started at: ${getCurrentFormattedTime()} =====")

        val url =
            "https://api.openweathermap.org/data/2.5/weather?lat=$LAT&lon=$LON&appid=$API_KEY&units=metric"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        return try {
            Log.d(TAG, "Making weather API request to: $url")
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Weather API request failed with code: ${response.code}")
                return Result.retry()
            }

            val jsonString = response.body?.string()
            if (jsonString == null) {
                Log.e(TAG, "Weather API response body is null")
                return Result.retry()
            }

            Log.d(TAG, "Weather API response received: $jsonString")

            val json = JSONObject(jsonString)
            val main = json.getJSONObject("main")
            val weather = json.getJSONArray("weather").getJSONObject(0)

            val temp = main.getDouble("temp")
            val humidity = main.getInt("humidity")
            val condition = weather.getString("main")

            Log.d(TAG, "Parsed weather data - Temp: $temp, Humidity: $humidity, Condition: $condition")

            saveWeatherLocally(temp, humidity, condition)

            val data = mapOf(
                "temp" to temp,
                "humidity" to humidity,
                "condition" to condition,
                "timestamp" to System.currentTimeMillis()
            )

            Log.d(TAG, "Uploading weather data to Firestore: $data")
            uploadWeatherData(data)

            Log.d(TAG, "===== WeatherWorker finished successfully at: ${getCurrentFormattedTime()} =====")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Exception in WeatherWorker: ", e)
            Result.retry()
        }
    }

    private fun getCurrentFormattedTime(): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    private fun saveWeatherLocally(temp: Double, humidity: Int, condition: String) {
        Log.d(TAG, "Saving weather data locally")
        val prefs = applicationContext.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("latest_temp", temp.toFloat())
            .putInt("latest_humidity", humidity)
            .putString("latest_condition", condition)
            .putLong("weather_timestamp", System.currentTimeMillis())
            .apply()
        Log.d(TAG, "Weather data saved locally")
    }

    private fun uploadWeatherData(data: Map<String, Any>) {
        val firestore = FirebaseFirestore.getInstance()
        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        if (userEmail == null) {
            Log.e(TAG, "User not authenticated. Skipping upload.")
            return
        }

        val now = Calendar.getInstance()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
        val docId = "$dateStr-$timeStr"

        firestore.collection(userEmail)
            .document("weather")
            .collection(dateStr)
            .document(docId)
            .set(data)
            .addOnSuccessListener {
                Log.d(TAG, "Weather data uploaded successfully: $docId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to upload weather data", e)
            }
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Weather Updates",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Weather Update")
            .setContentText("Fetching latest weather info...")
            .setSmallIcon(R.drawable.add)
            .build()
        return ForegroundInfo(NOTIF_ID, notification)
    }
}