package com.Travelplannerfyp.travelplannerapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TravelPlannerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}