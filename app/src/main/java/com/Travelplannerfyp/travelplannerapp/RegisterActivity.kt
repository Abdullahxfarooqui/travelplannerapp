package com.Travelplannerfyp.travelplannerapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.Travelplannerfyp.travelplannerapp.R
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView
    private lateinit var passwordStrengthText: TextView
    private lateinit var progressBar: ProgressBar
    
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val TAG = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize views
        emailInput = findViewById(R.id.emailEditText)
        passwordInput = findViewById(R.id.passwordEditText)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        registerButton = findViewById(R.id.registerButton)
        loginLink = findViewById(R.id.loginLink)
        passwordStrengthText = findViewById(R.id.passwordStrengthText)
        progressBar = findViewById(R.id.progressBar)

        // Set up password strength checker
        passwordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updatePasswordStrengthView(s.toString())
            }
        })

        // Set up register button
        registerButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (!validateInputs(email, password, confirmPassword)) {
                return@setOnClickListener
            }

            // Show loading state
            showLoading(true)

            // Create user account
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            // Send Firebase verification email
                            user.sendEmailVerification()
                                .addOnCompleteListener { verificationTask ->
                                    if (verificationTask.isSuccessful) {
                                        Log.d(TAG, "Verification email sent.")
                                        // Create user data in Realtime Database (without OTP)
                                        val userData = mapOf(
                                            "email" to email,
                                            "createdAt" to System.currentTimeMillis(),
                                            "lastLogin" to System.currentTimeMillis(),
                                            "emailVerified" to false // Keep this false until user verifies via email link
                                            // Removed "otp" field
                                        )

                                        database.child("users").child(user.uid)
                                            .setValue(userData)
                                            .addOnSuccessListener {
                                                showLoading(false)
                                                Toast.makeText(this, "Account created! Please check your email to verify your account.", Toast.LENGTH_LONG).show()
                                                
                                                // Navigate back to Login screen
                                                val intent = Intent(this, LoginActivity::class.java)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                startActivity(intent)
                                                finish()
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e(TAG, "Failed to create user data", e)
                                                showLoading(false)
                                                // Optionally delete the created user if DB entry fails
                                                user.delete()
                                                Toast.makeText(this, "Failed to save user data. Please try again.", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        Log.e(TAG, "sendEmailVerification failed", verificationTask.exception)
                                        showLoading(false)
                                        // Optionally delete the created user if email sending fails
                                        user.delete()
                                        Toast.makeText(this, "Failed to send verification email. Please try again.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    } else {
                        // Show error message
                        showLoading(false)
                        val errorMessage = task.exception?.message ?: "Registration failed"
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Set up login link
        loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            // Don't finish here, allow user to go back if needed
        }
    }
    
    private fun validateInputs(email: String, password: String, confirmPassword: String): Boolean {
        // Check if fields are empty
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Check password match
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Check password strength
        if (password.length < 8) {
            Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (!isStrongPassword(password)) {
            Toast.makeText(this, "Password must contain uppercase, lowercase, number and special character", Toast.LENGTH_LONG).show()
            return false
        }
        
        return true
    }
    
    private fun isStrongPassword(password: String): Boolean {
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        
        return hasUppercase && hasLowercase && hasDigit && hasSpecialChar
    }
    
    private fun updatePasswordStrengthView(password: String) {
        if (password.isEmpty()) {
            passwordStrengthText.text = ""
            passwordStrengthText.visibility = View.GONE
            return
        }
        
        passwordStrengthText.visibility = View.VISIBLE
        
        val strength = when {
            password.length < 8 -> {
                passwordStrengthText.setTextColor(getColor(android.R.color.holo_red_dark))
                "Weak"
            }
            !isStrongPassword(password) -> {
                passwordStrengthText.setTextColor(getColor(android.R.color.holo_orange_dark))
                "Medium"
            }
            else -> {
                passwordStrengthText.setTextColor(getColor(android.R.color.holo_green_dark))
                "Strong"
            }
        }
        
        passwordStrengthText.text = "Password Strength: $strength"
    }
    
    // Remove generateOtp() function
    // private fun generateOtp(): String {
    // ... }
    
    // Remove simulateSendEmail() function
    // private fun simulateSendEmail(email: String, otp: String) {
    // ... }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        registerButton.isEnabled = !isLoading
        loginLink.isEnabled = !isLoading
    }
}