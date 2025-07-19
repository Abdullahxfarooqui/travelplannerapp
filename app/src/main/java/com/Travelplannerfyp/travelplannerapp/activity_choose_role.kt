package com.Travelplannerfyp.travelplannerapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.Travelplannerfyp.travelplannerapp.R
import com.google.firebase.auth.FirebaseAuth

class activity_choose_role : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_role)

        val userButton: Button = findViewById(R.id.btn_continue_as_user)
        val organizerButton: Button = findViewById(R.id.btn_continue_as_organizer)
        val adminButton: Button = findViewById(R.id.btn_continue_as_admin)

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val adminEmail = "abdullahxfarooquii@gmail.com"

        userButton.setOnClickListener {
            if (currentUser != null && currentUser.email.equals(adminEmail, ignoreCase = true)) {
                Toast.makeText(this, "Admin account cannot be used as a user.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("SelectedRole", "User")
            editor.apply()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        organizerButton.setOnClickListener {
            if (currentUser != null && currentUser.email.equals(adminEmail, ignoreCase = true)) {
                Toast.makeText(this, "Admin account cannot be used as an organizer.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            Toast.makeText(this, "Organizer button clicked", Toast.LENGTH_SHORT).show()
            val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("SelectedRole", "Organizer")
            editor.apply()
            val intent = Intent(this, organizermain::class.java)
            startActivity(intent)
            finish()
        }

        adminButton.setOnClickListener {
            val intent = Intent(this, AdminLoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}