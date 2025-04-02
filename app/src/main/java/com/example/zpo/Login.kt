package com.example.zpo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_layout)

        val loginButton = findViewById<Button>(R.id.ButtonLogIn)
        val registerButton = findViewById<Button>(R.id.buttonRegister)

        // Obsługa kliknięcia przycisku Log in
        loginButton.setOnClickListener {
            val intent = Intent(this, main_page::class.java)
            startActivity(intent)
        }

        // Obsługa kliknięcia przycisku Register
        registerButton.setOnClickListener {
            val intent = Intent(this, Register::class.java) // Upewnij się, że masz klasę Register
            startActivity(intent)
        }
    }
}
