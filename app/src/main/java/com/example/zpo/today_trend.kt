package com.example.zpo

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class today_trend : AppCompatActivity() {
    private val TAG = "today_trend"
    private val db = FirebaseFirestore.getInstance()

    private val userEmail: String?
        get() = FirebaseAuth.getInstance().currentUser?.email

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_today_trend)

        val lineChart = findViewById<LineChart>(R.id.chart)

        fetchPressureData { entries ->
            Log.d(TAG, "Fetched ${entries.size} chart entries")

            if (entries.isEmpty()) {
                Log.w(TAG, "No pressure data available for today.")
            }

            val dataSet = LineDataSet(entries, "Pressure (hPa)").apply {
                color = Color.BLACK
                valueTextColor = Color.BLACK
                setDrawCircles(true)
                setDrawValues(false)
            }

            lineChart.data = LineData(dataSet)

            lineChart.xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                valueFormatter = TimeAxisFormatter()
            }

            lineChart.axisRight.isEnabled = false
            lineChart.description.isEnabled = false

            lineChart.marker = object : MarkerView(this, R.layout.marker_view) {
                @SuppressLint("MissingInflatedId")
                private val textView: TextView = findViewById(R.id.marker_text)

                override fun refreshContent(e: Entry?, highlight: Highlight?) {
                    e?.let {
                        val hour = it.x.toInt()
                        val minutes = if (it.x % 1 == 0f) "00" else "30"
                        val pressure = it.y
                        textView.text = String.format("%02d:%s - %.2f hPa", hour, minutes, pressure)
                    }
                    super.refreshContent(e, highlight)
                }

                override fun getOffset(): MPPointF {
                    return MPPointF((-width / 2).toFloat(), (-height).toFloat())
                }
            }

            lineChart.invalidate()
        }
    }

    private fun fetchPressureData(onComplete: (List<Entry>) -> Unit) {
        val email = userEmail
        if (email == null) {
            Log.e(TAG, "No user is logged in.")
            onComplete(emptyList())
            return
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val chartData = mutableMapOf<Float, MutableList<Float>>()

        Log.d(TAG, "Fetching data from Firestore path: $email/pressure/$today")

        db.collection(email)
            .document("pressure")
            .collection(today)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val docId = doc.id
                    val pressure = doc.getDouble("pressure")?.toFloat()

                    Log.d(TAG, "Document: $docId, Pressure: $pressure")

                    if (pressure != null) {
                        val timeSlot = convertTimeToHalfHourFloat(docId)

                        if (timeSlot >= 0f) {
                            chartData.getOrPut(timeSlot) { mutableListOf() }.add(pressure)
                        } else {
                            Log.w(TAG, "Skipped due to bad time format: $docId")
                        }
                    } else {
                        Log.w(TAG, "Skipped document without valid pressure: $docId")
                    }
                }

                val averagedEntries = chartData.map { (x, yList) ->
                    val avgPressure = yList.average().toFloat()
                    Entry(x, avgPressure)
                }.sortedBy { it.x }

                onComplete(averagedEntries)
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to fetch Firestore data", it)
                onComplete(emptyList())
            }
    }

    private fun convertTimeToHalfHourFloat(docId: String): Float {
        val timePart = docId.takeLast(5)
        val parts = timePart.split(":")
        if (parts.size < 2) {
            Log.w(TAG, "Invalid time format in ID: $docId")
            return -1f
        }

        val hour = parts[0].toIntOrNull()
        val minute = parts[1].toIntOrNull()
        if (hour == null || minute == null) {
            Log.w(TAG, "Non-numeric hour/minute in ID: $docId")
            return -1f
        }

        return when {
            minute < 15 -> hour.toFloat()
            minute < 45 -> hour + 0.5f
            else -> (hour + 1).toFloat()
        }
    }

    class TimeAxisFormatter : com.github.mikephil.charting.formatter.ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val hour = value.toInt()
            val minutes = if (value % 1 == 0f) "00" else "30"
            return String.format("%02d:%s", hour, minutes)
        }
    }
}