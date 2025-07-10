package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.Travelplannerfyp.travelplannerapp.R

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var emailInput: EditText
    private lateinit var resetButton: Button
    private lateinit var backToLoginButton: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var successMessage: TextView
    
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "ForgotPasswordActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        
        // Initialize views
        emailInput = findViewById(R.id.emailInput)
        resetButton = findViewById(R.id.resetButton)
        backToLoginButton = findViewById(R.id.backToLoginButton)
        progressBar = findViewById(R.id.progressBar)
        successMessage = findViewById(R.id.successMessage)
        
        // Set up reset button
        resetButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            
            if (!validateEmail(email)) {
                return@setOnClickListener
            }
            
            sendPasswordResetEmail(email)
        }
        
        // Set up back to login button
        backToLoginButton.setOnClickListener {
            finish()
        }
    }
    
    private fun validateEmail(email: String): Boolean {
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun sendPasswordResetEmail(email: String) {
        showLoading(true)
        
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showLoading(false)
                
                if (task.isSuccessful) {
                    Log.d(TAG, "Password reset email sent to $email")
                    showSuccessMessage()
                } else {
                    Log.e(TAG, "Failed to send password reset email", task.exception)
                    val errorMessage = task.exception?.message ?: "Failed to send reset email"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }
    
    private fun showSuccessMessage() {
        emailInput.visibility = View.GONE
        resetButton.visibility = View.GONE
        successMessage.visibility = View.VISIBLE
    }
    
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            resetButton.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            resetButton.isEnabled = true
        }
    }
}