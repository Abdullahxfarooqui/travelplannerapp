package com.Travelplannerfyp.travelplannerapp.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    /**
     * Format a double amount as PKR currency
     */
    fun formatAsPKR(amount: Double): String {
        val formatter = NumberFormat.getInstance(Locale("en", "PK"))
        formatter.maximumFractionDigits = 0 // No decimal points for PKR
        return "Rs. ${formatter.format(amount)}"
    }
    
    /**
     * Format a string amount as PKR currency
     */
    fun formatAsPKR(amount: String): String {
        return try {
            val numericAmount = amount.replace(",", "").toDoubleOrNull() ?: 0.0
            formatAsPKR(numericAmount)
        } catch (e: Exception) {
            "Rs. $amount"
        }
    }
    
    /**
     * Format a string amount as PKR currency with custom prefix
     */
    fun formatAsPKR(amount: String, prefix: String): String {
        return try {
            val numericAmount = amount.replace(",", "").toDoubleOrNull() ?: 0.0
            val formatter = NumberFormat.getInstance(Locale("en", "PK"))
            formatter.maximumFractionDigits = 0 // No decimal points for PKR
            "$prefix ${formatter.format(numericAmount)}"
        } catch (e: Exception) {
            "$prefix $amount"
        }
    }
}