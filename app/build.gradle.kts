plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-parcelize") // Add this line for Parcelize
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.travelplannerapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.travelplannerapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // SafetyNet for reCAPTCHA
    implementation("com.google.android.gms:play-services-safetynet:18.0.1")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.crashlytics.buildtools)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))
    implementation (("com.google.firebase:firebase-storage:20.3.0")) // or latest
    implementation("com.squareup.picasso:picasso:2.8")
    
    // Firebase Storage for image uploads
    implementation("com.google.firebase:firebase-storage-ktx")
    
    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth-ktx")
    
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    // Activity Result API for image picking
    implementation("androidx.activity:activity-ktx:1.8.2")
    
    // CircleImageView for circular profile images
    implementation("de.hdodenhof:circleimageview:3.1.0")

    implementation("com.google.code.gson:gson:2.10.1")

    // OkHttp for network requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

}