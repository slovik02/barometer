package com.example.zpo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PressureNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (document != null && document.contains("lower") && document.contains("upper")) {
                val lower = document.getDouble("lower") ?: return@addOnSuccessListener
                val upper = document.getDouble("upper") ?: return@addOnSuccessListener

                val sharedPrefs = context.getSharedPreferences("ZPO_PREFS", Context.MODE_PRIVATE)
                val lastPressure = sharedPrefs.getFloat("last_pressure", -1f).toDouble()

                if (lastPressure < lower || lastPressure > upper) {
                    sendNotification(context, lastPressure)
                }
            }
        }
    }

    private fun sendNotification(context: Context, pressure: Double) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "pressure_channel",
                "Powiadomienia o ciśnieniu",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "pressure_channel")
            .setContentTitle("Uwaga na ciśnienie!")
            .setContentText("Ciśnienie $pressure hPa może wpływać na Twoje samopoczucie.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1001, notification)
    }
}
