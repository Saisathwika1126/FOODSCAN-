package com.example.foodscan.utils

import com.example.foodscan.data.models.Product
import com.example.foodscan.data.models.UserProfile

object RewardSystem {

    fun calculatePoints(product: Product, userProfile: UserProfile?): Int {
        var points = 10 // Base points for scanning

        // Bonus for healthy choices
        if (product.nutritionFacts.sugar < 10) points += 5
        if (product.nutritionFacts.salt < 0.5) points += 5
        if (product.nutritionFacts.fat < 5) points += 5
        if (product.nutritionFacts.fiber > 3) points += 10

        // Age-based bonus for kids
        if (userProfile?.age != null && userProfile.age < 13) {
            points += 5 // Extra encouragement for kids
        }

        return points
    }

    fun getBadge(points: Int): Badge {
        return when {
            points >= 1000 -> Badge.PLATINUM
            points >= 500 -> Badge.GOLD
            points >= 250 -> Badge.SILVER
            points >= 100 -> Badge.BRONZE
            else -> Badge.NOVICE
        }
    }

    enum class Badge(val title: String, val emoji: String, val color: String) {
        NOVICE("Food Explorer", "🌱", "#8BC34A"),
        BRONZE("Snack Detective", "🥉", "#CD7F32"),
        SILVER("Nutrition Hero", "🥈", "#C0C0C0"),
        GOLD("Food Master", "🥇", "#FFD700"),
        PLATINUM("Legendary Eater", "🏆", "#E5E4E2")
    }
}