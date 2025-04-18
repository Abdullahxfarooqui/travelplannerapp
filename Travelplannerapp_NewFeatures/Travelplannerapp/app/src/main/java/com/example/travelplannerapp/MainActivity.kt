package com.example.travelplannerapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var signUpTextView: TextView
    private lateinit var forgetPasswordTextView: TextView

    override fun onStart() {
        super.onStart()

        val currentUser = auth.currentUser
        currentUser?.let {
            val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            val role = sharedPreferences.getString("SelectedRole", null)
            val intent = when (role) {
                "User" -> Intent(this, usermain::class.java)
                "Organizer" -> Intent(this, organizermain::class.java)
                else -> Intent(this, activity_choose_role::class.java)
            }
            intent.putExtra("email", it.email)
            intent.putExtra("uid", it.uid)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        // Initialize views
        progressBar = findViewById(R.id.progressbar)
        emailInput = findViewById(R.id.username_input)
        passwordInput = findViewById(R.id.password_input)
        loginButton = findViewById(R.id.login_button)
        signUpTextView = findViewById(R.id.tvSignUp)
        forgetPasswordTextView = findViewById(R.id.tvForgetPassword)

        // Login
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = View.GONE
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                        val user = auth.currentUser

                        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
                        val role = sharedPreferences.getString("SelectedRole", null)

                        val intent = when (role) {
                            "User" -> Intent(this, usermain::class.java)
                            "Organizer" -> Intent(this, organizermain::class.java)
                            else -> Intent(this, activity_choose_role::class.java)
                        }

                        intent.putExtra("email", user?.email)
                        intent.putExtra("uid", user?.uid)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            "Authentication failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        // Sign Up
        signUpTextView.setOnClickListener {
            startActivity(Intent(this, signup_activity::class.java))
        }

        // Forget Password
        forgetPasswordTextView.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please enter your registered email address",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.GONE
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Password reset email sent to $email",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Failed to send reset email: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun logout() {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        sharedPreferences.edit().remove("SelectedRole").apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
