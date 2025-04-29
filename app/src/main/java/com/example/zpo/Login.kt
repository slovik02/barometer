package com.example.zpo

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()


        if (auth.currentUser != null) {
            goToMainPage()
            finish()
            return
        }

        setContentView(R.layout.login_layout)
        initializeViews()

        loginButton.setOnClickListener {
            logInUser()
        }

        registerButton.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }
    }


    private fun initializeViews() {
        inputEmail = findViewById(R.id.EditTextEmail)
        inputPassword = findViewById(R.id.EditTextPassowrd)
        loginButton = findViewById(R.id.ButtonLogIn)
        registerButton = findViewById(R.id.buttonRegister)
    }

    private fun validateLoginDetails(): Boolean {
        return when {
            TextUtils.isEmpty(inputEmail.text.toString().trim()) -> {
                showToast("Please enter your email!")
                false
            }
            TextUtils.isEmpty(inputPassword.text.toString().trim()) -> {
                showToast("Please enter your password!")
                false
            }
            else -> true
        }
    }

    private fun logInUser() {
        if (validateLoginDetails()) {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        showToast("Login successful!")
                        goToMainPage()
                        finish()
                    } else {
                        showToast("Login failed: ${task.exception?.message}")
                    }
                }
        }
    }

    private fun goToMainPage() {
        val intent = Intent(this, main_page::class.java)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
