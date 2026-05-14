package com.example.foodscan.ui.navigation

import kotlinx.serialization.Serializable

// Sealed class for all app screens (type-safe navigation)
@Serializable
sealed class Screen {
    @Serializable
    data object Login : Screen()

    @Serializable
    data object Register : Screen()

    @Serializable
    data object ProfileSelection : Screen()

    @Serializable
    data object Home : Screen()

    @Serializable
    data object Profile : Screen()

    @Serializable
    data object Scanner : Screen()

    @Serializable
    data object ManualEntry : Screen()

    @Serializable
    data object History : Screen()

    @Serializable
    data class ProductResult(val barcode: String) : Screen()
}