package com.example.travelplannerapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class onboarding1 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding1)


        val getStartedButton: Button = findViewById(R.id.btn_next)

        getStartedButton.setOnClickListener {
            val intent = Intent(this, onboaring2::class.java)
            startActivity(intent)
            finish()
        }
    }
}
