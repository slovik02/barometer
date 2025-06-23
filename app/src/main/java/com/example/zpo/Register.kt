package com.example.zpo

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Aktywność rejestracji użytkownika.
 * Umożliwia podanie adresu e-mail oraz hasła, a następnie przekierowuje do konfiguracji ciśnienia.
 * Dane nie są jeszcze zapisywane w Firebase – zostaną zapisane dopiero w `PressureWindow`.
 */
class Register : AppCompatActivity() {
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var inputPasswordRepeat: EditText
    private lateinit var registerButton: Button
    private lateinit var backToLoginButton: Button

    /**
     * Inicjalizacja aktywności i ustawienie akcji kliknięć przycisków.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_layout)

        initializeViews()
        registerButton.setOnClickListener {
            registerUser()
        }
        backToLoginButton.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }
    }

    private fun initializeViews() {
        inputEmail = findViewById(R.id.editTextTextEmailNewAddress)
        inputPassword = findViewById(R.id.editTextTextNewPassword)
        inputPasswordRepeat = findViewById(R.id.editTextTextRepeatPassword)
        registerButton = findViewById(R.id.ButtonCreatAccount)
        backToLoginButton = findViewById(R.id.buttonBackToLogIn)
    }

    /**
     * Sprawdza poprawność danych wprowadzonych przez użytkownika.
     * Weryfikuje obecność e-maila, hasła i ich zgodność.
     * @return `true` jeśli dane są poprawne, w przeciwnym razie `false`
     */
    private fun validateRegisterDetails(): Boolean {
        return when {
            TextUtils.isEmpty(inputEmail.text.toString().trim()) -> {
                showToast("Please enter your email!")
                false
            }
            TextUtils.isEmpty(inputPassword.text.toString().trim()) -> {
                showToast("Please enter your password!")
                false
            }
            TextUtils.isEmpty(inputPasswordRepeat.text.toString().trim()) -> {
                showToast("Please confirm your password!")
                false
            }
            inputPassword.text.toString().trim() != inputPasswordRepeat.text.toString().trim() -> {
                showToast("Passwords do not match!")
                false
            }
            else -> true
        }
    }

    /**
     * Po udanej walidacji przekierowuje użytkownika do aktywności konfiguracyjnej `PressureWindow`,
     * przekazując dane e-mail i hasła jako parametry.
     */
    private fun registerUser() {
        if (validateRegisterDetails()) {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            val intent = Intent(this, PressureWindow::class.java)
            intent.putExtra("EMAIL", email)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
            finish()
        }
    }

    /**
     * Wyświetla krótką wiadomość w formie Toasta.
     * @param message Tekst wiadomości
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
