package com.example.travelplannerapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class usermain : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usermain)

        val logoutButton: Button = findViewById(R.id.logout1)

        logoutButton.setOnClickListener {

            FirebaseAuth.getInstance().signOut()


            val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.remove("SelectedRole")
            editor.apply()


            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}