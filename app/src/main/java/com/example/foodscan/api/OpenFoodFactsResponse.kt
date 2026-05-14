package com.example.foodscan.api

import java.util.Locale
import com.google.gson.annotations.SerializedName
import com.example.foodscan.data.models.Product
import com.example.foodscan.data.models.NutritionFacts

/**
 * Root response from Open Food Facts API
 */
data class OpenFoodFactsResponse(
    val code: String,
    val product: ProductResponse?,
    val status: Int,
    @SerializedName("status_verbose")
    val statusVerbose: String
)

/**
 * Product details from API
 */
data class ProductResponse(
    @SerializedName("product_name")
    val productName: String = "",

    val brands: String = "",

    @SerializedName("ingredients_text")
    val ingredientsText: String = "",

    @SerializedName("ingredients_text_en")
    val ingredientsTextEn: String = "",

    @SerializedName("allergens")
    val allergens: String = "",

    @SerializedName("allergens_tags")
    val allergensTags: List<String> = emptyList(),

    @SerializedName("nutriments")
    val nutriments: NutrimentsResponse = NutrimentsResponse(),

    @SerializedName("image_url")
    val imageUrl: String = "",

    @SerializedName("image_small_url")
    val imageSmallUrl: String = "",

    @SerializedName("quantity")
    val quantity: String = "",

    @SerializedName("serving_size")
    val servingSize: String = "",

    @SerializedName("serving_quantity")
    val servingQuantity: Double = 0.0,

    @SerializedName("ecoscore_grade")
    val ecoscoreGrade: String = "",

    @SerializedName("nutrition_grades")
    val nutritionGrades: String = ""
) {
    /**
     * Convert API response to our app's Product model
     */
    fun toProduct(barcode: String): Product {
        return Product(
            barcode = barcode,
            productName = productName.ifEmpty { "Unknown Product" },
            brand = brands,
            ingredients = parseIngredients(),
            allergens = parseAllergens(),
            nutritionFacts = nutriments.toNutritionFacts(servingSize, servingQuantity),
            imageUrl = imageUrl.ifEmpty { imageSmallUrl },
            quantity = quantity
        )
    }

    /**
     * Parse ingredients from text
     */
    private fun parseIngredients(): List<String> {
        val ingredientsText = if (ingredientsTextEn.isNotEmpty()) {
            ingredientsTextEn
        } else {
            this.ingredientsText
        }

        return if (ingredientsText.isNotEmpty()) {
            ingredientsText.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
    }

    /**
     * Parse allergens from tags
     */
    private fun parseAllergens(): List<String> {
        return allergensTags.map { tag ->
            tag.replace("en:", "") // Remove language prefix
                .replace("-", " ")
                .capitalize()
        }
    }
}

/**
 * Nutritional information from API
 */
data class NutrimentsResponse(
    // Energy (kcal)
    @SerializedName("energy-kcal")
    val energyKcal: Double = 0.0,

    @SerializedName("energy-kcal_100g")
    val energyKcalPer100g: Double = 0.0,

    // Sugar
    @SerializedName("sugars")
    val sugars: Double = 0.0,

    @SerializedName("sugars_100g")
    val sugarsPer100g: Double = 0.0,

    // Salt
    @SerializedName("salt")
    val salt: Double = 0.0,

    @SerializedName("salt_100g")
    val saltPer100g: Double = 0.0,

    // Fat
    @SerializedName("fat")
    val fat: Double = 0.0,

    @SerializedName("fat_100g")
    val fatPer100g: Double = 0.0,

    // Protein
    @SerializedName("proteins")
    val proteins: Double = 0.0,

    @SerializedName("proteins_100g")
    val proteinsPer100g: Double = 0.0,

    // Fiber
    @SerializedName("fiber")
    val fiber: Double = 0.0,

    @SerializedName("fiber_100g")
    val fiberPer100g: Double = 0.0,

    // Carbohydrates
    @SerializedName("carbohydrates")
    val carbohydrates: Double = 0.0,

    @SerializedName("carbohydrates_100g")
    val carbohydratesPer100g: Double = 0.0,

    // Saturated fat
    @SerializedName("saturated-fat")
    val saturatedFat: Double = 0.0,

    @SerializedName("saturated-fat_100g")
    val saturatedFatPer100g: Double = 0.0
) {
    /**
     * Convert to our app's NutritionFacts model
     */
    fun toNutritionFacts(servingSize: String, servingQuantity: Double): NutritionFacts {
        return NutritionFacts(
            calories = energyKcalPer100g,
            caloriesUnit = "kcal",
            sugar = sugarsPer100g,
            sugarUnit = "g",
            salt = saltPer100g,
            saltUnit = "g",
            fat = fatPer100g,
            fatUnit = "g",
            protein = proteinsPer100g,
            proteinUnit = "g",
            fiber = fiberPer100g,
            fiberUnit = "g",
            carbohydrates = carbohydratesPer100g,
            carbohydratesUnit = "g",
            saturatedFat = saturatedFatPer100g,
            saturatedFatUnit = "g",
            servingSize = servingSize,
            servingQuantity = servingQuantity
        )
    }
}

/**
 * Extension function to capitalize first letter
 */
fun String.capitalize(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }
}
