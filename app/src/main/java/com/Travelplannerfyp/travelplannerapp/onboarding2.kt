package com.Travelplannerfyp.travelplannerapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.Travelplannerfyp.travelplannerapp.R

class onboarding2 : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding2)

        val getStartedButton: Button = findViewById(R.id.btn_next)

        getStartedButton.setOnClickListener {
            val intent = Intent(this, onboarding3::class.java)
            startActivity(intent)
            finish()
        }
    }
}