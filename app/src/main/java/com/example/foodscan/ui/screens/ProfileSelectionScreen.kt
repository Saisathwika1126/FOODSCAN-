package com.example.foodscan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodscan.data.models.UserProfile
import com.example.foodscan.ui.viewmodel.ProfileViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun ProfileSelectionScreen(
    profileViewModel: ProfileViewModel,
    onProfileSelected: () -> Unit
) {
    val profiles by profileViewModel.allProfiles.collectAsState()
    val isLoading by profileViewModel.isLoading.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF141414), Color.Black)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isEditing) "Manage Profiles" else "Who's scanning?",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    modifier = Modifier.widthIn(max = 400.dp)
                ) {
                    items(profiles) { profile ->
                        ProfileItem(
                            profile = profile,
                            isEditing = isEditing,
                            onClick = {
                                profileViewModel.selectProfile(profile)
                                onProfileSelected()
                            }
                        )
                    }

                    if (profiles.size < 5) {
                        item {
                            AddProfileItem { showAddDialog = true }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = { isEditing = !isEditing },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Gray
                ),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
            ) {
                Text(if (isEditing) "DONE" else "MANAGE PROFILES", letterSpacing = 1.sp)
            }
        }
    }

    if (showAddDialog) {
        AddProfileDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, emoji ->
                val email = Firebase.auth.currentUser?.email ?: ""
                profileViewModel.createProfile(email, name, emoji)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ProfileItem(
    profile: UserProfile,
    isEditing: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = getAvatarColor(profile.avatarEmoji)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = profile.avatarEmoji, fontSize = 50.sp)
                }
            }
            
            if (isEditing) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                )
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = profile.displayName,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun AddProfileItem(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Profile",
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Add Profile",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun AddProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("👤") }
    val emojis = listOf("👤", "👨", "👩", "🧒", "👶", "👴", "👵", "🧔", "👱‍♂️", "👱‍♀️")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Avatar", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    emojis.take(5).forEach { emoji ->
                        EmojiSelect(emoji, selectedEmoji == emoji) { selectedEmoji = emoji }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    emojis.drop(5).forEach { emoji ->
                        EmojiSelect(emoji, selectedEmoji == emoji) { selectedEmoji = emoji }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedEmoji) },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EmojiSelect(emoji: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 24.sp)
    }
}

fun getAvatarColor(emoji: String): Color {
    return when (emoji) {
        "👨" -> Color(0xFFE50914)
        "👩" -> Color(0xFF2B59FF)
        "🧒" -> Color(0xFF00A8E8)
        "👶" -> Color(0xFFE87C03)
        "👴" -> Color(0xFFB9090B)
        "👵" -> Color(0xFF564D4D)
        else -> Color(0xFF1F1F1F)
    }
}
