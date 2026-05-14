package com.example.foodscan.data.models

data class AllergyAlert(
    val hasAllergens: Boolean = false,
    val allergens: List<String> = emptyList(),
    val severity: Severity = Severity.INFO
)

enum class Severity {
    INFO,
    WARNING,
    DANGER
}