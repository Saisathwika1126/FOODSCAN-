package com.example.foodscan.ui.viewmodel

import com.google.firebase.auth.FirebaseUser

sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
}