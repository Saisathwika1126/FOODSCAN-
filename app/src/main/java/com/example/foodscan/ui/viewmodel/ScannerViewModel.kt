package com.example.foodscan.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodscan.data.models.Product
import com.example.foodscan.data.repository.ProductRepository
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScannerViewModel(context: Context) : ViewModel() {

    private val productRepository = ProductRepository(context)
    private val TAG = "ScannerViewModel"

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _scannedBarcode = MutableStateFlow<String?>(null)
    val scannedBarcode: StateFlow<String?> = _scannedBarcode.asStateFlow()

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _isCameraBack = MutableStateFlow(true)
    val isCameraBack: StateFlow<Boolean> = _isCameraBack.asStateFlow()

    fun resetScan() {
        _scannedBarcode.value = null
        _product.value = null
        _errorMessage.value = null
        _isLoading.value = false
        _scanState.value = ScanState.Idle
    }

    // Fixed: Added resetToIdle as an alias for resetScan to resolve compilation error
    fun resetToIdle() {
        resetScan()
    }

    fun onBarcodeDetected(barcode: Barcode) {
        val value = barcode.rawValue ?: return
        
        // Prevent multiple simultaneous requests while one is in progress
        if (_isLoading.value || _scanState.value is ScanState.Processing) {
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "🔍 Camera scan detected: $value")

            resetToIdle()  
            _scannedBarcode.value = value
            _isLoading.value = true
            _scanState.value = ScanState.Processing

            try {
                val result = productRepository.getProductByBarcode(value)

                result.onSuccess { product ->
                    Log.d(TAG, "✅ Camera scan success: ${product.productName}")
                    _product.value = product
                    _isLoading.value = false
                    _scanState.value = ScanState.Success(product)
                }.onFailure { error ->
                    Log.e(TAG, "❌ Camera scan failed: ${error.message}")
                    _errorMessage.value = error.message
                    _isLoading.value = false
                    _scanState.value = ScanState.Error(error.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Crash during lookup", e)
                _errorMessage.value = "An error occurred: ${e.message}"
                _scanState.value = ScanState.Error(_errorMessage.value!!)
                _isLoading.value = false
            }
        }
    }

    fun processManualBarcode(barcode: String) {
        if (barcode.isBlank()) return
        
        // Prevent multiple simultaneous requests
        if (_isLoading.value || _scanState.value is ScanState.Processing) {
            return
        }

        viewModelScope.launch {
            resetToIdle()
            _scannedBarcode.value = barcode
            _isLoading.value = true
            _scanState.value = ScanState.Processing

            try {
                val result = productRepository.getProductByBarcode(barcode)

                result.onSuccess { product ->
                    _product.value = product
                    _isLoading.value = false
                    _scanState.value = ScanState.Success(product)
                }.onFailure { error ->
                    _errorMessage.value = error.message
                    _isLoading.value = false
                    _scanState.value = ScanState.Error(error.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                _errorMessage.value = "An error occurred: ${e.message}"
                _isLoading.value = false
                _scanState.value = ScanState.Error(_errorMessage.value!!)
            }
        }
    }

    fun toggleTorch() {
        _isTorchOn.value = !_isTorchOn.value
    }

    fun switchCamera() {
        _isCameraBack.value = !_isCameraBack.value
    }

    fun onScanError(error: String) {
        _errorMessage.value = error
        _scanState.value = ScanState.Error(error)
    }

    sealed class ScanState {
        object Idle : ScanState()
        object Processing : ScanState()
        data class Success(val product: Product) : ScanState()
        data class Error(val message: String) : ScanState()
    }
}
