package com.example.zpo

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PressureWindow : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.presure_window_layout)

        val editTextUpper = findViewById<EditText>(R.id.editTextTextUpper)
        val editTextLower = findViewById<EditText>(R.id.editTextLower)
        val buttonSubmit = findViewById<Button>(R.id.buttonWindow)

        editTextUpper.inputType = InputType.TYPE_CLASS_NUMBER
        editTextLower.inputType = InputType.TYPE_CLASS_NUMBER

        val email = intent.getStringExtra("EMAIL") ?: ""
        val password = intent.getStringExtra("PASSWORD") ?: ""

        buttonSubmit.setOnClickListener {
            val upperText = editTextUpper.text.toString()
            val lowerText = editTextLower.text.toString()

            if (upperText.isEmpty() || lowerText.isEmpty()) {
                Toast.makeText(this, "Please fill in both values", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val upperValue = upperText.toIntOrNull()
            val lowerValue = lowerText.toIntOrNull()

            if (upperValue == null || lowerValue == null) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (upperValue <= lowerValue) {
                Toast.makeText(this, "Upper limit must be greater than lower", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Tworzenie konta dopiero teraz
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
}
