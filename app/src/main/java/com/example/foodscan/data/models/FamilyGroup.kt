package com.example.foodscan.data.models

data class FamilyGroup(
    val groupId: String = "",
    val groupName: String = "",
    val adminUserId: String = "",
    val members: List<String> = emptyList(),  // List of user IDs
    val createdAt: Long = System.currentTimeMillis()
)