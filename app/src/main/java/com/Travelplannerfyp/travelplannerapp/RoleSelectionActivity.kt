package com.Travelplannerfyp.travelplannerapp

import android.content.Intent
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.Travelplannerfyp.travelplannerapp.R
import com.google.firebase.auth.FirebaseAuth

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

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val adminEmail = "abdullahxfarooquii@gmail.com"

        userButton.setOnClickListener {
            if (currentUser != null && currentUser.email.equals(adminEmail, ignoreCase = true)) {
                android.widget.Toast.makeText(this, "Admin account cannot be used as a user.", android.widget.Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            sharedPreferences.edit().apply {
                putString("SelectedRole", "User")
                apply()
            }

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }

        organizerButton.setOnClickListener {
            if (currentUser != null && currentUser.email.equals(adminEmail, ignoreCase = true)) {
                android.widget.Toast.makeText(this, "Admin account cannot be used as an organizer.", android.widget.Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            sharedPreferences.edit().apply {
                putString("SelectedRole", "Organizer")
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
