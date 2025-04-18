package com.example.travelplannerapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class activity_choose_role : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_role)

        val userButton: Button = findViewById(R.id.btn_continue_as_user)
        val organizerButton: Button = findViewById(R.id.btn_continue_as_organizer)


        userButton.setOnClickListener {

            val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("userRole", "User")
            editor.apply()


            val intent = Intent(this, usermain::class.java)
            startActivity(intent)
            finish()
        }


        organizerButton.setOnClickListener {

            val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("userRole", "Organizer")
            editor.apply()


            val intent = Intent(this, organizermain::class.java)
            startActivity(intent)
            finish()
        }
    }
}