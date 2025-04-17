package com.example.travelplannerapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val TAG = "SplashActivity"

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
        
        // Delay for splash screen display
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, 1500) // 1.5 second delay
    }
    
    private fun navigateToNextScreen() {
        try {
            // Try to get the current user, but handle potential Firebase initialization errors
            val currentUser = try {
                auth.currentUser
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get current user", e)
                null
            }
            
            if (currentUser != null && currentUser.uid.isNotEmpty()) {
                // User is signed in, go to MainActivity
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                Log.d(TAG, "User authenticated, navigating to MainActivity")
                startActivity(intent)
                finish()
            } else {
                // No user is signed in, go to LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                Log.d(TAG, "No user authenticated, navigating to LoginActivity")
                startActivity(intent)
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking authentication state", e)
            // On any error, default to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}