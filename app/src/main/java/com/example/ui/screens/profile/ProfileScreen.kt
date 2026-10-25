package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gamification.GamificationEngine
import com.example.data.model.BadgeInfo
import com.example.data.model.UserProfile
import com.example.data.tracker.TrackerAccount
import com.example.ui.theme.ArcaneGold
import com.example.ui.theme.ArcaneGreen
import com.example.ui.theme.ArcanePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: UserProfile?,
    gamificationEngine: GamificationEngine,
    trackerAccounts: List<TrackerAccount>,
    onEquipTitle: (String) -> Unit,
    onToggleTracker: (String, Boolean) -> Unit,
    onBackupLibrary: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    val currentLevel = profile?.level ?: 1
    val currentXp = profile?.currentXp ?: 0L
    val xpForCurrent = gamificationEngine.xpRequiredForLevel(currentLevel)
    val xpForNext = gamificationEngine.xpRequiredForNextLevel(currentLevel)
    val xpRange = (xpForNext - xpForCurrent).coerceAtLeast(1)
    val xpProgress = ((currentXp - xpForCurrent).toFloat() / xpRange.toFloat()).coerceIn(0f, 1f)

    val unlockedTitles = remember(profile?.unlockedTitles) {
        profile?.unlockedTitles?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: listOf("Novice Reader")
    }

    val unlockedBadgesSet = remember(profile?.unlockedBadges) {
        profile?.unlockedBadges?.split(",")?.map { it.trim() }?.toSet() ?: emptySet()
    }

    val badges = remember(unlockedBadgesSet) {
        gamificationEngine.getAllBadges(unlockedBadgesSet)
    }

    var showTitlePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = ArcaneGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Me • Arcane Cultivation", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onBackupLibrary, modifier = Modifier.testTag("backup_library_button")) {
                        Icon(Icons.Default.Backup, contentDescription = "Backup Library")
                    }
                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("open_settings_button")) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = ArcanePrimary)
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Level & Avatar Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(listOf(ArcaneGold, ArcanePrimary, ArcaneGold))
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Lv.${profile?.level ?: 1}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = ArcaneGold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = profile?.username ?: "Arcane Wanderer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Equipped Title Chip with Edit Affordance
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clickable { showTitlePicker = true }
                            .testTag("equipped_title_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = ArcaneGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = profile?.equippedTitle ?: "Novice Reader",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Level XP Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Cultivation Progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${currentXp} / ${xpForNext} XP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ArcaneGold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { xpProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ArcaneGold,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reading Stats Grid
            Text("Sanctuary Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    icon = Icons.Default.Timer,
                    value = "${profile?.totalMinutesRead ?: 0}m",
                    label = "Time Read",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.MenuBook,
                    value = "${profile?.totalWordsRead ?: 0}",
                    label = "Words Read",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "${profile?.currentStreakDays ?: 1} Days",
                    label = "Daily Streak",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.AutoAwesome,
                    value = profile?.favoriteGenre ?: "Fantasy",
                    label = "Favorite Genre",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // External Reading Trackers
            Text("External Trackers Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    trackerAccounts.forEach { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(acc.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (acc.isConnected) "Connected as @${acc.username}" else "Disconnected",
                                    fontSize = 11.sp,
                                    color = if (acc.isConnected) ArcaneGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = acc.isConnected,
                                onCheckedChange = { onToggleTracker(acc.trackerType, it) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Achievements & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Badges & Achievements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${unlockedBadgesSet.size} / ${badges.size} Unlocked",
                    style = MaterialTheme.typography.labelSmall,
                    color = ArcaneGold,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                badges.forEach { badge ->
                    BadgeItemCard(badge = badge)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Title Selector Bottom Sheet
    if (showTitlePicker) {
        AlertDialog(
            onDismissRequest = { showTitlePicker = false },
            title = { Text("Equip Arcane Title") },
            text = {
                Column {
                    unlockedTitles.forEach { title ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onEquipTitle(title)
                                    showTitlePicker = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = profile?.equippedTitle == title,
                                onClick = {
                                    onEquipTitle(title)
                                    showTitlePicker = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(title, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTitlePicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = ArcaneGold, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BadgeItemCard(badge: BadgeInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isUnlocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(badge.iconEmoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = badge.title,
                        fontWeight = FontWeight.Bold,
                        color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                    if (badge.isUnlocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.CheckCircle, contentDescription = "Unlocked", tint = ArcaneGreen, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
