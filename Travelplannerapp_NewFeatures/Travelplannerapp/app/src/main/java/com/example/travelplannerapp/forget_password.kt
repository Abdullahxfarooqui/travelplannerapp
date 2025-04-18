package com.example.travelplannerapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class forget_password : AppCompatActivity() {
    private lateinit var emailInput: EditText
    private lateinit var resetPasswordButton: Button
    private lateinit var phoneRecoveryText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_password)

        emailInput = findViewById(R.id.email_input)
        resetPasswordButton = findViewById(R.id.reset_password_button)
        phoneRecoveryText = findViewById(R.id.phone_recovery_text)

        resetPasswordButton.setOnClickListener {
            val email = emailInput.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            } else {

                Toast.makeText(this, "Password reset link sent to $email", Toast.LENGTH_SHORT).show()
            }
        }

        phoneRecoveryText.setOnClickListener {
            val intent = Intent(this, phone_recovery::class.java)
            startActivity(intent)
        }
    }
}