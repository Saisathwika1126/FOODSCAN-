
package com.example.foodscan.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

enum class HealthCondition {
    NONE,
    DIABETES,        // Sugar restriction
    HYPERTENSION,    // Salt restriction
    HEART_DISEASE,   // Fat restriction
    KIDNEY_ISSUE,    // Protein/Salt restriction
    OBESITY,         // Calorie restriction
    PREGNANT,        // Special considerations
    LACTATING,       // Special considerations
    ATHLETE          // Higher protein needs
}

@Parcelize
data class UserProfile(
    val profileId: String = UUID.randomUUID().toString(),
    val userId: String = "", // This is the Firebase UID (the "Account" owner)
    val email: String = "",
    val displayName: String = "",
    val age: Int = 0,
    val weight: Double = 0.0, // in kg
    val allergies: List<String> = emptyList(),
    val dietaryPreferences: List<String> = emptyList(),
    val healthConditions: List<HealthCondition> = emptyList(),
    val totalPoints: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),

    // New fields for enhanced functionality
    val avatarEmoji: String = "👤",
    val favoriteProducts: List<String> = emptyList(),
    val scanCount: Int = 0,
    val streakDays: Int = 0,
    val lastScanDate: Long = 0L,
    val badgeLevel: String = "Novice",
    val dailyGoal: Int = 5,
    val dailyProgress: Int = 0,
    val hydrationReminders: Boolean = true,
    val funFactsEnabled: Boolean = true,
    
    // Health metrics
    val height: Double = 0.0, // in cm
    val activityLevel: String = "Moderate",
    val dietaryGoal: String = "Maintain",
    val targetCalories: Int = 2000,
    
    val isPrimary: Boolean = false // To identify the main profile
) : Parcelable {

    fun calculateBMI(): Double {
        return if (weight > 0 && height > 0) {
            weight / ((height / 100) * (height / 100))
        } else 0.0
    }

    fun getBMICategory(): String {
        val bmi = calculateBMI()
        return when {
            bmi <= 0 -> "Unknown"
            bmi < 18.5 -> "Underweight"
            bmi < 25 -> "Normal"
            bmi < 30 -> "Overweight"
            else -> "Obese"
        }
    }

    fun getAgeGroup(): String {
        return when {
            age <= 0 -> "Unknown"
            age < 6 -> "Toddler"
            age < 13 -> "Child"
            age < 18 -> "Teen"
            age < 60 -> "Adult"
            else -> "Senior"
        }
    }

    fun getAgeBasedAvatar(): String {
        return when {
            age <= 0 -> "👤"
            age < 6 -> "👶"
            age < 13 -> "🧒"
            age < 18 -> "🧑"
            age < 60 -> "👨"
            else -> "👴"
        }
    }

    fun hasAllergies(): Boolean = allergies.isNotEmpty()

    fun isAllergicTo(ingredient: String): Boolean {
        return allergies.any {
            ingredient.contains(it, ignoreCase = true)
        }
    }

    fun getRecommendedWaterIntake(): Double {
        return when {
            age < 6 -> 1.0
            age < 13 -> 1.5
            age < 18 -> 2.0
            else -> 2.5
        }
    }

    fun getActivityMultiplier(): Double {
        return when (activityLevel) {
            "Sedentary" -> 1.2
            "Light" -> 1.375
            "Moderate" -> 1.55
            "Active" -> 1.725
            "Very Active" -> 1.9
            else -> 1.55
        }
    }

    fun calculateDailyCalories(): Int {
        val bmr = if (age > 0 && weight > 0 && height > 0) {
            (10 * weight) + (6.25 * height) - (5 * age) + 5
        } else {
            targetCalories.toDouble()
        }

        val tdee = bmr * getActivityMultiplier()

        return when (dietaryGoal) {
            "Lose" -> (tdee - 500).toInt()
            "Gain" -> (tdee + 500).toInt()
            else -> tdee.toInt()
        }.coerceAtLeast(1200)
    }

    // Check if user has specific health condition
    fun hasCondition(condition: HealthCondition): Boolean =
        healthConditions.contains(condition)
}