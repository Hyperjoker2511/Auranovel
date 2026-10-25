package com.example.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.HistoryEntry
import com.example.data.model.UserProfile
import com.example.ui.theme.ArcaneGold
import com.example.ui.theme.ArcaneGreen
import com.example.ui.theme.ArcanePrimary
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun HistoryScreen(
    historyList: List<HistoryEntry>,
    userProfile: UserProfile?,
    onResumeReading: (novelId: String, chapterId: String) -> Unit,
    onNovelClick: (novelId: String) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onClearAllHistory: () -> Unit
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear Reading History?") },
            text = { Text("This will remove all recorded reading timelines. Your library and book bookmarks will not be affected.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllHistory()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Group history by chronological buckets
    val now = System.currentTimeMillis()
    val groupedHistory = remember(historyList) {
        val today = mutableListOf<HistoryEntry>()
        val yesterday = mutableListOf<HistoryEntry>()
        val thisWeek = mutableListOf<HistoryEntry>()
        val older = mutableListOf<HistoryEntry>()

        val oneDayMillis = TimeUnit.DAYS.toMillis(1)
        val sevenDaysMillis = TimeUnit.DAYS.toMillis(7)

        for (entry in historyList) {
            val diff = now - entry.readTimestamp
            when {
                diff < oneDayMillis -> today.add(entry)
                diff < 2 * oneDayMillis -> yesterday.add(entry)
                diff < sevenDaysMillis -> thisWeek.add(entry)
                else -> older.add(entry)
            }
        }

        linkedMapOf(
            "Today" to today,
            "Yesterday" to yesterday,
            "Past 7 Days" to thisWeek,
            "Earlier" to older
        ).filter { it.value.isNotEmpty() }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Reading History",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${historyList.size} reading sessions recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (historyList.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Stats Banner
                if (userProfile != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HistoryStatsCard(profile = userProfile)
                }
            }
        }
    ) { paddingValues ->
        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = ArcanePrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Reading History Yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Open a novel or document to start your reading timeline",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedHistory.forEach { (sectionTitle, entries) ->
                    item {
                        Text(
                            text = sectionTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ArcanePrimary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(entries, key = { it.id }) { entry ->
                        HistoryItemCard(
                            entry = entry,
                            onResume = { onResumeReading(entry.novelId, entry.chapterId) },
                            onNovelClick = { onNovelClick(entry.novelId) },
                            onDelete = { onDeleteEntry(entry.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryStatsCard(profile: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = ArcanePrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${profile.totalMinutesRead}m", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Text("Time Read", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            VerticalDivider(modifier = Modifier.height(30.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = ArcaneGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${profile.currentStreakDays} Days", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Text("Streak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            VerticalDivider(modifier = Modifier.height(30.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = ArcaneGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lv. ${profile.level}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Text("Rank", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    entry: HistoryEntry,
    onResume: () -> Unit,
    onNovelClick: () -> Unit,
    onDelete: () -> Unit
) {
    val timeAgo = remember(entry.readTimestamp) {
        formatTimeAgo(entry.readTimestamp)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${entry.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = entry.coverUrl,
                contentDescription = entry.novelTitle,
                modifier = Modifier
                    .size(width = 60.dp, height = 85.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onNovelClick() },
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.novelTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onNovelClick() }
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = entry.chapterTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeAgo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (entry.progressPercent > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { entry.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = ArcanePrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove from history",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                FilledTonalButton(
                    onClick = onResume,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ArcanePrimary.copy(alpha = 0.2f),
                        contentColor = ArcanePrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resume", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
