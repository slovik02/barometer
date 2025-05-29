package com.example.zpo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class trends : AppCompatActivity() {
    private lateinit var today_button: ConstraintLayout
    private lateinit var weather_but: ConstraintLayout

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_trends)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        today_button = findViewById(R.id.today_layout)
        today_button.setOnClickListener {
            homeToday()
        }

        weather_but = findViewById(R.id.weather_button)
        weather_but.setOnClickListener {
            homeWeather()
        }

    }

    private fun homeToday(){
        val intent = Intent(this, today_trend::class.java)
        startActivity(intent)
    }

    private fun homeWeather(){
        val intent = Intent(this, weather::class.java)
        startActivity(intent)
    }

}