package com.Travelplannerfyp.travelplannerapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.Travelplannerfyp.travelplannerapp.R

class AdminSignUpActivity : AppCompatActivity() {
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var signUpButton: Button
    // Removed all references to adminSignUpProgressBar as it does not exist in the layout.
    private val adminEmail = "abdullahxfarooquii@gmail.com"
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_signup)

        emailInput = findViewById(R.id.adminEmailInput)
        passwordInput = findViewById(R.id.adminPasswordInput)
        signUpButton = findViewById(R.id.adminSignUpButton)
        // Removed all references to adminSignUpProgressBar as it does not exist in the layout.

        signUpButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Enter a valid email"
                return@setOnClickListener
            }
            if (password.length < 8) {
                passwordInput.error = "Password must be at least 8 characters"
                return@setOnClickListener
            }
            if (email != adminEmail) {
                Toast.makeText(this, "Only authorized email can sign up as admin", Toast.LENGTH_SHORT).show()
                auth.signOut()
                return@setOnClickListener
            }
            // Removed all references to adminSignUpProgressBar as it does not exist in the layout.
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val uid = user?.uid
                        if (uid != null) {
                            db.child("users").child(uid).setValue(
                                mapOf(
                                    "name" to "Abdullah Farooqui",
                                    "email" to email,
                                    "createdAt" to System.currentTimeMillis(),
                                    "status" to "active",
                                    "admin" to true
                                )
                            ).addOnSuccessListener {
                                checkAdminAndRedirect(uid)
                            }.addOnFailureListener { e ->
                                // Removed all references to adminSignUpProgressBar as it does not exist in the layout.
                                Toast.makeText(this, "Failed to save admin data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        val exception = task.exception
                        if (exception != null && exception.message?.contains("email address is already in use") == true) {
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { signInTask ->
                                    if (signInTask.isSuccessful) {
                                        val user = auth.currentUser
                                        val uid = user?.uid
                                        if (uid != null) {
                                            db.child("users").child(uid).child("isAdmin").setValue(true)
                                                .addOnSuccessListener {
                                                    checkAdminAndRedirect(uid)
                                                }
                                                .addOnFailureListener { e ->
                                                    // Removed all references to adminSignUpProgressBar as it does not exist in the layout.
                                                    Toast.makeText(this, "Failed to update admin status: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    } else {
                                        // Removed all references to adminSignUpProgressBar as it does not exist in the layout.
                                        Toast.makeText(this, "Email already in use. Wrong password for existing admin account.", Toast.LENGTH_LONG).show()
                                    }
                                }
                        } else {
                            // Removed all references to adminSignUpProgressBar as it does not exist in the layout.
                            Toast.makeText(this, "${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }
    }

    private fun checkAdminAndRedirect(uid: String) {
        // Removed all references to adminSignUpProgressBar as it does not exist in the layout.
        db.child("users").child(uid).child("isAdmin").get()
            .addOnSuccessListener { snapshot ->
                // Removed all references to adminSignUpProgressBar as it does not exist in the layout.
                val isAdmin = snapshot.getValue(Boolean::class.java) == true
                // Removed reference to AdminPanelActivity as it no longer exists.
                // TODO: Implement new admin navigation if needed in the future.
                if (isAdmin) {
                    startActivity(Intent(this, activity_choose_role::class.java))
                } else {
                    startActivity(Intent(this, activity_choose_role::class.java))
                }
                finish()
            }
            .addOnFailureListener { e ->
                // Removed all references to adminSignUpProgressBar as it does not exist in the layout.
                Toast.makeText(this, "Failed to check admin status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
} 