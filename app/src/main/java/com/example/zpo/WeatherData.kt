package com.example.zpo

data class WeatherData (
    /**
     * Data class representing a simplified snapshot of weather information.
     *
     * @property hour The time of the weather reading, formatted as a string (e.g., "14:00").
     * @property temperature The temperature in degrees Celsius.
     * @property condition A short string describing the weather condition (e.g., "Clear", "Rain").
     */
    val hour: String,
    val temperature: Float,
    val condition: String
)