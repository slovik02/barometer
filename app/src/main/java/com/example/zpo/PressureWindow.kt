package com.example.zpo

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigDecimal
import java.math.RoundingMode

class PressureWindow : AppCompatActivity(), SensorEventListener {

    private lateinit var editTextUpper: EditText
    private lateinit var editTextLower: EditText
    private lateinit var buttonSubmit: Button
    private lateinit var currentPressureTextView: TextView

    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.presure_window_layout)

        editTextUpper = findViewById(R.id.editTextTextUpper)
        editTextLower = findViewById(R.id.editTextLower)
        buttonSubmit = findViewById(R.id.buttonWindow)
        currentPressureTextView = findViewById(R.id.currentPressureView)

        editTextUpper.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        editTextLower.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        // Sensor setup
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        if (pressureSensor == null) {
            currentPressureTextView.text = "Barometer not available"
            Toast.makeText(this, "Your device doesn't have a pressure sensor", Toast.LENGTH_LONG).show()
        } else {
            sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_UI)
        }

        val email = intent.getStringExtra("EMAIL") ?: ""
        val password = intent.getStringExtra("PASSWORD") ?: ""

        buttonSubmit.setOnClickListener {
            val upperText = editTextUpper.text.toString().replace(',', '.')
            val lowerText = editTextLower.text.toString().replace(',', '.')

            val upperValue = upperText.toDoubleOrNull()
            val lowerValue = lowerText.toDoubleOrNull()

            if (upperValue == null || lowerValue == null) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (upperValue <= lowerValue) {
                Toast.makeText(this, "Upper limit must be greater than lower", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val userId = authResult.user?.uid
                    val db = FirebaseFirestore.getInstance()

                    val pressureData = hashMapOf(
                        "upper" to upperValue,
                        "lower" to lowerValue
                    )

                    if (userId != null) {
                        db.collection("users").document(userId)
                            .set(pressureData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "User registered and data saved!", Toast.LENGTH_SHORT).show()
                                FirebaseAuth.getInstance().signOut()
                                startActivity(Intent(this, Login::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to save limits: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
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
        // Optional: log accuracy changes
    }

    override fun onResume() {
        super.onResume()
        pressureSensor?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}
