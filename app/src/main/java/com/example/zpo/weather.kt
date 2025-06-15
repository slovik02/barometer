package com.example.zpo

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight


class weather : AppCompatActivity() {

    private lateinit var imageConditionIcon: ImageView
    private lateinit var textConditionSummary: TextView
    private lateinit var textTemperature: TextView
    private lateinit var textCondition: TextView
    private lateinit var textHumidity: TextView
    private lateinit var chart: LineChart
    private lateinit var db: FirebaseFirestore

    private val userEmail: String? get() = FirebaseAuth.getInstance().currentUser?.email

    companion object {
        private const val API_KEY = "d3d49b927b20dd11a7ee9d0926d830aa"
        private const val LATITUDE = 52.23
        private const val LONGITUDE = 21.01
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        textTemperature = findViewById(R.id.temp_value)
        textCondition = findViewById(R.id.weather_value)
        textHumidity = findViewById(R.id.humidity_value)
        chart = findViewById(R.id.weather_chart)

        db = FirebaseFirestore.getInstance()

        textConditionSummary = findViewById(R.id.summary)
        fetchDominantCondition()

        imageConditionIcon = findViewById(R.id.condition_icon)
        fetchDominantCondition()


        fetchCurrentWeather()
        fetchTemperatureData { tempEntries, humidityEntries ->
            if (tempEntries.isEmpty()) {
                Log.w(TAG, "No temperature data available to display.")
            }
            displayChart(tempEntries, humidityEntries)
        }

    }

    private fun fetchDominantCondition() {
        val email = userEmail
        if (email == null) {
            Log.e(TAG, "No user is logged in.")
            return
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val conditionCount = mutableMapOf<String, Int>()

        db.collection(email)
            .document("weather")
            .collection(today)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val condition = doc.getString("condition") ?: continue
                    val key = when {
                        condition.contains("clear", ignoreCase = true) -> "Clear"
                        condition.contains("rain", ignoreCase = true) -> "Rain"
                        condition.contains("cloud", ignoreCase = true) -> "Clouds"
                        else -> "Other"
                    }
                    conditionCount[key] = conditionCount.getOrDefault(key, 0) + 1
                }

                val dominantCondition = conditionCount.maxByOrNull { it.value }?.key

                val (summaryText, imageRes) = when (dominantCondition) {
                    "Clear" -> "There are no clouds and rain today. " to R.drawable.sunny
                    "Rain" -> "It is rainy today. Remember to take your umbrella!" to R.drawable.rain
                    "Clouds" -> "It is cloudy today." to R.drawable.clouds
                    else -> "Weather condition unclear" to R.drawable.clouds // fallback
                }

                textConditionSummary.text = summaryText
                imageConditionIcon.setImageResource(imageRes)
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to fetch condition data", it)
                textConditionSummary.text = "Error loading condition"
            }
    }



    private fun fetchCurrentWeather() {
        val url =
            "https://api.openweathermap.org/data/2.5/weather?lat=$LATITUDE&lon=$LONGITUDE&appid=$API_KEY&units=metric"

        OkHttpClient().newCall(Request.Builder().url(url).build())
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Weather API call failed", e)
                    runOnUiThread {
                        textTemperature.text = "Failed"
                        textCondition.text = "-"
                        textHumidity.text = "-"
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        runOnUiThread {
                            textTemperature.text = "Error ${response.code}"
                            textCondition.text = "-"
                            textHumidity.text = "-"
                        }
                        return
                    }

                    val jsonString = response.body?.string()
                    try {
                        val json = JSONObject(jsonString ?: return)
                        val main = json.getJSONObject("main")
                        val weather = json.getJSONArray("weather").getJSONObject(0)

                        val temp = main.getDouble("temp")
                        val humidity = main.getInt("humidity")
                        val condition = weather.getString("main")

                        runOnUiThread {
                            textTemperature.text = "$temp°C"
                            textCondition.text = condition
                            textHumidity.text = "$humidity%"
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse weather JSON", e)
                        runOnUiThread {
                            textTemperature.text = "Parse error"
                            textCondition.text = "-"
                            textHumidity.text = "-"
                        }
                    }
                }
            })
    }

    private fun fetchTemperatureData(onComplete: (List<Entry>, List<Entry>) -> Unit) {
        val email = userEmail
        if (email == null) {
            Log.e(TAG, "No user is logged in.")
            onComplete(emptyList(), emptyList())
            return
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val tempData = mutableMapOf<Float, MutableList<Float>>()
        val humidityData = mutableMapOf<Float, MutableList<Float>>()

        db.collection(email)
            .document("weather")
            .collection(today)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val docId = doc.id
                    val temp = doc.getDouble("temp")?.toFloat()
                    val humidity = doc.getDouble("humidity")?.toFloat()
                    val timeSlot = convertTimeToHalfHourFloat(docId)

                    if (timeSlot >= 0f) {
                        temp?.let { tempData.getOrPut(timeSlot) { mutableListOf() }.add(it) }
                        humidity?.let { humidityData.getOrPut(timeSlot) { mutableListOf() }.add(it) }
                    } else {
                        Log.w(TAG, "Invalid time in doc: $docId")
                    }
                }

                val tempEntries = tempData.map { (x, yList) -> Entry(x, yList.average().toFloat()) }.sortedBy { it.x }
                val humidityEntries = humidityData.map { (x, yList) -> Entry(x, yList.average().toFloat()) }.sortedBy { it.x }

                onComplete(tempEntries, humidityEntries)
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to fetch weather data", it)
                onComplete(emptyList(), emptyList())
            }
    }


    private fun displayChart(tempEntries: List<Entry>, humidityEntries: List<Entry>) {
        val tempDataSet = LineDataSet(tempEntries, "Temperature (°C)").apply {
            color = Color.RED
            valueTextColor = Color.RED
            setDrawCircles(true)
            setDrawValues(false)
        }

        val humidityDataSet = LineDataSet(humidityEntries, "Humidity (%)").apply {
            color = Color.BLUE
            valueTextColor = Color.BLUE
            setDrawCircles(true)
            setDrawValues(false)
        }

        chart.data = LineData(tempDataSet, humidityDataSet)

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            valueFormatter = TimeAxisFormatter()
            textColor = Color.BLACK
        }

        chart.axisLeft.textColor = Color.BLACK
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false

        chart.marker = CustomMarkerView(this)
        chart.invalidate()
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

    class TimeAxisFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val hour = value.toInt()
            val minutes = if (value % 1 == 0f) "00" else "30"
            return String.format("%02d:%s", hour, minutes)
        }
    }

    class CustomMarkerView(context: Context) : MarkerView(context, R.layout.marker_view) {
        private val textView: TextView = findViewById(R.id.marker_text)

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            e?.let {
                val hour = it.x.toInt()
                val minutes = if (it.x % 1 == 0f) "00" else "30"
                val value = it.y
                val label = highlight?.dataSetIndex?.let {
                    if (it == 0) "Temp" else "Humidity"
                } ?: "Value"
                textView.text = String.format("%02d:%s - %s: %.2f", hour, minutes, label, value)
            }
            super.refreshContent(e, highlight)
        }
    }

}