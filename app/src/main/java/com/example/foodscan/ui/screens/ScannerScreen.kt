@file:OptIn(
    com.google.accompanist.permissions.ExperimentalPermissionsApi::class,
    androidx.camera.core.ExperimentalGetImage::class
)
package com.example.foodscan.ui.screens

import android.Manifest
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodscan.ui.viewmodel.ScannerViewModel
import com.google.accompanist.permissions.*
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    scannerViewModel: ScannerViewModel = viewModel(),
    onBarcodeScanned: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onManualEntry: () -> Unit
) {
    val context = LocalContext.current
    val scanState by scannerViewModel.scanState.collectAsState()
    val isTorchOn by scannerViewModel.isTorchOn.collectAsState()
    val isCameraBack by scannerViewModel.isCameraBack.collectAsState()
    val isLoading by scannerViewModel.isLoading.collectAsState()
    val errorMessage by scannerViewModel.errorMessage.collectAsState()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Handle successful scan
    LaunchedEffect(scanState) {
        when (val state = scanState) {
            is ScannerViewModel.ScanState.Success -> {
                onBarcodeScanned(state.product.barcode)
            }
            else -> {}
        }
    }

    when {
        cameraPermissionState.status.isGranted -> {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    ScannerContent(
                        isTorchOn = isTorchOn,
                        isCameraBack = isCameraBack,
                        // Only detect if not already loading or successful
                        onBarcodeDetected = { 
                            if (!isLoading && scanState is ScannerViewModel.ScanState.Idle) {
                                scannerViewModel.onBarcodeDetected(it)
                            }
                        },
                        onScanError = { scannerViewModel.onScanError(it) },
                        onNavigateBack = onNavigateBack,
                        onToggleTorch = { scannerViewModel.toggleTorch() },
                        onSwitchCamera = { scannerViewModel.switchCamera() }
                    )

                    // Show loading overlay only if we are processing
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Analyzing barcode...",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                    
                    // Show error message if any
                    errorMessage?.let { error ->
                        Snackbar(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                            action = {
                                TextButton(onClick = { scannerViewModel.resetScan() }) {
                                    Text("Dismiss")
                                }
                            }
                        ) {
                            Text(error)
                        }
                    }
                }

                Button(
                    onClick = onManualEntry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("Or Enter Barcode Manually")
                }
            }
        }
        cameraPermissionState.status.shouldShowRationale -> {
            PermissionRationaleScreen(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                onNavigateBack = onNavigateBack
            )
        }
        else -> {
            PermissionDeniedScreen(
                onNavigateBack = onNavigateBack,
                onOpenSettings = {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun ScannerContent(
    isTorchOn: Boolean,
    isCameraBack: Boolean,
    onBarcodeDetected: (Barcode) -> Unit,
    onScanError: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onToggleTorch: () -> Unit,
    onSwitchCamera: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        try {
                            val preview = Preview.Builder().build()

                            val resolutionSelector = ResolutionSelector.Builder()
                                .setResolutionStrategy(
                                    ResolutionStrategy(
                                        android.util.Size(1280, 720),
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                    )
                                )
                                .build()

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setResolutionSelector(resolutionSelector)
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            val options = BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                                .build()

                            val barcodeScanner = BarcodeScanning.getClient(options)

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val inputImage = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )

                                    barcodeScanner.process(inputImage)
                                        .addOnSuccessListener { barcodes ->
                                            if (barcodes.isNotEmpty()) {
                                                onBarcodeDetected(barcodes[0])
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("Scanner", "❌ Scan failed", e)
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            val cameraSelector = if (isCameraBack) {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            } else {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            }

                            cameraProvider.unbindAll()

                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            
                            camera.cameraControl.enableTorch(isTorchOn)

                            preview.setSurfaceProvider(surfaceProvider)

                        } catch (e: Exception) {
                            Log.e("Camera", "Camera error", e)
                            onScanError("Camera error: ${e.message}")
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        ScannerOverlay(
            onNavigateBack = onNavigateBack,
            onToggleTorch = onToggleTorch,
            onSwitchCamera = onSwitchCamera,
            isTorchOn = isTorchOn
        )
    }
}

@Composable
fun ScannerOverlay(
    onNavigateBack: () -> Unit,
    onToggleTorch: () -> Unit,
    onSwitchCamera: () -> Unit,
    isTorchOn: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Scan Barcode",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                Box(modifier = Modifier.size(48.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(300.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        size = Size(size.width, size.height)
                    )

                    drawRect(
                        color = Color.Transparent,
                        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                        size = Size(
                            size.width - 8.dp.toPx(),
                            size.height - 8.dp.toPx()
                        ),
                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                    )

                    drawRect(
                        color = Color.Green,
                        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                        size = Size(
                            size.width - 8.dp.toPx(),
                            size.height - 8.dp.toPx()
                        ),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Position barcode within the green frame",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = if (isTorchOn) "Turn torch off" else "Turn torch on",
                        tint = if (isTorchOn) Color.Yellow else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = onSwitchCamera,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.Cameraswitch,
                        contentDescription = "Switch camera",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRationaleScreen(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Camera Permission Needed",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "FOODSCAN+ needs camera access to scan product barcodes. This is essential for the app to function.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permission")
        }

        TextButton(onClick = onNavigateBack) {
            Text("Go Back")
        }
    }
}

@Composable
fun PermissionDeniedScreen(
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Camera Permission Denied",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Camera permission is required to scan barcodes. Please enable it in app settings.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open Settings")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateBack) {
            Text("Go Back")
        }
    }
}