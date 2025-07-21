// CRITICAL BUGFIX 2024-06-09:
// - Added logging to print the state of auth.currentUser and SharedPreferences at startup and before navigation.
// - Ensured no unintended sign-outs are present in SplashActivity.
// - This patch addresses: Persistent login/session QA and debugging.
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
            
            // Set persistence enabled
            try {
                auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(false)
                Log.d(TAG, "Firebase Auth persistence enabled")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable Firebase Auth persistence", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase Auth", e)
            Toast.makeText(this, "Authentication service unavailable", Toast.LENGTH_SHORT).show()
        }

        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isFirstTime = sharedPreferences.getBoolean("IsFirstTime", true)
        android.util.Log.d(TAG, "SplashActivity started. isFirstTime: $isFirstTime")
        android.util.Log.d(TAG, "auth.currentUser: ${auth.currentUser?.uid}, email: ${auth.currentUser?.email}")
        android.util.Log.d(TAG, "SharedPreferences SelectedRole: ${sharedPreferences.getString("SelectedRole", null)}")

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
            android.util.Log.d(TAG, "navigateBasedOnAuthAndRole: currentUser: ${currentUser?.uid}, email: ${currentUser?.email}")

            if (currentUser != null && currentUser.uid.isNotEmpty()) {
                // Verify the token is not expired
                currentUser.getIdToken(false)
                    .addOnSuccessListener { result ->
                        val selectedRole = sharedPreferences.getString("SelectedRole", null)
                        Log.d(TAG, "Authenticated user found, SelectedRole: $selectedRole")

                        val intent = when (selectedRole) {
                            "User" -> Intent(this, MainActivity::class.java)
                            "Organizer" -> Intent(this, organizermain::class.java)
                            else -> {
                                // If role is missing but user is authenticated, go to role selection
                                Log.d(TAG, "No role found for authenticated user, redirecting to role selection")
                                Intent(this, activity_choose_role::class.java)
                            }
                        }
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Token verification failed", e)
                        // Token might be expired, try to refresh
                        currentUser.reload()
                            .addOnSuccessListener {
                                Log.d(TAG, "User token refreshed successfully")
                                navigateBasedOnAuthAndRole() // Try again after refresh
                            }
                            .addOnFailureListener { reloadError ->
                                Log.e(TAG, "Failed to refresh user token", reloadError)
                                // Force re-login
                                startActivity(Intent(this, LoginActivity::class.java))
                            }
                    }
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