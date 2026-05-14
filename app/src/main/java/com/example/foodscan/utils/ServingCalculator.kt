package com.example.foodscan.utils

import com.example.foodscan.data.models.Product
import com.example.foodscan.data.models.UserProfile
import com.example.foodscan.data.models.HealthCondition
import com.example.foodscan.data.models.HealthScore
import com.example.foodscan.data.models.RestrictionLevel
import com.example.foodscan.data.models.ServingSuggestion
import com.example.foodscan.data.models.PostSnackTip

object ServingCalculator {

    private enum class ProductType {
        BISCUIT, CHOCOLATE, DRINK, CHIPS, CEREAL, YOGURT, ICE_CREAM, BREAD, FRUIT, VEGETABLE, UNKNOWN
    }

    private fun detectProductType(product: Product): ProductType {
        val name = product.productName.lowercase()
        return when {
            name.contains("biscuit") || name.contains("cookie") -> ProductType.BISCUIT
            name.contains("chocolate") || name.contains("choco") -> ProductType.CHOCOLATE
            name.contains("drink") || name.contains("soda") || name.contains("juice") -> ProductType.DRINK
            name.contains("chip") || name.contains("crisp") -> ProductType.CHIPS
            name.contains("cereal") || name.contains("oat") -> ProductType.CEREAL
            name.contains("yogurt") || name.contains("yoghurt") -> ProductType.YOGURT
            name.contains("ice cream") -> ProductType.ICE_CREAM
            name.contains("bread") || name.contains("toast") -> ProductType.BREAD
            name.contains("fruit") || name.contains("apple") || name.contains("banana") -> ProductType.FRUIT
            name.contains("veg") || name.contains("carrot") -> ProductType.VEGETABLE
            else -> ProductType.UNKNOWN
        }
    }

    private fun calculateHealthScore(product: Product, userProfile: UserProfile): HealthScore {
        var badPoints = 0
        var goodPoints = 0

        if (product.nutritionFacts.sugar > 15) badPoints += 2
        else if (product.nutritionFacts.sugar > 10) badPoints += 1
        else goodPoints += 2

        if (product.nutritionFacts.salt > 1.0) badPoints += 2
        else if (product.nutritionFacts.salt > 0.5) badPoints += 1
        else goodPoints += 2

        if (product.nutritionFacts.fat > 15) badPoints += 2
        else if (product.nutritionFacts.fat < 5) goodPoints += 1

        if (product.nutritionFacts.fiber > 3) goodPoints += 2
        if (product.nutritionFacts.protein > 8) goodPoints += 2

        return when {
            goodPoints >= badPoints + 2 -> HealthScore.GOOD
            badPoints >= goodPoints + 2 -> HealthScore.HARMFUL
            else -> HealthScore.MODERATE
        }
    }

    private fun calculateRestrictionLevel(
        product: Product,
        userProfile: UserProfile
    ): Pair<RestrictionLevel, String?> {
        var warning = ""
        var level = RestrictionLevel.SAFE

        userProfile.healthConditions.forEach { condition ->
            when (condition) {
                HealthCondition.DIABETES -> {
                    if (product.nutritionFacts.sugar > 10) {
                        warning += "⚠️ High sugar - not ideal for diabetes. "
                        level = if (level.ordinal < RestrictionLevel.CAUTION.ordinal) RestrictionLevel.CAUTION else level
                    }
                    if (product.nutritionFacts.sugar > 20) {
                        level = RestrictionLevel.AVOID
                    }
                }
                HealthCondition.HYPERTENSION -> {
                    if (product.nutritionFacts.salt > 0.5) {
                        warning += "🧂 High salt - watch your blood pressure. "
                        level = if (level.ordinal < RestrictionLevel.CAUTION.ordinal) RestrictionLevel.CAUTION else level
                    }
                    if (product.nutritionFacts.salt > 1.0) {
                        level = RestrictionLevel.AVOID
                    }
                }
                HealthCondition.HEART_DISEASE -> {
                    if (product.nutritionFacts.fat > 15) {
                        warning += "❤️ High fat - limited intake advised for heart health. "
                        level = if (level.ordinal < RestrictionLevel.CAUTION.ordinal) RestrictionLevel.CAUTION else level
                    }
                }
                else -> {}
            }
        }
        return Pair(level, if (warning.isNotEmpty()) warning else null)
    }

    private fun getBaseServingInGrams(type: ProductType): Double {
        return when (type) {
            ProductType.BISCUIT -> 40.0 // Approx 3-4 biscuits
            ProductType.CHOCOLATE -> 30.0
            ProductType.DRINK -> 250.0 // For drinks, ml is roughly grams
            ProductType.CHIPS -> 30.0
            ProductType.CEREAL -> 40.0
            ProductType.YOGURT -> 125.0
            ProductType.ICE_CREAM -> 100.0
            ProductType.BREAD -> 50.0 // Approx 2 slices
            ProductType.FRUIT -> 150.0
            ProductType.VEGETABLE -> 100.0
            else -> 50.0
        }
    }

    private fun getVisualComparison(productType: ProductType, amountGrams: Double): Triple<String, String, String> {
        return when (productType) {
            ProductType.BISCUIT -> {
                val num = (amountGrams / 15.0).toInt().coerceAtLeast(1)
                Triple("$num biscuit${if (num > 1) "s" else ""} (small stack)", "🍪", "About $num palm-sized biscuit${if (num > 1) "s" else ""}")
            }
            ProductType.CHOCOLATE -> {
                val num = (amountGrams / 10.0).toInt().coerceAtLeast(1)
                Triple("$num piece${if (num > 1) "s" else ""} (size of a thumb)", "🍫", "Roughly $num square${if (num > 1) "s" else ""} of a bar")
            }
            ProductType.DRINK -> {
                if (amountGrams >= 1000) Triple("1 Liter bottle", "🍶", "A full large water bottle")
                else if (amountGrams >= 500) Triple("Half Liter bottle", "🥤", "A standard water bottle")
                else if (amountGrams >= 250) Triple("One full glass", "🥛", "A regular drinking glass")
                else Triple("Small juice box size", "🧃", "A small tea cup")
            }
            ProductType.CHIPS -> {
                Triple("About 1 small bowl", "🥣", "One handful of chips")
            }
            ProductType.FRUIT -> {
                Triple("Size of a tennis ball", "🎾", "Roughly the size of your fist")
            }
            else -> {
                Triple("Size of a deck of cards", "🃏", "About the size of your palm")
            }
        }
    }

    fun calculateSafeServing(product: Product, user: UserProfile): ServingSuggestion {
        val productType = detectProductType(product)
        val healthScore = calculateHealthScore(product, user)
        val (restrictionLevel, warningMessage) = calculateRestrictionLevel(product, user)

        val ageMultiplier = when {
            user.age < 6 -> 0.25
            user.age < 13 -> 0.5
            user.age < 18 -> 0.75
            else -> 1.0
        }

        val healthMultiplier = when (healthScore) {
            HealthScore.GOOD -> 1.2
            HealthScore.MODERATE -> 1.0
            HealthScore.HARMFUL -> 0.5
        }

        val baseAmountGrams = getBaseServingInGrams(productType)
        val finalMultiplier = ageMultiplier * healthMultiplier
        val actualAmountGrams: Double = baseAmountGrams * finalMultiplier

        val portionSize = when {
            finalMultiplier >= 1.0 -> "Full"
            finalMultiplier >= 0.7 -> "Medium"
            finalMultiplier >= 0.4 -> "Small"
            else -> "Mini"
        }

        val actualQuantity = when (productType) {
            ProductType.BISCUIT -> {
                val numBiscuits = (actualAmountGrams / 15.0).toInt().coerceAtLeast(1)
                "${numBiscuits} biscuit${if (numBiscuits != 1) "s" else ""}"
            }
            ProductType.CHOCOLATE -> {
                val numPieces = (actualAmountGrams / 10.0).toInt().coerceAtLeast(1)
                "${numPieces} piece${if (numPieces != 1) "s" else ""}"
            }
            ProductType.DRINK -> {
                val ml = actualAmountGrams.toInt()
                if (ml >= 1000) String.format("%.1f L", ml / 1000.0) else "${ml}ml"
            }
            else -> "${actualAmountGrams.toInt()}g"
        }

        val caloriesPerPortion = (product.nutritionFacts.calories * (actualAmountGrams / 100.0)).toInt()
        val sugarPerPortion = product.nutritionFacts.sugar * (actualAmountGrams / 100.0)
        val saltPerPortion = product.nutritionFacts.salt * (actualAmountGrams / 100.0)

        val finalRestrictionLevel = if (healthScore == HealthScore.HARMFUL && restrictionLevel.ordinal < RestrictionLevel.AVOID.ordinal) {
            RestrictionLevel.AVOID
        } else {
            restrictionLevel
        }

        val (healthMessage, emoji, maxPerDay) = when (finalRestrictionLevel) {
            RestrictionLevel.SAFE -> Triple("✅ Safe for you", "😊", 3)
            RestrictionLevel.CAUTION -> Triple("🟡 Use caution", "🤔", 2)
            RestrictionLevel.AVOID -> Triple("🟠 Best to avoid", "⚠️", 1)
            RestrictionLevel.DANGER -> Triple("🔴 Do not consume", "🚫", 0)
        }

        val (visualComp, compEmoji, household) = getVisualComparison(productType, actualAmountGrams)

        return ServingSuggestion(
            recommendedPercentage = finalMultiplier,
            recommendedAmount = actualQuantity,
            maxPerDay = maxPerDay,
            funFact = when {
                product.nutritionFacts.sugar > 25 -> "🍬 High sugar alert!"
                product.nutritionFacts.fiber > 5 -> "🌾 Fiber superstar!"
                else -> "✨ Balanced choice!"
            },
            emoji = emoji,
            healthScore = healthScore,
            actualQuantity = actualQuantity,
            healthMessage = healthMessage,
            portionSize = portionSize,
            caloriesPerPortion = caloriesPerPortion,
            sugarPerPortion = sugarPerPortion,
            saltPerPortion = saltPerPortion,
            warningMessage = warningMessage,
            restrictionLevel = finalRestrictionLevel,
            visualComparison = visualComp,
            comparisonEmoji = compEmoji,
            householdItem = household
        )
    }

    fun getPostSnackTips(product: Product): List<PostSnackTip> {
        val tips = mutableListOf<PostSnackTip>()
        if (product.nutritionFacts.sugar > 15) {
            tips.add(PostSnackTip("💧 Hydrate", "Drink water after sugar to help your body process it better.", "🥤", "Drink Water"))
            tips.add(PostSnackTip("🚶 Walk it Off", "A 10-min walk can help stabilize blood sugar spikes.", "👟", "Light Walk"))
        }
        tips.add(PostSnackTip("🧘 Mindful Bite", "Chew slowly and enjoy every flavor of your snack.", "✨", "Mindfulness"))
        return tips
    }
}
