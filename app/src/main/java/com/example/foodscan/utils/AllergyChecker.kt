package com.example.foodscan.utils

import android.util.Log

class AllergyChecker {

    private val TAG = "AllergyChecker"

    /**
     * Check if product contains any user allergens
     */
    fun checkForAllergens(
        productIngredients: List<String>,
        userAllergies: List<String>
    ): AllergyAlert {
        Log.d(TAG, "Checking allergies: $userAllergies against ingredients")

        if (userAllergies.isEmpty()) {
            return AllergyAlert(hasAllergens = false)
        }

        val foundAllergens = mutableListOf<String>()

        // Check each ingredient against user allergies
        for (ingredient in productIngredients) {
            val ingredientLower = ingredient.lowercase()

            for (allergy in userAllergies) {
                val allergyLower = allergy.lowercase()

                // Check if ingredient contains allergen
                if (ingredientLower.contains(allergyLower)) {
                    foundAllergens.add(allergy)
                    Log.d(TAG, "Found allergen: $allergy in ingredient: $ingredient")
                }

                // Check common variations
                when (allergyLower) {
                    "peanut" -> {
                        if (ingredientLower.contains("peanut") ||
                            ingredientLower.contains("groundnut") ||
                            ingredientLower.contains("arachis")) {
                            foundAllergens.add(allergy)
                        }
                    }
                    "milk" -> {
                        if (ingredientLower.contains("milk") ||
                            ingredientLower.contains("dairy") ||
                            ingredientLower.contains("whey") ||
                            ingredientLower.contains("casein") ||
                            ingredientLower.contains("lactose")) {
                            foundAllergens.add(allergy)
                        }
                    }
                    "egg" -> {
                        if (ingredientLower.contains("egg") ||
                            ingredientLower.contains("albumin") ||
                            ingredientLower.contains("ovalbumin")) {
                            foundAllergens.add(allergy)
                        }
                    }
                    "wheat" -> {
                        if (ingredientLower.contains("wheat") ||
                            ingredientLower.contains("gluten") ||
                            ingredientLower.contains("farina") ||
                            ingredientLower.contains("semolina")) {
                            foundAllergens.add(allergy)
                        }
                    }
                    "soy" -> {
                        if (ingredientLower.contains("soy") ||
                            ingredientLower.contains("soya") ||
                            ingredientLower.contains("tofu") ||
                            ingredientLower.contains("edamame") ||
                            ingredientLower.contains("miso") ||
                            ingredientLower.contains("tempeh")) {
                            foundAllergens.add(allergy)
                        }
                    }
                    "tree nut" -> {
                        val nuts = listOf("almond", "walnut", "pecan", "cashew",
                            "hazelnut", "pistachio", "macadamia", "brazil nut")
                        if (nuts.any { ingredientLower.contains(it) }) {
                            foundAllergens.add(allergy)
                        }
                    }
                }
            }
        }

        return AllergyAlert(
            hasAllergens = foundAllergens.isNotEmpty(),
            allergens = foundAllergens.distinct(),
            severity = when {
                foundAllergens.size > 2 -> Severity.DANGER
                foundAllergens.isNotEmpty() -> Severity.WARNING
                else -> Severity.INFO
            }
        )
    }

    data class AllergyAlert(
        val hasAllergens: Boolean,
        val allergens: List<String> = emptyList(),
        val severity: Severity = Severity.INFO
    )

    enum class Severity {
        INFO,
        WARNING,
        DANGER
    }
}