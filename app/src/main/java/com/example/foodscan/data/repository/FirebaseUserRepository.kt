package com.example.foodscan.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.example.foodscan.data.models.UserProfile
import com.example.foodscan.data.models.FamilyGroup
import com.example.foodscan.data.models.ScanHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseUserRepository : UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Collection references
    private val usersCollection = db.collection("users")
    private val familiesCollection = db.collection("families")
    private val scansCollection = db.collection("scans")

    override suspend fun createUserProfile(userProfile: UserProfile): Result<String> {
        return try {
            val userId = userProfile.userId.ifEmpty { auth.currentUser?.uid ?: "" }
            if (userId.isEmpty()) {
                return Result.failure(Exception("No authenticated user"))
            }

            val userWithId = userProfile.copy(userId = userId)
            usersCollection.document(userId).set(userWithId).await()
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(userId: String): Result<UserProfile?> {
        return try {
            val document = usersCollection.document(userId).get().await()
            val userProfile = document.toObject(UserProfile::class.java)
            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit> {
        return try {
            usersCollection.document(userProfile.userId).set(userProfile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUserProfile(userId: String): Result<Unit> {
        return try {
            usersCollection.document(userId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createFamilyGroup(familyGroup: FamilyGroup): Result<String> {
        return try {
            val documentRef = familiesCollection.document()
            val groupWithId = familyGroup.copy(groupId = documentRef.id)
            documentRef.set(groupWithId).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFamilyGroup(groupId: String): Result<FamilyGroup?> {
        return try {
            val document = familiesCollection.document(groupId).get().await()
            val familyGroup = document.toObject(FamilyGroup::class.java)
            Result.success(familyGroup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addMemberToFamily(groupId: String, userId: String): Result<Unit> {
        return try {
            familiesCollection.document(groupId).update("members",
                FieldValue.arrayUnion(userId)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeMemberFromFamily(groupId: String, userId: String): Result<Unit> {
        return try {
            familiesCollection.document(groupId).update("members",
                FieldValue.arrayRemove(userId)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveScanHistory(scanHistory: ScanHistory): Result<String> {
        return try {
            val documentRef = scansCollection.document()
            val scanWithId = scanHistory.copy(id = documentRef.id)
            documentRef.set(scanWithId).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserScanHistory(userId: String): Flow<List<ScanHistory>> {
        return scansCollection
            .whereEqualTo("userId", userId)
            .orderBy("scannedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(ScanHistory::class.java) }
            }
    }

    fun getProfileScanHistory(profileId: String): Flow<List<ScanHistory>> {
        return scansCollection
            .whereEqualTo("profileId", profileId)
            .orderBy("scannedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(ScanHistory::class.java) }
            }
    }
}
