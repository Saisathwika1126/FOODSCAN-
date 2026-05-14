package com.example.foodscan.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodscan.data.models.ScanHistory
import com.example.foodscan.data.repository.FirebaseUserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {
    private val userRepository = FirebaseUserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _history = MutableStateFlow<List<ScanHistory>>(emptyList())
    val history: StateFlow<List<ScanHistory>> = _history.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var historyJob: kotlinx.coroutines.Job? = null

    fun loadHistoryForProfile(profileId: String) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            _isLoading.value = true
            userRepository.getProfileScanHistory(profileId)
                .onEach { _history.value = it }
                .onCompletion { _isLoading.value = false }
                .catch { /* handle error */ }
                .collect()
        }
    }

    fun saveScan(historyItem: ScanHistory) {
        viewModelScope.launch {
            userRepository.saveScanHistory(historyItem)
        }
    }
}
