package com.Travelplannerfyp.travelplannerapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.Travelplannerfyp.travelplannerapp.R
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private val TAG = "UnifiedSplash"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Initialize Firebase Auth
        try {
            auth = FirebaseAuth.getInstance()
            Log.d(TAG, "Firebase Auth initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase Auth", e)
            Toast.makeText(this, "Authentication service unavailable", Toast.LENGTH_SHORT).show()
        }

        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isFirstTime = sharedPreferences.getBoolean("IsFirstTime", true)

        // Splash delay
        Handler(Looper.getMainLooper()).postDelayed({
            if (isFirstTime) {
                // Show onboarding
                sharedPreferences.edit().putBoolean("IsFirstTime", false).apply()
                startActivity(Intent(this, onboarding1::class.java))
            } else {
                navigateBasedOnAuthAndRole()
            }
            finish()
        }, 2000)
    }

    private fun navigateBasedOnAuthAndRole() {
        try {
            val currentUser = try {
                auth.currentUser
            } catch (e: Exception) {
                Log.e(TAG, "Error accessing current user", e)
                null
            }

            if (currentUser != null && currentUser.uid.isNotEmpty()) {
                val selectedRole = sharedPreferences.getString("SelectedRole", null)
                Log.d(TAG, "Authenticated user found, SelectedRole: $selectedRole")

                val intent = when (selectedRole) {
                    "User" -> Intent(this, MainActivity::class.java)
                    "Organizer" -> Intent(this, organizermain::class.java)
                    else -> Intent(this, MainActivity::class.java) // fallback
                }

                startActivity(intent)
            } else {
                Log.d(TAG, "No authenticated user, redirecting to login")
                startActivity(Intent(this, LoginActivity::class.java))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during navigation", e)
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    fun logout() {
        sharedPreferences.edit()
            .remove("SelectedRole")
            .putBoolean("IsFirstTime", true)
            .apply()

        FirebaseAuth.getInstance().signOut()

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}