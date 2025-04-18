package com.example.travelplannerapp

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class signup_activity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var progressBar: ProgressBar
    private val handler = Handler(Looper.getMainLooper())
    private var isCheckingVerification = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Views
        progressBar = findViewById(R.id.progressbar)
        val fullNameEditText = findViewById<TextInputEditText>(R.id.etFullName)
        val emailEditText = findViewById<TextInputEditText>(R.id.etEmail)
        val passwordEditText = findViewById<TextInputEditText>(R.id.etPassword)
        val confirmPasswordEditText = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val dobEditText = findViewById<TextInputEditText>(R.id.etDob)
        val signUpButton = findViewById<Button>(R.id.btnSignUp)
        val alreadyAccountTextView = findViewById<TextView>(R.id.tvAlreadyAccount)

        dobEditText.setOnClickListener {
            showDatePicker(dobEditText)
        }

        signUpButton.setOnClickListener {
            val fullName = fullNameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()
            val dob = dobEditText.text.toString().trim()

            if (validateInputs(fullName, email, password, confirmPassword, dob)) {
                registerUser(fullName, email, password, dob)
            }
        }

        alreadyAccountTextView.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun showDatePicker(dobEditText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                dobEditText.setText(selectedDate)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun validateInputs(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        dob: String
    ): Boolean {
        return when {
            fullName.isEmpty() -> showError(R.id.etFullName, "Full Name is required")
            email.isEmpty() -> showError(R.id.etEmail, "Email is required")
            dob.isEmpty() -> showError(R.id.etDob, "Date of Birth is required")
            password.isEmpty() -> showError(R.id.etPassword, "Password is required")
            password.length < 6 -> showError(R.id.etPassword, "Password must be at least 6 characters")
            confirmPassword.isEmpty() -> showError(R.id.etConfirmPassword, "Please confirm your password")
            password != confirmPassword -> showError(R.id.etConfirmPassword, "Passwords do not match")
            else -> true
        }
    }

    private fun showError(viewId: Int, message: String): Boolean {
        val view = findViewById<TextInputEditText>(viewId)
        view.error = message
        view.requestFocus()
        return false
    }

    private fun registerUser(fullName: String, email: String, password: String, dob: String) {
        progressBar.visibility = View.VISIBLE
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        sendEmailVerification(user, fullName, email, dob)
                    }
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun sendEmailVerification(user: FirebaseUser, fullName: String, email: String, dob: String) {
        user.sendEmailVerification().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                saveUserToFirestore(fullName, email, dob)
                Toast.makeText(this, "Verification email sent. Please verify your email.", Toast.LENGTH_LONG).show()
                startEmailVerificationChecker(user)
            } else {
                Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserToFirestore(fullName: String, email: String, dob: String) {
        val userData = hashMapOf(
            "fullName" to fullName,
            "email" to email,
            "dob" to dob,
            "verified" to false
        )

        db.collection("users").document(email)
            .set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "User data saved successfully.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save user data.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startEmailVerificationChecker(user: FirebaseUser) {
        if (isCheckingVerification) return

        isCheckingVerification = true
        progressBar.visibility = View.VISIBLE

        val checkVerificationRunnable = object : Runnable {
            override fun run() {
                user.reload().addOnCompleteListener {
                    if (user.isEmailVerified) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@signup_activity, "Email verified! Redirecting...", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@signup_activity, MainActivity::class.java))
                        finish()
                    } else {
                        handler.postDelayed(this, 3000) // Check again after 3 seconds
                    }
                }
            }
        }
        handler.post(checkVerificationRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        isCheckingVerification = false
        handler.removeCallbacksAndMessages(null)
    }
}