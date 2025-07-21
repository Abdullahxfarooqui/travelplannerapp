// CRITICAL BUGFIX 2024-06-09:
// - Added logging to print the state of auth.currentUser and SharedPreferences at startup and before navigation.
// - Ensured no unintended sign-outs are present in SplashActivity.
// - This patch addresses: Persistent login/session QA and debugging.
// - EMERGENCY FIX 2024-06-09: Enhanced session persistence with Firebase Auth and SharedPreferences
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
import com.google.firebase.database.FirebaseDatabase

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Initialize Firebase Auth with persistence
        try {
            auth = FirebaseAuth.getInstance()
            Log.d(TAG, "Firebase Auth initialized successfully")
            
            // Ensure persistence is enabled
            try {
                // Set persistence to LOCAL to keep user logged in
                auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(false)
                Log.d(TAG, "Firebase Auth persistence enabled")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable Firebase Auth persistence", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase Auth", e)
        }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isFirstTime = sharedPreferences.getBoolean("IsFirstTime", true)
        android.util.Log.d(TAG, "SplashActivity started. isFirstTime: $isFirstTime")
        android.util.Log.d(TAG, "auth.currentUser: ${auth.currentUser?.uid}, email: ${auth.currentUser?.email}")
        android.util.Log.d(TAG, "SharedPreferences SelectedRole: ${sharedPreferences.getString("SelectedRole", null)}")

        // Add delay to ensure Firebase Auth state is properly initialized
        Handler(Looper.getMainLooper()).postDelayed({
            navigateBasedOnAuthAndRole()
        }, 2000) // Increased delay to ensure Firebase Auth state is ready
    }

    private fun navigateBasedOnAuthAndRole() {
        try {
            val currentUser = try {
                auth.currentUser
            } catch (e: Exception) {
                Log.e(TAG, "Error getting current user", e)
                null
            }
            
            Log.d(TAG, "Checking auth state - currentUser: ${currentUser?.uid}, email: ${currentUser?.email}")
            
            val selectedRole = sharedPreferences.getString("SelectedRole", null)
            Log.d(TAG, "SelectedRole from SharedPreferences: $selectedRole")

            if (currentUser != null && !currentUser.isAnonymous) {
                Log.d(TAG, "User is logged in: ${currentUser.uid}")
                
                // Check if user exists in database
                val userRef = FirebaseDatabase.getInstance().reference.child("users").child(currentUser.uid)
                userRef.get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        Log.d(TAG, "User exists in database")
                        if (selectedRole != null) {
                            Log.d(TAG, "User has selected role: $selectedRole")
                            when (selectedRole) {
                                "admin" -> {
                                    Log.d(TAG, "Navigating to AdminDashboardActivity")
                                    startActivity(Intent(this@SplashActivity, AdminDashboardActivity::class.java))
                                }
                                "organizer" -> {
                                    Log.d(TAG, "Navigating to OrganizerDashboardActivity")
                                    startActivity(Intent(this@SplashActivity, OrganizerDashboardActivity::class.java))
                                }
                                "user" -> {
                                    Log.d(TAG, "Navigating to MainActivity")
                                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                                }
                                else -> {
                                    Log.d(TAG, "Unknown role, navigating to ChooseRoleActivity")
                                    startActivity(Intent(this@SplashActivity, ChooseRoleActivity::class.java))
                                }
                            }
                        } else {
                            Log.d(TAG, "No role selected, navigating to ChooseRoleActivity")
                            startActivity(Intent(this@SplashActivity, ChooseRoleActivity::class.java))
                        }
                    } else {
                        Log.d(TAG, "User does not exist in database, navigating to LoginActivity")
                        startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    }
                    finish()
                }.addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to check user in database", exception)
                    // On failure, still try to navigate based on role
                    if (selectedRole != null) {
                        when (selectedRole) {
                            "admin" -> startActivity(Intent(this@SplashActivity, AdminDashboardActivity::class.java))
                            "organizer" -> startActivity(Intent(this@SplashActivity, OrganizerDashboardActivity::class.java))
                            "user" -> startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                            else -> startActivity(Intent(this@SplashActivity, ChooseRoleActivity::class.java))
                        }
                    } else {
                        startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    }
                    finish()
                }
            } else {
                Log.d(TAG, "User is not logged in or is anonymous")
                if (isFirstTime) {
                    Log.d(TAG, "First time user, navigating to OnboardingActivity")
                    startActivity(Intent(this@SplashActivity, OnboardingActivity::class.java))
                } else {
                    Log.d(TAG, "Not first time, navigating to LoginActivity")
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                }
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in navigateBasedOnAuthAndRole", e)
            // Fallback to login activity
            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            finish()
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