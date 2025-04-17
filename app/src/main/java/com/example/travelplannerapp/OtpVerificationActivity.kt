package com.example.travelplannerapp

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random
import com.google.firebase.auth.ActionCodeSettings

class OtpVerificationActivity : AppCompatActivity() {
    private lateinit var otpEditText: EditText
    private lateinit var verifyButton: Button
    private lateinit var resendButton: TextView
    private lateinit var timerTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var countDownTimer: CountDownTimer

    private var email: String = ""
    private var userId: String = ""
    private var generatedOtp: String = ""
    private val TAG = "OtpVerificationActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize views
        otpEditText = findViewById(R.id.otpEditText)
        verifyButton = findViewById(R.id.verifyButton)
        resendButton = findViewById(R.id.resendOtpButton)
        timerTextView = findViewById(R.id.timerTextView)
        emailTextView = findViewById(R.id.emailTextView)

        // Get data from intent
        email = intent.getStringExtra("email") ?: ""
        userId = intent.getStringExtra("userId") ?: ""
        generatedOtp = intent.getStringExtra("otp") ?: ""

        if (email.isEmpty() || userId.isEmpty() || generatedOtp.isEmpty()) {
            Log.e(TAG, "Missing required data: email=$email, userId=$userId, otp=$generatedOtp")
            Toast.makeText(this, "Verification error. Please try again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Display masked email
        emailTextView.text = maskEmail(email)

        // Start countdown timer
        startCountdownTimer()

        // Set up verify button
        verifyButton.setOnClickListener {
            val enteredOtp = otpEditText.text.toString().trim()
            if (enteredOtp.isEmpty()) {
                Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyOtp(enteredOtp)
        }

        // Set up resend button
        resendButton.setOnClickListener {
            resendOtp()
        }
    }

    private fun verifyOtp(enteredOtp: String) {
        verifyButton.isEnabled = false
        verifyButton.text = "Verifying..."

        if (enteredOtp == generatedOtp) {
            // OTP is correct, update user verification status
            val userRef = database.reference.child("users").child(userId)
            userRef.child("emailVerified").setValue(true)
                .addOnSuccessListener {
                    // Mark email as verified in Firebase Auth if possible
                    auth.currentUser?.let { user ->
                        user.sendEmailVerification()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d(TAG, "Verification email sent")
                                } else {
                                    Log.e(TAG, "Failed to send verification email", task.exception)
                                }
                            }
                    }

                    Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show()

                    // Navigate to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update verification status", e)
                    Toast.makeText(this, "Verification failed. Please try again.", Toast.LENGTH_SHORT).show()
                    verifyButton.isEnabled = true
                    verifyButton.text = "Verify"
                }
        } else {
            // Incorrect OTP
            Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show()
            verifyButton.isEnabled = true
            verifyButton.text = "Verify"
        }
    }

    private fun resendOtp() {
        // Generate a new OTP
        generatedOtp = generateOtp()

        // Update the OTP in the database
        val userRef = database.reference.child("users").child(userId)
        userRef.child("otp").setValue(generatedOtp)
            .addOnSuccessListener {
                // Simulate sending email with OTP
                simulateSendEmail(email, generatedOtp)

                // Reset the timer
                if (::countDownTimer.isInitialized) {
                    countDownTimer.cancel()
                }
                startCountdownTimer()

                Toast.makeText(this, "OTP resent to your email", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update OTP", e)
                Toast.makeText(this, "Failed to resend OTP. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startCountdownTimer() {
        resendButton.visibility = View.GONE
        timerTextView.visibility = View.VISIBLE

        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                timerTextView.text = "Resend OTP in ${seconds}s"
            }

            override fun onFinish() {
                timerTextView.visibility = View.GONE
                resendButton.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun maskEmail(email: String): String {
        val atIndex = email.indexOf('@')
        if (atIndex <= 1) return email // Can't mask if too short

        val username = email.substring(0, atIndex)
        val domain = email.substring(atIndex)

        val maskedUsername = if (username.length <= 3) {
            username.first() + "*".repeat(username.length - 1)
        } else {
            username.first() + "*".repeat(username.length - 2) + username.last()
        }

        return maskedUsername + domain
    }

    private fun generateOtp(): String {
        // Generate a 6-digit OTP
        return String.format("%06d", Random.nextInt(1000000))
    }

    private fun simulateSendEmail(email: String, otp: String) {
        Log.d(TAG, "OTP for $email: $otp")
        
        // Use Firebase Authentication to send an email with OTP
        val auth = FirebaseAuth.getInstance()
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://travelplannerapp-5617e.firebaseapp.com")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                "com.example.travelplannerapp",
                true, /* installIfNotAvailable */
                null /* minimumVersion */)
            .build()
            
        // Send verification email with custom template that includes OTP
        auth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Email sent with OTP to $email")
                    // Save the OTP to Firebase Database for verification
                    database.reference.child("users").child(userId).child("otp").setValue(otp)
                    Toast.makeText(this, "Verification code sent to your email", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "Failed to send email", task.exception)
                    Toast.makeText(this, "Failed to send verification email. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
    }
}
