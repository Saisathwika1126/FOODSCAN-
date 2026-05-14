package com.example.foodscan.data.models

data class ScanHistory(
    val id: String = "",
    val userId: String = "", // Account ID
    val profileId: String = "", // Specific Profile ID
    val productBarcode: String = "",
    val productName: String = "",
    val scannedAt: Long = System.currentTimeMillis(),
    val recommendedServingPercentage: Double = 1.0,
    val allergyAlert: Boolean = false,
    val pointsEarned: Int = 0,
    val emoji: String = "🍽️"
)
