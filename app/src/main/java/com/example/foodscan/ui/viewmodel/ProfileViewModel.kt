package com.example.foodscan.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodscan.data.models.HealthCondition
import com.example.foodscan.data.models.UserProfile
import com.example.foodscan.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val repository = ProfileRepository()

    private val _allProfiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val allProfiles: StateFlow<List<UserProfile>> = _allProfiles

    private val _activeProfile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _activeProfile // Kept as 'profile' for backward compatibility

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    init {
        observeAllProfiles()
    }

    private fun observeAllProfiles() {
        viewModelScope.launch {
            repository.getAllProfilesFlow()
                .catch { error ->
                    _errorMessage.value = "Profile error: ${error.message}"
                }
                .collect { result ->
                    result.onSuccess { profiles ->
                        _allProfiles.value = profiles
                        // If we have profiles but no active one, and there's only one, set it
                        if (_activeProfile.value == null && profiles.size == 1) {
                            _activeProfile.value = profiles[0]
                        } else if (_activeProfile.value != null) {
                            // Update active profile if it's in the list
                            profiles.find { it.profileId == _activeProfile.value?.profileId }?.let {
                                _activeProfile.value = it
                            }
                        }
                    }.onFailure { error ->
                        _errorMessage.value = "Failed to load profiles: ${error.message}"
                    }
                }
        }
    }

    fun selectProfile(profile: UserProfile) {
        _activeProfile.value = profile
    }

    fun createProfile(email: String, displayName: String = "New Profile", avatarEmoji: String = "👤") {
        if (_allProfiles.value.size >= 5) {
            _errorMessage.value = "Maximum 5 profiles allowed"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null

            val isPrimary = _allProfiles.value.isEmpty()
            val newProfile = UserProfile(
                email = email,
                displayName = displayName,
                avatarEmoji = avatarEmoji,
                isPrimary = isPrimary
            )

            val result = repository.createUserProfile(newProfile)
            result.onSuccess { createdProfile ->
                _successMessage.value = "Profile '$displayName' created! ✨"
                if (isPrimary) _activeProfile.value = createdProfile
            }.onFailure { error ->
                _errorMessage.value = "Failed to create profile: ${error.message}"
            }

            _isSaving.value = false
        }
    }

    fun updateProfile(
        displayName: String,
        age: Int,
        weight: Double,
        height: Double,
        allergies: List<String>,
        dietaryPreferences: List<String>,
        healthConditions: List<HealthCondition>
    ) {
        viewModelScope.launch {
            val currentProfile = _activeProfile.value ?: return@launch

            _isSaving.value = true
            _errorMessage.value = null
            _successMessage.value = null

            val updatedProfile = currentProfile.copy(
                displayName = displayName,
                age = age,
                weight = weight,
                height = height,
                allergies = allergies,
                dietaryPreferences = dietaryPreferences,
                healthConditions = healthConditions
            )

            val result = repository.updateProfile(updatedProfile)
            result.onSuccess {
                _successMessage.value = "Profile updated successfully! ✨"
            }.onFailure { error ->
                _errorMessage.value = "Update failed: ${error.message}"
            }

            _isSaving.value = false
        }
    }

    fun clearProfile() {
        _activeProfile.value = null
        _allProfiles.value = emptyList()
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}