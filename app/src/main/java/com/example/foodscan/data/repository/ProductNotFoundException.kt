package com.example.foodscan.data.repository

import com.example.foodscan.data.models.Product

class ProductNotFoundException(
    message: String,
    val minimalProduct: Product? = null
) : Exception(message)
