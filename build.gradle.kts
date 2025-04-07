// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

buildscript {
    dependencies {
        // Add the Google Services classpath for Firebase
        classpath("com.google.gms:google-services:4.4.2")
    }
}

// Clean task
task<Delete>("clean") {
    delete(rootProject.buildDir)
}
