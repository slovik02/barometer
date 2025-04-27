package com.example.zpo

import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class PressureWorker (context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val pressure = getSavedPressure() ?: return Result.failure()
        val firestore = FirebaseFirestore.getInstance()
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return Result.failure()

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
            .add(data)
            .addOnSuccessListener {
                Log.d("PressureWorker", "Data saved successfully")
            }
            .addOnFailureListener {
                Log.e("PressureWorker", "Error saving data", it)
            }

        //  Schedule next run
        WorkManager.getInstance(applicationContext).enqueue(
            OneTimeWorkRequestBuilder<PressureWorker>()
                .setInitialDelay(2, TimeUnit.MINUTES)
                .build()
        )

        return Result.success()
    }

    private fun getSavedPressure(): Float? {
        val prefs = applicationContext.getSharedPreferences("pressure_prefs", Context.MODE_PRIVATE)
        return prefs.getFloat("latest_pressure", -1f).takeIf { it >= 0 }
    }
}