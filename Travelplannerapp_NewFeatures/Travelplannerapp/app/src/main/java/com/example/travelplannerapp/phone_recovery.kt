package com.example.travelplannerapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class phone_recovery : AppCompatActivity() {
    private lateinit var phoneInput: EditText
    private lateinit var submitPhoneButton: Button
    private lateinit var otpInput: EditText
    private lateinit var submitOtpButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_recovery)

        // Initialize views
        phoneInput = findViewById(R.id.phone_input)
        submitPhoneButton = findViewById(R.id.submit_phone_button)
        otpInput = findViewById(R.id.otp_input)
        submitOtpButton = findViewById(R.id.submit_otp_button)

        // Handle phone number submission
        submitPhoneButton.setOnClickListener {
            val phoneNumber = phoneInput.text.toString().trim()

            if (phoneNumber.isEmpty()) {
                // Show a Toast message if the phone number is empty
                Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show()
            } else {
                // Send OTP to the phone number (dummy implementation)
                // You can add your actual OTP sending logic here.
                Toast.makeText(this, "OTP sent to $phoneNumber", Toast.LENGTH_SHORT).show()

                // Hide phone number input and submit button
                phoneInput.visibility = View.GONE
                submitPhoneButton.visibility = View.GONE

                // Show OTP input and submit OTP button
                otpInput.visibility = View.VISIBLE
                submitOtpButton.visibility = View.VISIBLE
            }
        }

        // Handle OTP submission
        submitOtpButton.setOnClickListener {
            val otp = otpInput.text.toString().trim()

            if (otp.isEmpty()) {
                // Show a Toast message if the OTP field is empty
                Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show()
            } else {
                // Validate OTP (dummy implementation)
                // You can add your actual OTP validation logic here.
                Toast.makeText(this, "OTP validated", Toast.LENGTH_SHORT).show()

                // Proceed with the next steps (e.g., reset password)
            }
        }
    }
}