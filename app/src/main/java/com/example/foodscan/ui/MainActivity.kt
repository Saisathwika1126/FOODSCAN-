package com.example.foodscan.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.foodscan.ui.navigation.Screen
import com.example.foodscan.ui.screens.*
import com.example.foodscan.ui.theme.FoodScanTheme
import com.example.foodscan.ui.viewmodel.*
import com.example.foodscan.data.models.ScanHistory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodScanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FoodScanApp(applicationContext)
                }
            }
        }
    }
}

@Composable
fun FoodScanApp(context: Context) {
    val navController = rememberNavController()

    // Initialize ViewModels
    val authViewModel: AuthViewModel = viewModel()
    val scannerViewModel: ScannerViewModel = viewModel(
        factory = ScannerViewModelFactory(context)
    )
    val profileViewModel: ProfileViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()

    // Observe states
    val authState by authViewModel.authState.collectAsState()
    val activeProfile by profileViewModel.profile.collectAsState()
    val scanHistory by historyViewModel.history.collectAsState()

    // Handle authentication navigation
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            if (activeProfile == null) {
                navController.navigate(Screen.ProfileSelection) {
                    popUpTo(Screen.Login) { inclusive = true }
                }
            }
        } else if (authState is AuthState.Unauthenticated) {
            navController.navigate(Screen.Login) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Load history when profile changes
    LaunchedEffect(activeProfile) {
        activeProfile?.let {
            historyViewModel.loadHistoryForProfile(it.profileId)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login
    ) {
        composable<Screen.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.ProfileSelection) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register)
                }
            )
        }

        composable<Screen.Register> {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.ProfileSelection) {
                        popUpTo(Screen.Register) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<Screen.ProfileSelection> {
            ProfileSelectionScreen(
                profileViewModel = profileViewModel,
                onProfileSelected = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.ProfileSelection) { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.Home> {
            HomeScreen(
                onNavigateToScanner = {
                    navController.navigate(Screen.Scanner)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile)
                }
            )
        }

        composable<Screen.Scanner> {
            ScannerScreen(
                scannerViewModel = scannerViewModel,
                onBarcodeScanned = { barcode ->
                    navController.navigate(Screen.ProductResult(barcode))
                },
                onNavigateBack = {
                    scannerViewModel.resetScan()
                    navController.popBackStack()
                },
                onManualEntry = {
                    navController.navigate(Screen.ManualEntry)
                }
            )
        }

        composable<Screen.ManualEntry> {
            ManualEntryScreen(
                onSubmitBarcode = { barcode ->
                    scannerViewModel.processManualBarcode(barcode)
                    navController.navigate(Screen.ProductResult(barcode))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<Screen.ProductResult> { backStackEntry ->
            val params = backStackEntry.toRoute<Screen.ProductResult>()
            val product by scannerViewModel.product.collectAsState()
            val isLoading by scannerViewModel.isLoading.collectAsState()
            val errorMessage by scannerViewModel.errorMessage.collectAsState()

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Fetching product data...")
                            Text(
                                text = "Barcode: ${params.barcode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                errorMessage != null -> {
                    ProductErrorScreen(
                        barcode = params.barcode,
                        errorMessage = errorMessage!!,
                        onScanAgain = {
                            scannerViewModel.resetScan()
                            navController.popBackStack()
                        }
                    )
                }
                product != null -> {
                    ProductResultScreen(
                        product = product!!,
                        profileViewModel = profileViewModel,
                        onLogClick = { suggestion ->
                            val currentProfile = activeProfile
                            if (currentProfile != null) {
                                historyViewModel.saveScan(
                                    ScanHistory(
                                        userId = currentProfile.userId,
                                        profileId = currentProfile.profileId,
                                        productBarcode = product!!.barcode,
                                        productName = product!!.productName,
                                        recommendedServingPercentage = suggestion.recommendedPercentage,
                                        pointsEarned = 10,
                                        emoji = suggestion.emoji
                                    )
                                )
                            }
                            scannerViewModel.resetScan()
                            navController.popBackStack()
                        },
                        onScanAgain = {
                            scannerViewModel.resetScan()
                            navController.popBackStack()
                        }
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading...")
                    }
                }
            }
        }

        composable<Screen.Profile> {
            ProfileScreen(
                profileViewModel = profileViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(Screen.ProfileSelection) {
                        popUpTo(Screen.Home) { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.History> {
            HistoryScreen(
                scanHistory = scanHistory,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun ProductErrorScreen(
    barcode: String,
    errorMessage: String,
    onScanAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "❌ Error",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Barcode: $barcode")
                Text(errorMessage)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tips:\n• Check your internet connection\n• Try another product\n• The product may not be in our database",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onScanAgain,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan Again")
        }
    }
}

class ScannerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScannerViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
