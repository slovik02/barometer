package com.example.zpo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class weather : AppCompatActivity() {
    private lateinit var textTemperature: TextView
    private lateinit var textCondition: TextView
    private lateinit var textHumidity: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        val sampleWeatherData = listOf(
            WeatherData("08:00", 18f, "Clear"),
            WeatherData("09:00", 19f, "Clear"),
            WeatherData("10:00", 20f, "Clear"),
            WeatherData("11:00", 21f, "Clear"),
            WeatherData("12:00", 22f, "Clear"),
            WeatherData("13:00", 22f, "Clouds")
        )

        val chart = findViewById<LineChart>(R.id.weather_chart)
        setupWeatherChart(chart, sampleWeatherData, this)

        textTemperature = findViewById(R.id.temp_value)
        textCondition = findViewById(R.id.weather_value)
        textHumidity = findViewById(R.id.humidity_value)

        val latitude = 52.23
        val longitude = 21.01
        val apiKey = "d3d49b927b20dd11a7ee9d0926d830aa"

        val url = "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=$apiKey&units=metric"

        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
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
                if (jsonString != null) {
                    try {
                        val json = JSONObject(jsonString)
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
                        runOnUiThread {
                            textTemperature.text = "Parse error"
                            textCondition.text = "-"
                            textHumidity.text = "-"
                        }
                    }
                }
            }
        })
    }

    fun setupWeatherChart(chart: LineChart, weatherList: List<WeatherData>, context: Context) {
        val entries = ArrayList<Entry>()
        val icons = ArrayList<Drawable?>()
        val labels = ArrayList<String>()

        weatherList.forEachIndexed { index, data ->
            entries.add(Entry(index.toFloat(), data.temperature))

            // Add icons for each weather condition
            val resId = getWeatherIconResId(data.condition)
            val originalIcon = ContextCompat.getDrawable(context, resId)
            val resizedIcon = originalIcon?.let {
                val width = 10  // adjust as needed
                val height = 10
                it.setBounds(0, 0, width, height)
                it
            }
            icons.add(resizedIcon)

            labels.add(data.hour)
        }

        val dataSet = LineDataSet(entries, "Temperature")
        dataSet.setDrawIcons(true)
        dataSet.setDrawValues(false)
        dataSet.color = Color.YELLOW
        dataSet.lineWidth = 2f
        dataSet.setDrawCircles(true)
        dataSet.circleRadius = 4f
        dataSet.setCircleColor(Color.YELLOW)

        // Set icons
        entries.forEachIndexed { i, e -> e.icon = icons[i] }

        val lineData = LineData(dataSet)
        chart.data = lineData

        // Format X-Axis with hours
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            valueFormatter = IndexAxisValueFormatter(labels)
            textColor = Color.BLACK
            textSize = 12f
        }

        chart.axisRight.isEnabled = false
        chart.axisLeft.textColor = Color.BLACK
        chart.axisLeft.textSize = 12f

        chart.legend.isEnabled = false
        chart.description.isEnabled = false
        chart.setTouchEnabled(false)

        chart.invalidate() // Refresh chart
    }

    fun getWeatherIconResId(condition: String): Int {
        return when (condition.lowercase()) {
            "clear" -> R.drawable.sunny
            "clouds" -> R.drawable.clouds
            "rain" -> R.drawable.rain
            else -> R.drawable.sunny
        }
    }

}