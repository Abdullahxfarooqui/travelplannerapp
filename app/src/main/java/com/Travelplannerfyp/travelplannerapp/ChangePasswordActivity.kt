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
    private lateinit var currentPasswordInput: TextInputEditText
    private lateinit var newPasswordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var currentPasswordLayout: TextInputLayout
    private lateinit var newPasswordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var changePasswordButton: MaterialButton
    private lateinit var cancelButton: MaterialButton
    
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        currentPasswordInput = findViewById(R.id.currentPasswordInput)
        newPasswordInput = findViewById(R.id.newPasswordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        currentPasswordLayout = findViewById(R.id.currentPasswordLayout)
        newPasswordLayout = findViewById(R.id.newPasswordLayout)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout)
        changePasswordButton = findViewById(R.id.changePasswordButton)
        cancelButton = findViewById(R.id.cancelButton)
    }

    private fun setupClickListeners() {
        changePasswordButton.setOnClickListener {
            if (validateInputs()) {
                changePassword()
            }
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun validateInputs(): Boolean {
        val currentPassword = currentPasswordInput.text.toString().trim()
        val newPassword = newPasswordInput.text.toString().trim()
        val confirmPassword = confirmPasswordInput.text.toString().trim()

        var isValid = true

        if (currentPassword.isEmpty()) {
            currentPasswordLayout.error = "Current password is required"
            currentPasswordLayout.isErrorEnabled = true
            isValid = false
        } else {
            currentPasswordLayout.error = null
            currentPasswordLayout.isErrorEnabled = false
        }

        if (newPassword.isEmpty()) {
            newPasswordLayout.error = "New password is required"
            newPasswordLayout.isErrorEnabled = true
            isValid = false
        } else if (newPassword.length < 6) {
            newPasswordLayout.error = "Password must be at least 6 characters"
            newPasswordLayout.isErrorEnabled = true
            isValid = false
        } else {
            newPasswordLayout.error = null
            newPasswordLayout.isErrorEnabled = false
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.error = "Please confirm your new password"
            confirmPasswordLayout.isErrorEnabled = true
            isValid = false
        } else if (confirmPassword != newPassword) {
            confirmPasswordLayout.error = "Passwords do not match"
            confirmPasswordLayout.isErrorEnabled = true
            isValid = false
        } else {
            confirmPasswordLayout.error = null
            confirmPasswordLayout.isErrorEnabled = false
        }

        return isValid
    }

    private fun changePassword() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to change password", Toast.LENGTH_SHORT).show()
            return
        }

        val currentPassword = currentPasswordInput.text.toString().trim()
        val newPassword = newPasswordInput.text.toString().trim()

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
                currentPasswordLayout.error = "Current password is incorrect"
                currentPasswordLayout.isErrorEnabled = true
            }
    }
} 