package com.Travelplannerfyp.travelplannerapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.Travelplannerfyp.travelplannerapp.R

class onboarding1 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboaring1)


        val getStartedButton: Button = findViewById(R.id.btn_next)

        getStartedButton.setOnClickListener {
            val intent = Intent(this, onboarding2::class.java)
            startActivity(intent)
            finish()
        }
    }
}
