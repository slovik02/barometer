package com.example.zpo


import android.app.AlertDialog
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigDecimal
import java.math.RoundingMode

class Survey : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null
    private lateinit var pressureTextView: TextView

    private lateinit var radioGroup1: RadioGroup
    private lateinit var radioGroup2: RadioGroup
    private lateinit var radioGroup3: RadioGroup
    private lateinit var submitButton: Button

    private var currentPressure: String = "N/A"
    private var numericPressure: Double = -1.0

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.survey_layout)

        pressureTextView = findViewById(R.id.textViewPreasureShowing)
        radioGroup1 = findViewById(R.id.radioGroup1)
        radioGroup2 = findViewById(R.id.radioGroup2)
        radioGroup3 = findViewById(R.id.radioGroup3)
        submitButton = findViewById(R.id.button2submit)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        if (pressureSensor == null) {
            pressureTextView.text = "Barometer not available"
        } else {
            sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        submitButton.setOnClickListener {
            val feeling = getSelectedRadioText(radioGroup1)
            val headache = getSelectedRadioText(radioGroup2)
            val sleepy = getSelectedRadioText(radioGroup3)

            val userId = auth.currentUser?.uid

            if (userId == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val surveyData = hashMapOf(
                "feeling" to feeling,
                "headache" to headache,
                "sleepy" to sleepy,
                "pressure" to currentPressure,
                "numeric_pressure" to numericPressure,
                "timestamp" to FieldValue.serverTimestamp()
            )

            // Zapis do podkolekcji użytkownika
            db.collection("users").document(userId)
                .collection("wellbeing_surveys")
                .add(surveyData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Survey saved!", Toast.LENGTH_SHORT).show()
                    checkNegativeAnswersInPressureRange(userId)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun getSelectedRadioText(group: RadioGroup): String {
        val selectedId = group.checkedRadioButtonId
        return if (selectedId != -1) {
            findViewById<RadioButton>(selectedId).text.toString()
        } else {
            "Not answered"
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            val pressure = event.values[0]
            numericPressure = pressure.toDouble()
            val roundedPressure = BigDecimal(pressure.toDouble()).setScale(2, RoundingMode.HALF_UP)
            currentPressure = "$roundedPressure hPa"
            pressureTextView.text = currentPressure
            sensorManager.unregisterListener(this)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Required but unused
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    private fun checkNegativeAnswersInPressureRange(userId: String) {
        val userRef = db.collection("users").document(userId)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.contains("lower") && document.contains("upper")) {
                    val lower = document.getDouble("lower") ?: return@addOnSuccessListener
                    val upper = document.getDouble("upper") ?: return@addOnSuccessListener

                    userRef.collection("wellbeing_surveys")
                        .whereGreaterThanOrEqualTo("numeric_pressure", lower)
                        .whereLessThanOrEqualTo("numeric_pressure", upper)
                        .get()
                        .addOnSuccessListener { result ->
                            var negativeCount = 0
                            for (doc in result) {
                                val feeling = doc.getString("feeling") ?: ""
                                val headache = doc.getString("headache") ?: ""
                                val sleepy = doc.getString("sleepy") ?: ""

                                if (
                                    feeling == "Bad" ||
                                    headache == "Hurts bad" ||
                                    sleepy == "Yes"
                                ) {
                                    negativeCount++
                                }
                            }

                            if (negativeCount >= 5) {
                                AlertDialog.Builder(this)
                                    .setTitle("Sugerowana zmiana")
                                    .setMessage("Zaobserwowano 5 negatywnych wyników w Twoim zakresie dobrego ciśnienia. Czy chcesz zmodyfikować jego wartości?")
                                    .setPositiveButton("Tak") { _, _ ->
                                        // Można przejść do aktywności z edycją lub otworzyć formularz
                                    }
                                    .setNegativeButton("Nie", null)
                                    .show()
                            }
                        }
                }
            }
    }
}