package com.Travelplannerfyp.travelplannerapp.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    fun formatAsPKR(amount: Double): String {
        val formatter = NumberFormat.getInstance(Locale("en", "PK"))
        return "Rs. ${formatter.format(amount)}"
    }
} 