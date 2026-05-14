package com.example.foodscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodscan.data.models.HealthCondition
import com.example.foodscan.ui.viewmodel.ProfileViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val profile by profileViewModel.profile.collectAsState()
    val isLoading by profileViewModel.isLoading.collectAsState()
    val isSaving by profileViewModel.isSaving.collectAsState()
    val errorMessage by profileViewModel.errorMessage.collectAsState()
    val successMessage by profileViewModel.successMessage.collectAsState()

    // Local state for form fields
    var displayName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }

    // List states
    var newAllergy by remember { mutableStateOf("") }
    var newDietaryPref by remember { mutableStateOf("") }

    val allergies = remember { mutableStateListOf<String>() }
    val dietaryPrefs = remember { mutableStateListOf<String>() }

    // Health conditions state
    var selectedConditions by remember { mutableStateOf(emptyList<HealthCondition>()) }

    // Update local state when profile loads
    LaunchedEffect(profile) {
        profile?.let { p ->
            displayName = p.displayName
            age = if (p.age > 0) p.age.toString() else ""
            weight = if (p.weight > 0) p.weight.toString() else ""
            height = if (p.height > 0) p.height.toString() else ""

            allergies.clear()
            allergies.addAll(p.allergies)

            dietaryPrefs.clear()
            dietaryPrefs.addAll(p.dietaryPreferences)

            selectedConditions = p.healthConditions
        }
    }

    // Auto-create profile if it doesn't exist
    LaunchedEffect(profile) {
        if (profile == null && !isLoading) {
            val currentUser = Firebase.auth.currentUser
            if (currentUser != null) {
                profileViewModel.createProfile(currentUser.email ?: "")
            }
        }
    }

    // Clear messages after 3 seconds
    LaunchedEffect(successMessage, errorMessage) {
        if (successMessage != null || errorMessage != null) {
            delay(3000)
            profileViewModel.clearMessages()
        }
    }

    var showLogoutDialog by remember { mutableStateOf(false) }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Yes", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👤 My Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar and Points Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = profile?.avatarEmoji ?: "👤",
                                        fontSize = 30.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Points
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Total Points",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = profile?.totalPoints?.toString() ?: "0",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Badge
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = when (profile?.badgeLevel) {
                                            "Platinum" -> "🏆"
                                            "Gold" -> "🥇"
                                            "Silver" -> "🥈"
                                            "Bronze" -> "🥉"
                                            else -> "🌱"
                                        },
                                        fontSize = 24.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Personal Information
                item {
                    Text(
                        text = "📋 Personal Information",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it.filter { char -> char.isDigit() } },
                        label = { Text("Age") },
                        leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it.filter { char -> char.isDigit() || char == '.' } },
                            label = { Text("Weight (kg)") },
                            leadingIcon = { Icon(Icons.Default.MonitorWeight, contentDescription = null) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(16.dp)
                        )

                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it.filter { char -> char.isDigit() || char == '.' } },
                            label = { Text("Height (cm)") },
                            leadingIcon = { Icon(Icons.Default.Straighten, contentDescription = null) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                // Health Conditions
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🩺 Health Conditions",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HealthCondition.entries.filter { it != HealthCondition.NONE }.forEach { condition ->
                                    FilterChip(
                                        selected = selectedConditions.contains(condition),
                                        onClick = {
                                            selectedConditions = if (selectedConditions.contains(condition)) {
                                                selectedConditions - condition
                                            } else {
                                                selectedConditions + condition
                                            }
                                        },
                                        label = {
                                            Text(
                                                when (condition) {
                                                    HealthCondition.DIABETES -> "🍬 Diabetes"
                                                    HealthCondition.HYPERTENSION -> "🧂 BP/Heart"
                                                    HealthCondition.HEART_DISEASE -> "❤️ Heart Disease"
                                                    HealthCondition.KIDNEY_ISSUE -> "🫘 Kidney Issue"
                                                    HealthCondition.OBESITY -> "⚖️ Obesity"
                                                    HealthCondition.PREGNANT -> "🤰 Pregnant"
                                                    HealthCondition.LACTATING -> "🍼 Lactating"
                                                    HealthCondition.ATHLETE -> "💪 Athlete"
                                                    else -> ""
                                                }
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Allergies Section
                item {
                    Text(
                        text = "⚠️ Allergies",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // List of allergies
                if (allergies.isEmpty()) {
                    item {
                        Text(
                            text = "No allergies added",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(allergies) { allergy ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚠️ $allergy")
                                IconButton(
                                    onClick = { allergies.remove(allergy) }
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // Add new allergy
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newAllergy,
                            onValueChange = { newAllergy = it },
                            label = { Text("New Allergy") },
                            placeholder = { Text("eg: milk, peanut") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )

                        Button(
                            onClick = {
                                if (newAllergy.isNotBlank()) {
                                    allergies.add(newAllergy)
                                    newAllergy = ""
                                }
                            },
                            enabled = newAllergy.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }

                // Dietary Preferences Section
                item {
                    Text(
                        text = "🥗 Dietary Preferences",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                // List of dietary preferences
                if (dietaryPrefs.isEmpty()) {
                    item {
                        Text(
                            text = "No dietary preferences added",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(dietaryPrefs) { pref ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(pref)
                                IconButton(
                                    onClick = { dietaryPrefs.remove(pref) }
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // Add new dietary preference
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newDietaryPref,
                            onValueChange = { newDietaryPref = it },
                            label = { Text("New Dietary Preference") },
                            placeholder = { Text("eg: veggies") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )

                        Button(
                            onClick = {
                                if (newDietaryPref.isNotBlank()) {
                                    dietaryPrefs.add(newDietaryPref)
                                    newDietaryPref = ""
                                }
                            },
                            enabled = newDietaryPref.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }

                // Messages
                if (errorMessage != null) {
                    item {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (successMessage != null) {
                    item {
                        Text(
                            text = successMessage!!,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Save Button
                item {
                    Button(
                        onClick = {
                            profileViewModel.updateProfile(
                                displayName = displayName,
                                age = age.toIntOrNull() ?: 0,
                                weight = weight.toDoubleOrNull() ?: 0.0,
                                height = height.toDoubleOrNull() ?: 0.0,
                                allergies = allergies.toList(),
                                dietaryPreferences = dietaryPrefs.toList(),
                                healthConditions = selectedConditions
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isSaving && profile != null,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Bottom padding
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
