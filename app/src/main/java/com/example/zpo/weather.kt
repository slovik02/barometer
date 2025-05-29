package com.example.zpo

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
}