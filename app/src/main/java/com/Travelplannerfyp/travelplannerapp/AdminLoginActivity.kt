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

class AdminLoginActivity : AppCompatActivity() {
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar
    private val adminEmail = "abdullahxfarooquii@gmail.com"
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)

        emailInput = findViewById(R.id.adminEmailInput)
        passwordInput = findViewById(R.id.adminPasswordInput)
        loginButton = findViewById(R.id.adminLoginButton)
        progressBar = findViewById(R.id.adminLoginProgressBar)

        loginButton.setOnClickListener {
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
            if (!email.equals(adminEmail, ignoreCase = true)) {
                Toast.makeText(this, "Only the admin email can log in here.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            progressBar.visibility = View.VISIBLE
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.GONE
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val uid = user?.uid
                        if (uid != null) {
                            val userRef = db.child("users").child(uid)
                            userRef.get().addOnSuccessListener { userSnapshot ->
                                if (!userSnapshot.exists()) {
                                    // Create the user node with admin: true
                                    val adminData = mapOf(
                                        "name" to "Abdullah Farooqui",
                                        "email" to email,
                                        "createdAt" to System.currentTimeMillis(),
                                        "status" to "active",
                                        "admin" to true
                                    )
                                    userRef.setValue(adminData)
                                }
                                
                                // Check for admin status - try both field names and email
                                val isAdminByField = userSnapshot.child("admin").getValue(Boolean::class.java) == true ||
                                                   userSnapshot.child("isAdmin").getValue(Boolean::class.java) == true
                                val isAdminByEmail = email.equals(adminEmail, ignoreCase = true)
                                val isAdminByUid = uid == "QSYFRzkmQEa4vqy30CC6CaA8ACq1"
                                
                                if (isAdminByField || isAdminByEmail || isAdminByUid) {
                                    // Update to use new admin field structure
                                    userRef.child("admin").setValue(true)
                                    userRef.child("name").setValue("Abdullah Farooqui")
                                    userRef.child("status").setValue("active")
                                    
                                    val intent = Intent(this, AdminDashboardActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this, "This account is not an admin.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
} 