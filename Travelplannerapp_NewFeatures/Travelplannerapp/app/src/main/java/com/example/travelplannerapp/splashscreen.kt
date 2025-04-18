package com.example.travelplannerapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class splashscreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashscreen)


        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isFirstTime = sharedPreferences.getBoolean("IsFirstTime", true)
        val selectedRole = sharedPreferences.getString("SelectedRole", null)


        Log.d("SplashScreen", "SelectedRole: $selectedRole")

        Handler(Looper.getMainLooper()).postDelayed({
            if (isFirstTime) {

                val intent = Intent(this, onboarding1::class.java)
                startActivity(intent)


                sharedPreferences.edit().putBoolean("IsFirstTime", false).apply()
            } else {

                if (selectedRole != null) {

                    val intent = when (selectedRole) {
                        "User" -> Intent(this, usermain::class.java)
                        "Organizer" -> Intent(this, organizermain::class.java)
                        else -> {

                            Log.d("SplashScreen", "Invalid role, redirecting to login screen.")
                            Intent(this, MainActivity::class.java)
                        }
                    }
                    startActivity(intent)
                } else {

                    Log.d("SplashScreen", "No role selected, redirecting to login screen.")
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
            }
            finish()
        }, 3000)
    }


    fun logout() {

        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("SelectedRole")
        editor.putBoolean("IsFirstTime", true)
        editor.apply()


        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}