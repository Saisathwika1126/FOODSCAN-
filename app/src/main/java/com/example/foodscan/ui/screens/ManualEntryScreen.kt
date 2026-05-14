package com.example.foodscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    onSubmitBarcode: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var barcodeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enter Barcode Manually") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Type or paste the barcode number",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = barcodeInput,
                onValueChange = {
                    barcodeInput = it.filter { char -> char.isDigit() }
                    errorMessage = null
                },
                label = { Text("Barcode (EAN/UPC)") },
                placeholder = { Text("e.g., 5449000000996") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorMessage != null,
                supportingText = errorMessage?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )

            Text(
                text = "Enter 8-13 digit barcode number",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 16.dp, top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    when {
                        barcodeInput.isEmpty() -> {
                            errorMessage = "Please enter a barcode"
                        }
                        barcodeInput.length < 8 || barcodeInput.length > 13 -> {
                            errorMessage = "Barcode must be 8-13 digits"
                        }
                        else -> {
                            onSubmitBarcode(barcodeInput)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = barcodeInput.isNotBlank()
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Look Up Product", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "📌 Tips:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "• Enter the number below the barcode\n• Include all digits (no spaces)\n• Common formats: EAN-13 (13 digits), UPC-A (12 digits)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}