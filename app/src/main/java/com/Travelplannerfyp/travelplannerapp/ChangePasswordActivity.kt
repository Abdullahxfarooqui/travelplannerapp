package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity() {
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var changePasswordButton: MaterialButton
    
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        changePasswordButton = findViewById(R.id.changePasswordButton)
    }

    private fun setupClickListeners() {
        changePasswordButton.setOnClickListener {
            if (validateInputs()) {
                changePassword()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val currentPassword = etCurrentPassword.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        var isValid = true

        if (currentPassword.isEmpty()) {
            Toast.makeText(this, "Current password is required", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (newPassword.isEmpty()) {
            Toast.makeText(this, "New password is required", Toast.LENGTH_SHORT).show()
            isValid = false
        } else if (newPassword.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please confirm your new password", Toast.LENGTH_SHORT).show()
            isValid = false
        } else if (confirmPassword != newPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun changePassword() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to change password", Toast.LENGTH_SHORT).show()
            return
        }

        val currentPassword = etCurrentPassword.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()

        changePasswordButton.isEnabled = false
        changePasswordButton.text = "Changing Password..."

        // Re-authenticate user before changing password
        val credential = com.google.firebase.auth.EmailAuthProvider
            .getCredential(currentUser.email!!, currentPassword)

        currentUser.reauthenticate(credential)
            .addOnSuccessListener {
                // Password re-authentication successful, now change password
                currentUser.updatePassword(newPassword)
                    .addOnSuccessListener {
                        changePasswordButton.text = "Change Password"
                        changePasswordButton.isEnabled = true
                        Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show()
                        
                        // Show success dialog
                        AlertDialog.Builder(this)
                            .setTitle("Success")
                            .setMessage("Your password has been changed successfully.")
                            .setPositiveButton("OK") { _, _ ->
                                finish()
                            }
                            .show()
                    }
                    .addOnFailureListener { e ->
                        changePasswordButton.text = "Change Password"
                        changePasswordButton.isEnabled = true
                        Log.e("ChangePasswordActivity", "Failed to change password", e)
                        Toast.makeText(this, "Failed to change password: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                changePasswordButton.text = "Change Password"
                changePasswordButton.isEnabled = true
                Log.e("ChangePasswordActivity", "Failed to re-authenticate", e)
                Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_LONG).show()
                etCurrentPassword.error = "Current password is incorrect"
            }
    }
} 