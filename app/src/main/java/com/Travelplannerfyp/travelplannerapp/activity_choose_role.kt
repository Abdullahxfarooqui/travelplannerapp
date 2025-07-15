package com.Travelplannerfyp.travelplannerapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.Travelplannerfyp.travelplannerapp.R

class activity_choose_role : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_role)

        val userButton: Button = findViewById(R.id.btn_continue_as_user)
        val organizerButton: Button = findViewById(R.id.btn_continue_as_organizer)


        userButton.setOnClickListener {

            val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("SelectedRole", "User")
            editor.apply()


            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }


        organizerButton.setOnClickListener {
            Toast.makeText(this, "Organizer button clicked", Toast.LENGTH_SHORT).show()

            val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("SelectedRole", "Organizer")
            editor.apply()

            val intent = Intent(this, organizermain::class.java)
            startActivity(intent)
            finish()
        }
    }
}