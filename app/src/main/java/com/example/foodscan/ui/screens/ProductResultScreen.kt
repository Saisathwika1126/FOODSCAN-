package com.example.foodscan.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.foodscan.data.models.PostSnackTip
import com.example.foodscan.data.models.Product
import com.example.foodscan.data.models.ServingSuggestion
import com.example.foodscan.data.models.UserProfile
import com.example.foodscan.data.models.HealthScore
import com.example.foodscan.data.models.RestrictionLevel
import com.example.foodscan.ui.viewmodel.ProfileViewModel
import com.example.foodscan.utils.NotificationHelper
import com.example.foodscan.utils.ServingCalculator

class ProductDetailsViewModel : ViewModel() {
    var selectedSection by mutableStateOf("overview")
        private set

    fun selectSection(section: String) {
        selectedSection = section
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductResultScreen(
    product: Product,
    profileViewModel: ProfileViewModel,
    onLogClick: (ServingSuggestion) -> Unit,
    onScanAgain: () -> Unit
) {
    val context = LocalContext.current
    val detailViewModel: ProductDetailsViewModel = viewModel()
    val selectedSection = detailViewModel.selectedSection

    // Observe real-time profile updates
    val userProfile by profileViewModel.profile.collectAsState()

    // Use the profile directly - it updates automatically when changed
    val effectiveUserProfile = userProfile ?: UserProfile(
        displayName = "Guest",
        age = 30,
        weight = 70.0,
        allergies = emptyList(),
        healthConditions = emptyList()
    )

    val servingSuggestion = remember(product, effectiveUserProfile) {
        ServingCalculator.calculateSafeServing(product, effectiveUserProfile)
    }

    val postTips = remember(product) {
        ServingCalculator.getPostSnackTips(product)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val bgUrl = when (servingSuggestion.restrictionLevel) {
            RestrictionLevel.SAFE -> "https://images.unsplash.com/photo-1490818387583-1baba5e6382b?q=80&w=2024&auto=format&fit=crop"
            else -> "https://images.unsplash.com/photo-1543353071-873f17a7a088?q=80&w=2070&auto=format&fit=crop"
        }

        AsyncImage(
            model = bgUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "✨ Snack Insights",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = onScanAgain) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Back", 
                                tint = Color.White
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                ProductCompactHeader(product = product)

                SectionTabs(
                    selectedSection = selectedSection,
                    onSectionSelected = { detailViewModel.selectSection(it) }
                )

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = selectedSection,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        },
                        label = "SectionTransition"
                    ) { section ->
                        when (section) {
                            "overview" -> OverviewContent(product, effectiveUserProfile, servingSuggestion)
                            "serving" -> ServingContent(servingSuggestion, product, effectiveUserProfile)
                            "ingredients" -> IngredientsContent(product, effectiveUserProfile)
                            "nutrition" -> NutritionContent(product)
                            "tips" -> TipsContent(postTips, context)
                        }
                    }
                }

                ActionButtons(onLogClick = { onLogClick(servingSuggestion) }, onScanAgain = onScanAgain)
            }
        }
    }
}

@Composable
fun ProductCompactHeader(product: Product) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(4.dp)
            ) {
                if (product.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = product.productName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Text(
                    text = product.brand,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun SectionTabs(
    selectedSection: String,
    onSectionSelected: (String) -> Unit
) {
    val tabs = listOf(
        "overview" to "📋 Info",
        "serving" to "🍽️ Portion",
        "ingredients" to "🥗 Ingredients",
        "nutrition" to "📊 Nutrition",
        "tips" to "💡 Tips"
    )

    val selectedIndex = tabs.indexOfFirst { it.first == selectedSection }.coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = Color.Transparent,
        contentColor = Color.White,
        divider = {},
        edgePadding = 16.dp,
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = MaterialTheme.colorScheme.primary,
                    height = 3.dp
                )
            }
        }
    ) {
        tabs.forEach { (section, label) ->
            Tab(
                selected = selectedSection == section,
                onClick = { onSectionSelected(section) },
                text = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedSection == section) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedSection == section) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                    )
                }
            )
        }
    }
}

@Composable
fun OverviewContent(product: Product, userProfile: UserProfile, suggestion: ServingSuggestion) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HealthRatingCard(suggestion) }

        if (product.allergens.isNotEmpty()) {
            item { AllergyAlertCard(product.allergens, userProfile.allergies) }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🚀 Quick Highlights", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    HighlightRow("Health Message", suggestion.healthMessage, suggestion.emoji)
                    HighlightRow("Recommended", suggestion.actualQuantity, "🍽️")
                    HighlightRow("Calories", "${suggestion.caloriesPerPortion} kcal", "🔥")
                }
            }
        }
    }
}

@Composable
fun HighlightRow(label: String, value: String, emoji: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, modifier = Modifier.width(24.dp))
            Text(label, color = Color.White.copy(alpha = 0.8f))
        }
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ServingContent(suggestion: ServingSuggestion, product: Product, userProfile: UserProfile) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ServingSuggestionCard(suggestion) }
        item { VisualServingCard(suggestion) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📋 About This Portion", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Based on your age (${userProfile.age} yrs) and health profile, we've calculated the ideal portion size for you.",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun VisualServingCard(suggestion: ServingSuggestion) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "👁️ Visual Portion Guide",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = suggestion.comparisonEmoji,
                            fontSize = 40.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = suggestion.visualComparison,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "That's about ${suggestion.actualQuantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏠",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = "Household tip: ${suggestion.householdItem}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun IngredientsContent(product: Product, userProfile: UserProfile) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (product.allergens.isNotEmpty()) {
            item { AllergyAlertCard(product.allergens, userProfile.allergies) }
        }
        item { IngredientsCard(product.ingredients, userProfile.allergies) }
    }
}

@Composable
fun NutritionContent(product: Product) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { NutritionCard(product) }
    }
}

@Composable
fun TipsContent(tips: List<PostSnackTip>, context: Context) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tips) { tip ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    NotificationHelper.showPostSnackTip(context, tip)
                },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tip.emoji, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tip.title, fontWeight = FontWeight.Bold)
                        Text(tip.message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButtons(onLogClick: () -> Unit, onScanAgain: () -> Unit) {
    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onLogClick,
            modifier = Modifier.weight(1f).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("✅ Log Snack", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onScanAgain,
            modifier = Modifier.weight(1f).height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = BorderStroke(1.dp, Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("🔄 Scan New")
        }
    }
}

@Composable
fun HealthRatingCard(suggestion: ServingSuggestion) {
    val color = when (suggestion.healthScore) {
        HealthScore.GOOD -> Color(0xFF69F0AE)
        HealthScore.MODERATE -> Color(0xFFFFD740)
        HealthScore.HARMFUL -> Color(0xFFFF5252)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(2.dp, color.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = color, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = when (suggestion.healthScore) {
                            HealthScore.GOOD -> "✓"
                            HealthScore.MODERATE -> "!"
                            HealthScore.HARMFUL -> "✗"
                        },
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(suggestion.healthMessage, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("Recommended Portion: ${suggestion.recommendedAmount}", color = Color.White)
            }
            Text(suggestion.emoji, fontSize = 40.sp)
        }
    }
}

private data class HealthSchemeColors(
    val background: Color,
    val border: Color,
    val text: Color,
    val progress: Color
)

@Composable
fun ServingSuggestionCard(suggestion: ServingSuggestion) {
    val hColors = when (suggestion.restrictionLevel) {
        RestrictionLevel.SAFE -> HealthSchemeColors(
            background = Color(0xFFE8F5E9),
            border = Color(0xFF4CAF50),
            text = Color(0xFF2E7D32),
            progress = Color(0xFF4CAF50)
        )
        RestrictionLevel.CAUTION -> HealthSchemeColors(
            background = Color(0xFFFFF3E0),
            border = Color(0xFFFF9800),
            text = Color(0xFFF57C00),
            progress = Color(0xFFFF9800)
        )
        RestrictionLevel.AVOID -> HealthSchemeColors(
            background = Color(0xFFFFE0B2),
            border = Color(0xFFFF5722),
            text = Color(0xFFE65100),
            progress = Color(0xFFFF5722)
        )
        RestrictionLevel.DANGER -> HealthSchemeColors(
            background = Color(0xFFFFEBEE),
            border = Color(0xFFF44336),
            text = Color(0xFFC62828),
            progress = Color(0xFFF44336)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = hColors.background),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(3.dp, hColors.border),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = hColors.border, modifier = Modifier.size(32.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = when (suggestion.restrictionLevel) {
                                    RestrictionLevel.SAFE -> "✓"
                                    RestrictionLevel.CAUTION -> "!"
                                    RestrictionLevel.AVOID -> "!!"
                                    RestrictionLevel.DANGER -> "✗"
                                },
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(suggestion.healthMessage, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = hColors.text)
                }
                Text(text = suggestion.emoji, fontSize = 36.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(suggestion.actualQuantity, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = hColors.text, fontSize = 36.sp)
                Text("(${suggestion.portionSize} portion)", color = hColors.text.copy(alpha = 0.8f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Portion size:", color = hColors.text.copy(alpha = 0.8f))
                    Text("${(suggestion.recommendedPercentage * 100).toInt()}% of adult serving", fontWeight = FontWeight.Bold, color = hColors.text)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { suggestion.recommendedPercentage.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                    color = hColors.progress,
                    trackColor = hColors.border.copy(alpha = 0.2f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔥", fontSize = 20.sp)
                        Text("${suggestion.caloriesPerPortion}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = hColors.text)
                        Text("kcal", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🍬", fontSize = 20.sp)
                        Text(String.format("%.1f", suggestion.sugarPerPortion), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (suggestion.sugarPerPortion > 10) Color.Red else hColors.text)
                        Text("g sugar", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📅", fontSize = 20.sp)
                        Text("${suggestion.maxPerDay}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = hColors.text)
                        Text("per day", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💡", fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                Text(suggestion.funFact, style = MaterialTheme.typography.bodyMedium, color = hColors.text)
            }
        }
    }
}

@Composable
fun AllergyAlertCard(productAllergens: List<String>, userAllergies: List<String>) {
    val matches = productAllergens.filter { a -> userAllergies.any { it.equals(a, true) } }
    if (matches.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.2f)),
            border = BorderStroke(2.dp, Color(0xFFFF5252)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🚨", fontSize = 32.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Allergy Warning!", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                    Text("Contains: ${matches.joinToString(", ")}", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun IngredientsCard(ingredients: List<String>, userAllergies: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🥗 Ingredients", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            if (ingredients.isEmpty()) {
                Text("No ingredients information available.", color = Color.White.copy(alpha = 0.6f))
            } else {
                ingredients.forEach { ingredient ->
                    val isAllergen = userAllergies.any { ingredient.contains(it, ignoreCase = true) }
                    Text(
                        text = if (isAllergen) "⚠️ $ingredient" else "• $ingredient",
                        color = if (isAllergen) Color(0xFFFF5252) else Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = if (isAllergen) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun NutritionCard(product: Product) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📊 Nutrition Facts (per 100g)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            NutritionItem("Energy", "${product.nutritionFacts.calories.toInt()} kcal", "🔥")
            NutritionItem("Sugar", "${product.nutritionFacts.sugar.toInt()}g", "🍬")
            NutritionItem("Fat", "${product.nutritionFacts.fat.toInt()}g", "🧈")
            NutritionItem("Protein", "${product.nutritionFacts.protein.toInt()}g", "💪")
            NutritionItem("Fiber", "${product.nutritionFacts.fiber.toInt()}g", "🌾")
            NutritionItem("Salt", "${product.nutritionFacts.salt}g", "🧂")
        }
    }
}

@Composable
fun NutritionItem(label: String, value: String, emoji: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Row {
            Text(emoji, modifier = Modifier.width(24.dp))
            Text(label, color = Color.White.copy(alpha = 0.7f))
        }
        Text(value, color = Color.White, fontWeight = FontWeight.Medium)
    }
}
