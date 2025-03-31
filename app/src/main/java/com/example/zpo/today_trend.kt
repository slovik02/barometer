package com.example.zpo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
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
import java.util.Calendar

class today_trend : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_today_trend)

        val lineChart = findViewById<LineChart>(R.id.chart)

        // Generate mock pressure data
        val entries = generatePressureData()

        // Create dataset
        val dataSet = LineDataSet(entries, "Pressure (hPa)").apply {
            color = Color.BLACK
            valueTextColor = Color.BLACK
            setDrawCircles(true) // Show data points
            setDrawValues(false)  // Hide labels on points
        }

        // Apply dataset to chart
        lineChart.data = LineData(dataSet)

        // Configure X-Axis for hours
        lineChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            valueFormatter = TimeAxisFormatter()
        }

        // Hide grid lines & right Y-axis
        lineChart.axisRight.isEnabled = false
        lineChart.description.isEnabled = false

        // Set up marker
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

        lineChart.invalidate() // Refresh the chart
    }

    private fun generatePressureData(): List<Entry> {
        val entries = mutableListOf<Entry>()
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        for (i in 0..(currentHour * 2)) { // Every 30 minutes
            val hourFraction = i / 2f
            val pressure = 1000 + (Math.random() * 10) // Simulated pressure data (1000-1010 hPa)
            entries.add(Entry(hourFraction, pressure.toFloat()))
        }

        return entries
    }

    class TimeAxisFormatter : com.github.mikephil.charting.formatter.ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val hour = value.toInt()
            return String.format("%02d:00", hour)
        }
    }
}