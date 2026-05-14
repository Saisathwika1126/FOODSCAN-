package com.example.foodscan.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val barcode: String = "",
    val productName: String = "",
    val brand: String = "",
    val ingredients: List<String> = emptyList(),
    val allergens: List<String> = emptyList(),
    val imageUrl: String = "",
    val nutritionFacts: NutritionFacts = NutritionFacts(),
    val quantity: String = ""
) : Parcelable

@Parcelize
data class NutritionFacts(
    val calories: Double = 0.0,
    val caloriesUnit: String = "kcal",
    val sugar: Double = 0.0,
    val sugarUnit: String = "g",
    val salt: Double = 0.0,
    val saltUnit: String = "g",
    val fat: Double = 0.0,
    val fatUnit: String = "g",
    val protein: Double = 0.0,
    val proteinUnit: String = "g",
    val fiber: Double = 0.0,
    val fiberUnit: String = "g",
    val carbohydrates: Double = 0.0,
    val carbohydratesUnit: String = "g",
    val saturatedFat: Double = 0.0,
    val saturatedFatUnit: String = "g",
    val calcium: Double = 0.0,
    val servingSize: String = "100g",
    val servingQuantity: Double = 0.0
) : Parcelable

@Parcelize
enum class HealthScore : Parcelable {
    GOOD, MODERATE, HARMFUL
}

@Parcelize
enum class RestrictionLevel : Parcelable {
    SAFE, CAUTION, AVOID, DANGER
}

@Parcelize
data class ServingSuggestion(
    val recommendedPercentage: Double = 1.0,
    val recommendedAmount: String = "",
    val actualQuantity: String = "",
    val portionSize: String = "",
    val caloriesPerPortion: Int = 0,
    val sugarPerPortion: Double = 0.0,
    val saltPerPortion: Double = 0.0,
    val maxPerDay: Int = 1,
    val funFact: String = "",
    val emoji: String = "✨",
    val healthScore: HealthScore = HealthScore.MODERATE,
    val healthMessage: String = "",
    val warningMessage: String? = null,
    val restrictionLevel: RestrictionLevel = RestrictionLevel.SAFE,
    
    // Visual comparison fields
    val visualComparison: String = "",
    val comparisonEmoji: String = "",
    val householdItem: String = ""
) : Parcelable

@Parcelize
data class PostSnackTip(
    val title: String = "",
    val message: String = "",
    val emoji: String = "💡",
    val action: String = ""
) : Parcelable
