package com.example.travelplannerapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class RoleSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        val userButton: Button = findViewById(R.id.btn_continue_as_user)
        val organizerButton: Button = findViewById(R.id.btn_continue_as_organizer)

        // ✅ FIX: Explicitly declare the type of fadeIn
        val fadeIn: Animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 1000
        findViewById<LinearLayout>(R.id.containerLayout).startAnimation(fadeIn)

        userButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            sharedPreferences.edit().apply {
                putString("userRole", "User")
                apply()
            }

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }

        organizerButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            sharedPreferences.edit().apply {
                putString("userRole", "Organizer")
                apply()
            }

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}
