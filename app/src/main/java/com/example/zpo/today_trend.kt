package com.example.zpo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.util.Calendar

class today_trend : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_today_trend)

        val lineChart = findViewById<LineChart>(R.id.chart)

        // Generate dummy pressure data
        val entries = generatePressureData()

        // Create LineDataSet
        val dataSet = LineDataSet(entries, "Pressure (hPa)")
        dataSet.color = Color.BLUE
        dataSet.valueTextColor = Color.BLACK
        dataSet.setDrawCircles(true)
        dataSet.setDrawValues(false)

        // Create LineData and set it to the chart
        val lineData = LineData(dataSet)
        lineChart.data = lineData

        // Customize X-Axis (hours of the day)
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = TimeAxisFormatter() // Format X-axis labels

        // Hide grid lines & right Y-axis
        lineChart.axisRight.isEnabled = false
        lineChart.description.isEnabled = false

        lineChart.invalidate() // Refresh the chart
    }

    private fun generatePressureData(): List<Entry> {
        val entries = mutableListOf<Entry>()
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        for (i in 0..(currentHour * 2)) { // Every 30 minutes
            val hourFraction = i / 2f // Convert to hours
            val pressure = 1000 + (Math.random() * 10) // Simulated pressure data (1000-1010 hPa)
            entries.add(Entry(hourFraction, pressure.toFloat()))
        }

        return entries
    }

    class TimeAxisFormatter : com.github.mikephil.charting.formatter.ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val hour = value.toInt()
            return String.format("%02d:00", hour) // Display as "00:00", "01:00", etc.
        }
    }
}