package com.example.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReaderSettings
import com.example.data.model.ReaderThemeMode
import com.example.data.model.ReadingPagingMode
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onSettingsChanged: (ReaderSettings) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Reader Aesthetics & Controls",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Theme selector buttons
            Text(text = "Color Palette", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ThemeOptionBadge(
                    label = "Light",
                    bg = Color(0xFFF9F7FD),
                    fg = Color.Black,
                    isSelected = settings.themeMode == ReaderThemeMode.LIGHT,
                    onClick = { onSettingsChanged(settings.copy(themeMode = ReaderThemeMode.LIGHT)) }
                )
                ThemeOptionBadge(
                    label = "Sepia",
                    bg = SepiaBackground,
                    fg = SepiaText,
                    isSelected = settings.themeMode == ReaderThemeMode.SEPIA,
                    onClick = { onSettingsChanged(settings.copy(themeMode = ReaderThemeMode.SEPIA)) }
                )
                ThemeOptionBadge(
                    label = "Midnight",
                    bg = ArcaneBackground,
                    fg = ArcaneOnSurface,
                    isSelected = settings.themeMode == ReaderThemeMode.DARK,
                    onClick = { onSettingsChanged(settings.copy(themeMode = ReaderThemeMode.DARK)) }
                )
                ThemeOptionBadge(
                    label = "AMOLED",
                    bg = Color.Black,
                    fg = Color.White,
                    isSelected = settings.themeMode == ReaderThemeMode.AMOLED,
                    onClick = { onSettingsChanged(settings.copy(themeMode = ReaderThemeMode.AMOLED)) }
                )
                ThemeOptionBadge(
                    label = "Emerald",
                    bg = EmeraldBackground,
                    fg = EmeraldText,
                    isSelected = settings.themeMode == ReaderThemeMode.EMERALD,
                    onClick = { onSettingsChanged(settings.copy(themeMode = ReaderThemeMode.EMERALD)) }
                )
                ThemeOptionBadge(
                    label = "E-Ink",
                    bg = EInkWhite,
                    fg = EInkBlack,
                    isSelected = settings.themeMode == ReaderThemeMode.E_INK,
                    onClick = { onSettingsChanged(settings.copy(themeMode = ReaderThemeMode.E_INK)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text Alignment
            Text(text = "Text Alignment", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = settings.textAlign == "Left",
                    onClick = { onSettingsChanged(settings.copy(textAlign = "Left")) },
                    label = { Text("Left Align") },
                    leadingIcon = { Icon(Icons.Default.FormatAlignLeft, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = settings.textAlign == "Justify",
                    onClick = { onSettingsChanged(settings.copy(textAlign = "Justify")) },
                    label = { Text("Justified") },
                    leadingIcon = { Icon(Icons.Default.FormatAlignJustify, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reading mode: Scroll vs Paged
            Text(text = "Reading Flow", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = settings.pagingMode == ReadingPagingMode.CONTINUOUS_SCROLL,
                    onClick = { onSettingsChanged(settings.copy(pagingMode = ReadingPagingMode.CONTINUOUS_SCROLL)) },
                    label = { Text("Continuous Scroll") },
                    leadingIcon = { Icon(Icons.Default.SwapVert, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = settings.pagingMode == ReadingPagingMode.PAGED_FLIP,
                    onClick = { onSettingsChanged(settings.copy(pagingMode = ReadingPagingMode.PAGED_FLIP)) },
                    label = { Text("Paged Mode") },
                    leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Font Size Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Font Size (${settings.fontSizeSp}sp)", style = MaterialTheme.typography.labelMedium)
                Row {
                    IconButton(
                        onClick = {
                            if (settings.fontSizeSp > 12) {
                                onSettingsChanged(settings.copy(fontSizeSp = settings.fontSizeSp - 1))
                            }
                        }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease Font")
                    }
                    IconButton(
                        onClick = {
                            if (settings.fontSizeSp < 36) {
                                onSettingsChanged(settings.copy(fontSizeSp = settings.fontSizeSp + 1))
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase Font")
                    }
                }
            }

            Slider(
                value = settings.fontSizeSp.toFloat(),
                onValueChange = { onSettingsChanged(settings.copy(fontSizeSp = it.toInt())) },
                valueRange = 12f..36f,
                steps = 23
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Font Family
            Text(text = "Typeface", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Serif", "Sans", "Monospace").forEach { family ->
                    FilterChip(
                        selected = settings.fontFamily == family,
                        onClick = { onSettingsChanged(settings.copy(fontFamily = family)) },
                        label = { Text(family) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Auto Scroll Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (settings.autoScrollSpeed == 0f) "Auto-Scroll: Disabled" else "Auto-Scroll Speed (${settings.autoScrollSpeed.toInt()}x)",
                    style = MaterialTheme.typography.labelMedium
                )
                if (settings.autoScrollSpeed > 0f) {
                    TextButton(onClick = { onSettingsChanged(settings.copy(autoScrollSpeed = 0f)) }) {
                        Text("Stop")
                    }
                }
            }
            Slider(
                value = settings.autoScrollSpeed,
                onValueChange = { onSettingsChanged(settings.copy(autoScrollSpeed = it)) },
                valueRange = 0f..8f,
                steps = 7
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ThemeOptionBadge(
    label: String,
    bg: Color,
    fg: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(bg)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) ArcanePrimary else Color.Gray.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Aa", color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp)
    }
}
