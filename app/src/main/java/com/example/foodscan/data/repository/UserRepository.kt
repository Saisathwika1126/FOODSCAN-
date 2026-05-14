package com.example.foodscan.data.repository

import com.example.foodscan.data.models.UserProfile
import com.example.foodscan.data.models.FamilyGroup
import com.example.foodscan.data.models.ScanHistory
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    // User operations
    suspend fun createUserProfile(userProfile: UserProfile): Result<String>
    suspend fun getUserProfile(userId: String): Result<UserProfile?>
    suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit>
    suspend fun deleteUserProfile(userId: String): Result<Unit>

    // Family operations
    suspend fun createFamilyGroup(familyGroup: FamilyGroup): Result<String>
    suspend fun getFamilyGroup(groupId: String): Result<FamilyGroup?>
    suspend fun addMemberToFamily(groupId: String, userId: String): Result<Unit>
    suspend fun removeMemberFromFamily(groupId: String, userId: String): Result<Unit>

    // Scan history
    suspend fun saveScanHistory(scanHistory: ScanHistory): Result<String>
    suspend fun getUserScanHistory(userId: String): Flow<List<ScanHistory>>
}
