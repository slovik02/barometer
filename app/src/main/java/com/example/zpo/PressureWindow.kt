package com.example.zpo

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PressureWindow : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.presure_window_layout)

        val editTextUpper = findViewById<EditText>(R.id.editTextTextUpper)
        val editTextLower = findViewById<EditText>(R.id.editTextLower)
        val buttonSubmit = findViewById<Button>(R.id.buttonWindow)

        editTextUpper.inputType = InputType.TYPE_CLASS_NUMBER
        editTextLower.inputType = InputType.TYPE_CLASS_NUMBER

        buttonSubmit.setOnClickListener {
            val upperValue = editTextUpper.text.toString().toIntOrNull()
            val lowerValue = editTextLower.text.toString().toIntOrNull()

            if (upperValue != null && lowerValue != null && upperValue > lowerValue) {
                val intent = Intent(this, main_page::class.java)
                intent.putExtra("UPPER_LIMIT", upperValue)
                intent.putExtra("LOWER_LIMIT", lowerValue)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Please enter valid limits (upper > lower)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
