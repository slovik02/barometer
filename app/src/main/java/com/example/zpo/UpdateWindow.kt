package com.example.zpo

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigDecimal
import java.math.RoundingMode

class PressureWindowActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var editTextUpper: EditText
    private lateinit var editTextLower: EditText
    private lateinit var submitButton: Button
    private lateinit var currentPressureTextView: TextView

    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.presure_window_layout)

        editTextUpper = findViewById(R.id.editTextTextUpper)
        editTextLower = findViewById(R.id.editTextLower)
        submitButton = findViewById(R.id.buttonWindow)
        currentPressureTextView = findViewById(R.id.currentPressureView)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        if (pressureSensor == null) {
            currentPressureTextView.text = "Barometer not available"
            Toast.makeText(this, "Your device doesn't have a pressure sensor", Toast.LENGTH_LONG).show()
        } else {
            sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_UI)
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("users").document(userId)
            .collection("wellbeing_surveys")
            .get()
            .addOnSuccessListener { result ->
                val goodPressures = result.filter { doc ->
                    doc.getString("feeling") == "Good" &&
                            doc.getString("headache") == "No pain" &&
                            doc.getString("sleepy") == "No"
                }.mapNotNull { doc ->
                    doc.getDouble("numeric_pressure")
                }

                if (goodPressures.size >= 3) {
                    val newLower = goodPressures.minOrNull() ?: 980.0
                    val newUpper = goodPressures.maxOrNull() ?: 1030.0
                    editTextLower.setText(String.format("%.1f", newLower))
                    editTextUpper.setText(String.format("%.1f", newUpper))
                } else {
                    Toast.makeText(this, "Not enough good data to suggest a new window", Toast.LENGTH_SHORT).show()
                }
            }

        submitButton.setOnClickListener {
            val upperText = editTextUpper.text.toString().replace(',', '.')
            val lowerText = editTextLower.text.toString().replace(',', '.')

            val upper = upperText.toDoubleOrNull()
            val lower = lowerText.toDoubleOrNull()

            if (upper == null || lower == null || lower >= upper) {
                Toast.makeText(this, "Invalid input values", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.collection("users").document(userId)
                .update(mapOf(
                    "upper" to upper,
                    "lower" to lower
                ))
                .addOnSuccessListener {
                    Toast.makeText(this, "Window updated!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, main_page::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update window", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            val pressure = event.values[0]
            val rounded = BigDecimal(pressure.toDouble()).setScale(2, RoundingMode.HALF_UP)
            currentPressureTextView.text = "$rounded hPa"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Możesz dodać obsługę zmian dokładności jeśli potrzebujesz
    }

    override fun onResume() {
        super.onResume()
        pressureSensor?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: run {
            currentPressureTextView.text = "Barometer not available"
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}