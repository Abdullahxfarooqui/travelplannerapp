package com.example.travelplannerapp

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.audiofx.EnvironmentalReverb
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import java.io.FileNotFoundException
import java.util.Calendar

class activity_profile : AppCompatActivity() {
    private lateinit var imgProfile: ImageView
    private lateinit var btnChooseImage: Button
    private lateinit var btnRemoveImage: Button
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etDob: EditText
    private lateinit var etCnic: EditText
    private lateinit var etMobile: EditText
    private lateinit var btnSave: Button

    private var imageUri: Uri? = null
    private lateinit var sharedPreferences: SharedPreferences

    private val REQUEST_PERMISSION = 1001

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            loadProfileImage(uri)
            saveProfileImageUri(uri)
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initViews()
        sharedPreferences = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        loadUserProfile()

        requestStoragePermission()

        btnChooseImage.setOnClickListener { pickImageFromGallery() }

        btnRemoveImage.setOnClickListener { removeProfileImage() }

        etDob.setOnClickListener { showDatePicker() }

        btnSave.setOnClickListener {
            if (isValidInput()) {
                saveUserProfile()  // Save the profile data and stay on the current screen
            }
        }
    }

    private fun initViews() {
        imgProfile = findViewById(R.id.profile_img)
        btnChooseImage = findViewById(R.id.btnChooseImage)
        btnRemoveImage = findViewById(R.id.btnRemoveImage)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etDob = findViewById(R.id.etDob)
        etCnic = findViewById(R.id.etCnic)
        etMobile = findViewById(R.id.etMobile)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_PERMISSION
                )
            }
        }
    }

    private fun pickImageFromGallery() {
        imagePickerLauncher.launch("image/*")
    }

    private fun loadProfileImage(uri: Uri) {
        try {
            Glide.with(this)
                .load(uri)
                .into(imgProfile)
            btnRemoveImage.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfileImageUri(uri: Uri) {
        sharedPreferences.edit().putString("ProfileImageUri", uri.toString()).apply()
    }

    private fun removeProfileImage() {
        imgProfile.setImageResource(R.drawable.ic_profile)
        imageUri = null
        btnRemoveImage.visibility = View.GONE
        sharedPreferences.edit().remove("ProfileImageUri").apply()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = "%02d/%02d/%04d".format(selectedDay, selectedMonth + 1, selectedYear)
            etDob.setText(formattedDate)
        }, year, month, day)

        datePicker.show()
    }

    private fun saveUserProfile() {
        sharedPreferences.edit().apply {
            putString("Name", etName.text.toString())
            putString("Email", etEmail.text.toString())
            putString("DOB", etDob.text.toString())
            putString("CNIC", etCnic.text.toString())
            putString("Mobile", etMobile.text.toString())
            imageUri?.let { putString("ProfileImageUri", it.toString()) }
            apply()
        }

        Snackbar.make(btnSave, "Profile saved successfully!", Snackbar.LENGTH_SHORT).show()
    }

    private fun loadUserProfile() {
        etName.setText(sharedPreferences.getString("Name", ""))
        etEmail.setText(sharedPreferences.getString("Email", ""))
        etDob.setText(sharedPreferences.getString("DOB", ""))
        etCnic.setText(sharedPreferences.getString("CNIC", ""))
        etMobile.setText(sharedPreferences.getString("Mobile", ""))

        val imageUriString = sharedPreferences.getString("ProfileImageUri", null)
        imageUriString?.let {
            try {
                imageUri = Uri.parse(it)
                loadProfileImage(imageUri!!)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Could not load saved image.", Toast.LENGTH_SHORT).show()
                btnRemoveImage.visibility = View.GONE
            }
        }
    }

    private fun isValidInput(): Boolean {
        return if (etName.text.isNullOrEmpty() ||
            etEmail.text.isNullOrEmpty() ||
            etDob.text.isNullOrEmpty() ||
            etCnic.text.isNullOrEmpty() ||
            etMobile.text.isNullOrEmpty()
        ) {
            Snackbar.make(btnSave, "All fields are required.", Snackbar.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission denied to read storage", Toast.LENGTH_SHORT).show()
        }
    }
}