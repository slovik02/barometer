package com.example.zpo

data class WeatherData (
    val hour: String,        // e.g. "08:00"
    val temperature: Float,  // e.g. 22.5
    val condition: String
)