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

    /**
     * Format a double amount as USD currency
     */
    fun formatAsUSD(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        return formatter.format(amount)
    }

    /**
     * Format a double amount as EUR currency
     */
    fun formatAsEUR(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.GERMANY)
        return formatter.format(amount)
    }

    /**
     * Format a double amount as a given currency code (e.g., PKR, USD, EUR)
     */
    fun format(amount: Double, currencyCode: String): String {
        val locale = when (currencyCode) {
            "PKR" -> Locale("en", "PK")
            "USD" -> Locale.US
            "EUR" -> Locale.GERMANY
            else -> Locale.getDefault()
        }
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = java.util.Currency.getInstance(currencyCode)
        return formatter.format(amount)
    }
}