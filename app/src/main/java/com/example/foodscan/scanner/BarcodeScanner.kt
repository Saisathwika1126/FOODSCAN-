package com.example.foodscan.scanner

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BarcodeScanner(
    private val context: Context,
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private val TAG = "BarcodeScanner"

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    processBarcodes(barcodes, imageProxy)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Barcode scanning failed", e)
                    imageProxy.close()
                }
                .addOnCompleteListener {
                    // ImageProxy will be closed in processBarcodes or on failure
                }
        } else {
            imageProxy.close()
        }
    }

    private fun processBarcodes(barcodes: List<Barcode>, imageProxy: ImageProxy) {
        for (barcode in barcodes) {
            val barcodeValue = barcode.rawValue
            if (!barcodeValue.isNullOrEmpty()) {
                Log.d(TAG, "Barcode detected: $barcodeValue")

                // Use coroutine to handle the result
                CoroutineScope(Dispatchers.Main).launch {
                    onBarcodeDetected(barcodeValue)
                }

                // Close after first valid barcode
                imageProxy.close()
                return
            }
        }
        imageProxy.close()
    }
}