package com.Travelplannerfyp.travelplannerapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.Travelplannerfyp.travelplannerapp.models.User
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
 import android.text.Editable
import android.text.TextWatcher
import com.google.android.gms.tasks.Task
import com.Travelplannerfyp.travelplannerapp.R

class LoginActivity : AppCompatActivity() {
    // UI components
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var passwordInput: TextInputEditText
    private lateinit var rememberMeCheckbox: MaterialCheckBox
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    private lateinit var forgotPasswordLink: TextView
    private lateinit var googleSignInButton: SignInButton
    private lateinit var progressBar: ProgressBar
    private lateinit var rootView: View
    private lateinit var containerLayout: View

    // Firebase components
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    
    // Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001
    
    // SharedPreferences for Remember Me
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_NAME = "LoginPrefs"
    private val KEY_REMEMBER_ME = "rememberMe"
    private val KEY_EMAIL = "email"
    private val KEY_PASSWORD = "password"
    
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize views
        initializeViews()
        
        // Set up SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
        // Configure Google Sign-In
        configureGoogleSignIn()
        
        // Set up click listeners
        setupClickListeners()
        
        // Load saved credentials if Remember Me was checked
        loadSavedCredentials()
    }

    private fun initializeViews() {
        // TEMP: Log all view IDs to debug
        val rootView = findViewById<View>(android.R.id.content)
        Log.d("ADMIN_BTN", "Root view: ${rootView.javaClass.simpleName}")
        
        // Try to find the button
        val loginAsAdminButton = findViewById<Button>(R.id.loginAsAdminButton)
        if (loginAsAdminButton != null) {
            Log.d("ADMIN_BTN", "Button found: ${loginAsAdminButton.text}")
            Toast.makeText(this, "Admin button found", Toast.LENGTH_SHORT).show()
            loginAsAdminButton.setOnClickListener {
                Log.d("ADMIN_BTN", "Login as Admin button clicked")
                Toast.makeText(this, "Opening Admin Panel...", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, AdminPanelActivity::class.java)
                Log.d("ADMIN_BTN", "Intent created: ${intent.component}")
                startActivity(intent)
                Log.d("ADMIN_BTN", "startActivity called")
            }
        } else {
            Log.e("ADMIN_BTN", "Button NOT found!")
            Toast.makeText(this, "Admin button NOT found!", Toast.LENGTH_LONG).show()
            
            // Try to find any button
            val anyButton = findViewById<Button>(android.R.id.button1)
            if (anyButton != null) {
                Log.d("ADMIN_BTN", "Found generic button: ${anyButton.text}")
            } else {
                Log.e("ADMIN_BTN", "No buttons found at all!")
            }
            
            // Log all view IDs in the layout
            val containerLayout = findViewById<View>(R.id.containerLayout)
            if (containerLayout != null) {
                Log.d("ADMIN_BTN", "Container layout found")
                // Log all child views
                for (i in 0 until (containerLayout as android.view.ViewGroup).childCount) {
                    val child = (containerLayout as android.view.ViewGroup).getChildAt(i)
                    Log.d("ADMIN_BTN", "Child $i: ${child.javaClass.simpleName}, ID: ${resources.getResourceEntryName(child.id)}")
                }
            } else {
                Log.e("ADMIN_BTN", "Container layout NOT found!")
            }
        }
        
        // Apply animations
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 1000
        rootView.startAnimation(fadeIn)
    }

    private fun configureGoogleSignIn() {
        // TEMP: Skip Google Sign-In setup for minimal layout testing
        try {
            Log.d("ADMIN_BTN", "Skipping Google Sign-In setup for minimal layout testing")
        } catch (e: Exception) {
            Log.e(TAG, "Error in configureGoogleSignIn: ${e.message}")
        }
    }

    private fun setupClickListeners() {
        // TEMP: Skip all click listeners except admin button for testing
        try {
            // These views don't exist in our minimal layout, so we'll skip them
            Log.d("ADMIN_BTN", "Skipping other click listeners for minimal layout testing")
        } catch (e: Exception) {
            Log.e("ADMIN_BTN", "Error in setupClickListeners: ${e.message}")
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        // Clear previous errors
        emailInputLayout.error = null
        passwordInputLayout.error = null

        if (email.isEmpty()) {
            emailInputLayout.error = "Email address is required"
            emailInput.requestFocus()
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = "Please enter a valid email address"
            emailInput.requestFocus()
            isValid = false
        }
        
        if (password.isEmpty()) {
            passwordInputLayout.error = "Password is required"
            if (isValid) passwordInput.requestFocus()
            isValid = false
        } else if (password.length < 8) {
            passwordInputLayout.error = "Password must be at least 8 characters"
            if (isValid) passwordInput.requestFocus()
            isValid = false
        }

        return isValid
    }

    private fun saveCredentialsIfNeeded(email: String, password: String) {
        val editor = sharedPreferences.edit()
        if (rememberMeCheckbox.isChecked) {
            editor.putBoolean(KEY_REMEMBER_ME, true)
            editor.putString(KEY_EMAIL, email)
            editor.putString(KEY_PASSWORD, password)
        } else {
            editor.clear()
        }
        editor.apply()
    }

    private fun loadSavedCredentials() {
        if (sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)) {
            val savedEmail = sharedPreferences.getString(KEY_EMAIL, "")
            val savedPassword = sharedPreferences.getString(KEY_PASSWORD, "")
            
            emailInput.setText(savedEmail)
            passwordInput.setText(savedPassword)
            rememberMeCheckbox.isChecked = true
        }
    }

    private fun loginWithEmailAndPassword(email: String, password: String) {
        showLoading(true)

        // Clear any previous errors
        emailInputLayout.error = null
        passwordInputLayout.error = null

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        if (user.isEmailVerified) {
                            updateLoginTimestamp(user.uid)
                        } else {
                            // Send verification email if not already sent
                            if (System.currentTimeMillis() - (user.metadata?.creationTimestamp ?: 0) < 300000) { // 5 minutes
                                user.sendEmailVerification()
                                    .addOnSuccessListener {
                                        showLoading(false)
                                        showSnackbar("Verification email sent. Please check your inbox.")
                                    }
                                    .addOnFailureListener { e ->
                                        showLoading(false)
                                        showSnackbar("Failed to send verification email: ${e.message}")
                                    }
                            }
                            showLoading(false)
                            showEmailNotVerifiedDialog(user)
                        }
                    } else {
                        showLoading(false)
                        emailInputLayout.error = "Login failed. User not found."
                    }
                } else {
                    showLoading(false)
                    handleLoginError(task.exception)
                }
            }
    }

    private fun handleLoginError(exception: Exception?) {
        val errorMessage = when {
            exception?.message?.contains("password") == true -> {
                passwordInputLayout.error = "Incorrect password. Please try again."
                "Incorrect password"
            }
            exception?.message?.contains("no user record") == true -> {
                emailInputLayout.error = "No account found with this email"
                "No account found with this email. Please register first."
            }
            exception?.message?.contains("network") == true -> {
                "Network error. Please check your internet connection."
            }
            exception?.message?.contains("blocked") == true -> {
                "Too many failed attempts. Please try again later."
            }
            exception?.message?.contains("invalid") == true -> {
                emailInputLayout.error = "Invalid email format"
                "Please enter a valid email address"
            }
            exception?.message?.contains("disabled") == true -> {
                "This account has been disabled. Please contact support."
            }
            else -> exception?.message ?: "Authentication failed"
        }
        
        showSnackbar(errorMessage)
        Log.e(TAG, "Login error: ${exception?.message}")
    }

    private fun checkFirebaseAuthEmailVerification(user: FirebaseUser) {
        if (user.isEmailVerified) {
            updateLoginTimestamp(user.uid)
        } else {
            showLoading(false)
            showEmailNotVerifiedDialog(user)
        }
    }

    private fun updateLoginTimestamp(userId: String) {
        val updates = mapOf(
            "lastLogin" to System.currentTimeMillis(),
            "isEmailVerified" to true
        )
        
        database.child("users").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                showLoading(false)
                startMainActivity()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update login timestamp", e)
                showLoading(false)
                startMainActivity() // Still proceed to main activity
            }
    }

    private fun showEmailNotVerifiedDialog(user: FirebaseUser) {
        AlertDialog.Builder(this)
            .setTitle("Email Not Verified")
            .setMessage("Your email address has not been verified. Please check your email for the verification link. Would you like to resend the verification email?")
            .setPositiveButton("Resend Email") { dialog, _ ->
                resendVerificationEmail(user)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun resendVerificationEmail(user: FirebaseUser) {
        showLoading(true)
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    showSnackbar("Verification email sent. Please check your inbox.")
                } else {
                    showSnackbar("Failed to send verification email. ${task.exception?.message}")
                }
            }
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Forgot Password")

        val view = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val emailEditText = view.findViewById<EditText>(R.id.emailEditText)
        
        // Pre-fill with email if available
        val currentEmail = emailInput.text.toString().trim()
        if (currentEmail.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(currentEmail).matches()) {
            emailEditText.setText(currentEmail)
        }
        
        builder.setView(view)

        builder.setPositiveButton("Send Reset Link") { dialog, _ ->
            val email = emailEditText.text.toString().trim()
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showSnackbar("Please enter a valid email address")
            } else {
                sendPasswordResetEmail(email)
                dialog.dismiss()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    private fun sendPasswordResetEmail(email: String) {
        showLoading(true)
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    showSnackbar("Password reset email sent to $email")
                } else {
                    showSnackbar("Failed to send reset email: ${task.exception?.message}")
                }
            }
    }

    private fun signInWithGoogle() {
        try {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching Google Sign-In", e)
            showSnackbar("Google Sign-In is currently unavailable. Please use email/password login.")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleGoogleSignInResult(task)
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                showLoading(false)
                showSnackbar("Google Sign-In failed. Please try again or use email login.")
            }
        }
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Google Sign In was successful, authenticate with Firebase
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            showLoading(false)
            when (e.statusCode) {
                CommonStatusCodes.NETWORK_ERROR -> 
                    showSnackbar("Network error. Please check your connection.")
                CommonStatusCodes.CANCELED -> 
                    Log.d(TAG, "Google Sign-In was canceled by user")
                CommonStatusCodes.INVALID_ACCOUNT -> 
                    showSnackbar("Invalid account. Please try again.")
                else -> {
                    Log.e(TAG, "Google sign in failed", e)
                    showSnackbar("Google Sign-In failed: ${e.message}")
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    val user = auth.currentUser
                    if (user != null) {
                        checkAndCreateUserInDatabase(user)
                    } else {
                        showLoading(false)
                        showSnackbar("Authentication failed")
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    showLoading(false)
                    showSnackbar("Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun checkAndCreateUserInDatabase(user: FirebaseUser) {
        val userId = user.uid
        database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    val userData = User(
                        userId = user.uid,
                        name = user.displayName ?: "User",
                        email = user.email ?: "",
                        profileImageUrl = user.photoUrl?.toString() ?: "",
                        createdAt = System.currentTimeMillis(),
                        lastLogin = System.currentTimeMillis(),
                        isEmailVerified = user.isEmailVerified,
                        role = "" // Role will be selected in RoleSelectionActivity
                    )

                    database.child("users").child(userId).setValue(userData)
                        .addOnSuccessListener {
                            showLoading(false)
                            startMainActivity()
                        }
                        .addOnFailureListener { e ->
                            showLoading(false)
                            showSnackbar("Database error: ${e.message}")
                        }
                } else {
                    updateLoginTimestamp(userId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showLoading(false)
                showSnackbar("Database error: ${error.message}")
            }
        })
    }

    private fun startMainActivity() {
        // Redirect to role selection instead of MainActivity
        val intent = Intent(this, activity_choose_role::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        loginButton.isEnabled = !isLoading
        googleSignInButton.isEnabled = !isLoading
        registerLink.isEnabled = !isLoading
        forgotPasswordLink.isEnabled = !isLoading
        emailInput.isEnabled = !isLoading
        passwordInput.isEnabled = !isLoading
        rememberMeCheckbox.isEnabled = !isLoading
    }
    
    private fun showSnackbar(message: String) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            startMainActivity()
        }
    }
}
