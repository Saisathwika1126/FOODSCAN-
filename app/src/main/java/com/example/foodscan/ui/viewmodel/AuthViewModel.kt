package com.example.foodscan.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Check if user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _authState.value = AuthState.Authenticated(currentUser)
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    _authState.value = AuthState.Authenticated(user)
                } else {
                    _errorMessage.value = "Login failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = when {
                    e.message?.contains("no user record") == true ->
                        "No account found with this email"
                    e.message?.contains("password is invalid") == true ->
                        "Incorrect password"
                    e.message?.contains("email address is badly formatted") == true ->
                        "Invalid email format"
                    else -> "Login failed: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    _authState.value = AuthState.Authenticated(user)
                } else {
                    _errorMessage.value = "Registration failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = when {
                    e.message?.contains("email address is already in use") == true ->
                        "Email already registered"
                    e.message?.contains("weak password") == true ->
                        "Password should be at least 6 characters"
                    e.message?.contains("email address is badly formatted") == true ->
                        "Invalid email format"
                    else -> "Registration failed: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }
}