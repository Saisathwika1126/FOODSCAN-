package com.example.foodscan.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.foodscan.data.models.UserProfile
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose

class ProfileRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val profilesCollection = db.collection("profiles")

    suspend fun createUserProfile(profile: UserProfile): Result<UserProfile> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No authenticated user"))
            val profileWithIds = profile.copy(userId = userId)
            
            // We use profileId as the document ID because one user can have multiple profiles
            profilesCollection.document(profileWithIds.profileId).set(profileWithIds, SetOptions.merge()).await()
            Result.success(profileWithIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfiles(): Result<List<UserProfile>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.success(emptyList())
            val querySnapshot = profilesCollection.whereEqualTo("userId", userId).get().await()
            val profiles = querySnapshot.toObjects(UserProfile::class.java)
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        return try {
            profilesCollection.document(profile.profileId).set(profile, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllProfilesFlow(): Flow<Result<List<UserProfile>>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: return@callbackFlow

        val listener = profilesCollection.whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val profiles = snapshot.toObjects(UserProfile::class.java)
                    trySend(Result.success(profiles))
                }
            }

        awaitClose { listener.remove() }
    }
}