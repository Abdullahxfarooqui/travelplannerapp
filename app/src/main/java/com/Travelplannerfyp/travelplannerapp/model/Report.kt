package com.Travelplannerfyp.travelplannerapp.model

data class Report(
    val reportId: String? = null,
    val reporterId: String? = null,
    val reportedItemId: String? = null,
    val reportedItemType: String? = null, // e.g., "trip" or "organizer"
    val reason: String? = null,
    val timestamp: String? = null,
    val status: String? = null // e.g., "unresolved", "resolved"
) 