package com.example.zpo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Register : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_layout)

        val registerButton = findViewById<Button>(R.id.ButtonCreatAccount)
        val backToLoginButton = findViewById<Button>(R.id.buttonBackToLogIn)

        val intent = Intent(this, Login::class.java)

        registerButton.setOnClickListener {
            startActivity(intent)
        }

        backToLoginButton.setOnClickListener {
            startActivity(intent)
        }
    }
}
