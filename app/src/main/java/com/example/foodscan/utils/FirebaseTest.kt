package com.yourname.foodscan.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.app

object FirebaseTest {
    private const val TAG = "FirebaseTest"

    fun testConnection() {
        // Test 1: Check Firebase initialization
        Log.d(TAG, "Firebase initialized: ${Firebase.app != null}")

        // Test 2: Test Authentication
        val auth = FirebaseAuth.getInstance()
        Log.d(TAG, "Auth instance: ${auth != null}")

        // Test 3: Test Firestore
        val db = FirebaseFirestore.getInstance()
        Log.d(TAG, "Firestore instance: ${db != null}")

        // Test 4: Test FCM token
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "FCM Token: ${task.result}")
                } else {
                    Log.e(TAG, "FCM Token failed", task.exception)
                }
            }
    }
}