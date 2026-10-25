package com.example.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommunityRepository
import com.example.data.model.ReaderSettings
import com.example.data.model.ReaderThemeMode
import com.example.data.model.ReadingPagingMode
import com.example.data.translation.TranslationConfig
import com.example.data.translation.TranslationProviderType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    readerSettings: ReaderSettings,
    translationConfig: TranslationConfig,
    activeRepositories: List<CommunityRepository>,
    onUpdateReaderSettings: (ReaderSettings) -> Unit,
    onUpdateTranslationConfig: (TranslationConfig) -> Unit,
    onAddRepository: (name: String, url: String) -> Unit,
    onDeleteRepository: (String) -> Unit,
    onExportBackup: () -> Unit
) {
    val scrollState = rememberScrollState()

    var showAddRepoDialog by remember { mutableStateOf(false) }
    var showAiConfigDialog by remember { mutableStateOf(false) }
    var showGithubExportDialog by remember { mutableStateOf(false) }
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = ArcaneGold)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Settings & Customization", fontWeight = FontWeight.Bold)
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
            // Section 1: Appearance & Themes
            SettingsSectionHeader(title = "Appearance & Themes", icon = Icons.Default.Palette)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("App & Reader Color Palette", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ThemePill(
                            name = "Midnight",
                            bg = ArcaneBackground,
                            fg = ArcaneOnSurface,
                            isSelected = readerSettings.themeMode == ReaderThemeMode.DARK,
                            onClick = { onUpdateReaderSettings(readerSettings.copy(themeMode = ReaderThemeMode.DARK)) }
                        )
                        ThemePill(
                            name = "AMOLED",
                            bg = Color.Black,
                            fg = Color.White,
                            isSelected = readerSettings.themeMode == ReaderThemeMode.AMOLED,
                            onClick = { onUpdateReaderSettings(readerSettings.copy(themeMode = ReaderThemeMode.AMOLED)) }
                        )
                        ThemePill(
                            name = "Sepia",
                            bg = SepiaBackground,
                            fg = SepiaText,
                            isSelected = readerSettings.themeMode == ReaderThemeMode.SEPIA,
                            onClick = { onUpdateReaderSettings(readerSettings.copy(themeMode = ReaderThemeMode.SEPIA)) }
                        )
                        ThemePill(
                            name = "Emerald",
                            bg = EmeraldBackground,
                            fg = EmeraldText,
                            isSelected = readerSettings.themeMode == ReaderThemeMode.EMERALD,
                            onClick = { onUpdateReaderSettings(readerSettings.copy(themeMode = ReaderThemeMode.EMERALD)) }
                        )
                        ThemePill(
                            name = "E-Ink",
                            bg = EInkWhite,
                            fg = EInkBlack,
                            isSelected = readerSettings.themeMode == ReaderThemeMode.E_INK,
                            onClick = { onUpdateReaderSettings(readerSettings.copy(themeMode = ReaderThemeMode.E_INK)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingsToggleRow(
                        title = "E-Ink High Contrast Mode",
                        subtitle = "Zero animations, pure monochrome high contrast for e-readers",
                        checked = readerSettings.themeMode == ReaderThemeMode.E_INK,
                        onCheckedChange = {
                            val newMode = if (it) ReaderThemeMode.E_INK else ReaderThemeMode.DARK
                            onUpdateReaderSettings(readerSettings.copy(themeMode = newMode))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Reader Engine Customization
            SettingsSectionHeader(title = "Reader Engine & Typography", icon = Icons.Default.AutoStories)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Reading Flow Mode", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = readerSettings.pagingMode == ReadingPagingMode.CONTINUOUS_SCROLL,
                            onClick = { onUpdateReaderSettings(readerSettings.copy(pagingMode = ReadingPagingMode.CONTINUOUS_SCROLL)) },
                            label = { Text("Continuous Scroll") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = readerSettings.pagingMode == ReadingPagingMode.PAGED_FLIP,
                            onClick = { onUpdateReaderSettings(readerSettings.copy(pagingMode = ReadingPagingMode.PAGED_FLIP)) },
                            label = { Text("Paged Flip") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Text Alignment", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = readerSettings.textAlign == "Left",
                            onClick = { onUpdateReaderSettings(readerSettings.copy(textAlign = "Left")) },
                            label = { Text("Left Align") },
                            leadingIcon = { Icon(Icons.Default.FormatAlignLeft, contentDescription = null) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = readerSettings.textAlign == "Justify",
                            onClick = { onUpdateReaderSettings(readerSettings.copy(textAlign = "Justify")) },
                            label = { Text("Justified") },
                            leadingIcon = { Icon(Icons.Default.FormatAlignJustify, contentDescription = null) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Typeface Family", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Serif", "Sans", "Monospace").forEach { family ->
                            FilterChip(
                                selected = readerSettings.fontFamily == family,
                                onClick = { onUpdateReaderSettings(readerSettings.copy(fontFamily = family)) },
                                label = { Text(family) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Font Size", style = MaterialTheme.typography.bodyMedium)
                        Text("${readerSettings.fontSizeSp} sp", fontWeight = FontWeight.Bold, color = ArcaneGold)
                    }
                    Slider(
                        value = readerSettings.fontSizeSp.toFloat(),
                        onValueChange = { onUpdateReaderSettings(readerSettings.copy(fontSizeSp = it.toInt())) },
                        valueRange = 12f..36f,
                        steps = 23
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Line Height Spacing", style = MaterialTheme.typography.bodyMedium)
                        Text("${"%.1f".format(readerSettings.lineHeightMultiplier)}x", fontWeight = FontWeight.Bold, color = ArcaneGold)
                    }
                    Slider(
                        value = readerSettings.lineHeightMultiplier,
                        onValueChange = { onUpdateReaderSettings(readerSettings.copy(lineHeightMultiplier = it)) },
                        valueRange = 1.2f..2.4f,
                        steps = 11
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsToggleRow(
                        title = "Show Battery & Clock Overlay",
                        subtitle = "Display status bar info cleanly inside the reader",
                        checked = readerSettings.showBatteryAndClock,
                        onCheckedChange = { onUpdateReaderSettings(readerSettings.copy(showBatteryAndClock = it)) }
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsToggleRow(
                        title = "Keep Screen Awake",
                        subtitle = "Prevent screen from sleeping while actively reading",
                        checked = readerSettings.keepScreenAwake,
                        onCheckedChange = { onUpdateReaderSettings(readerSettings.copy(keepScreenAwake = it)) }
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsToggleRow(
                        title = "Anti-AFK Cultivation Timer",
                        subtitle = "Automatically pauses reading XP points after 60s of inactivity",
                        checked = readerSettings.antiAfkEnabled,
                        onCheckedChange = { onUpdateReaderSettings(readerSettings.copy(antiAfkEnabled = it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: AI Translation Suite
            SettingsSectionHeader(title = "AI Translation & Connectors", icon = Icons.Default.Translate)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Active Provider", fontWeight = FontWeight.Bold)
                            Text(translationConfig.provider.displayName, fontSize = 12.sp, color = ArcaneGold)
                        }
                        Button(onClick = { showAiConfigDialog = true }) {
                            Text("Configure")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Default Target Language", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("EN", "ES", "FR", "DE", "JA", "ZH").forEach { lang ->
                            FilterChip(
                                selected = translationConfig.targetLanguage.equals(lang, ignoreCase = true),
                                onClick = { onUpdateTranslationConfig(translationConfig.copy(targetLanguage = lang)) },
                                label = { Text(lang) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 4: Extension Repositories (LNReader / Mihon Style)
            SettingsSectionHeader(title = "Extension Repositories", icon = Icons.Default.Hub)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${activeRepositories.size} Repositories Registered", fontWeight = FontWeight.Bold)
                        FilledTonalButton(onClick = { showAddRepoDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Repo")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    activeRepositories.forEach { repo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(repo.name, fontWeight = FontWeight.SemiBold)
                                Text(repo.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            if (!repo.isDefault) {
                                IconButton(onClick = { onDeleteRepository(repo.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f))
                                }
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("Default", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 5: Data Backup, GitHub Publishing & About
            SettingsSectionHeader(title = "Backup & GitHub Publishing", icon = Icons.Default.CloudSync)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            onExportBackup()
                            backupStatusMessage = "Full library snapshot backup created successfully!"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Full Library Backup (JSON)")
                    }

                    if (backupStatusMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(backupStatusMessage!!, color = ArcaneGreen, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showGithubExportDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("publish_github_settings_button")
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Publish to GitHub / Export ZIP")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "AuraNovel v1.0.0 (Native Kotlin & Jetpack Compose)\nClean Architecture • Zero Tracking • 100% Client-Side",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Add Repository Dialog
    if (showAddRepoDialog) {
        var repoName by remember { mutableStateOf("") }
        var repoUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddRepoDialog = false },
            title = { Text("Add Extension Repository") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Paste a repository JSON manifest URL or choose a community preset below:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = repoName,
                        onValueChange = { repoName = it },
                        label = { Text("Repository Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = repoUrl,
                        onValueChange = { repoUrl = it },
                        label = { Text("Manifest JSON Endpoint") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Quick Community Presets:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    FilledTonalButton(
                        onClick = {
                            repoName = "Keiyoushi Light Novels Hub"
                            repoUrl = "https://raw.githubusercontent.com/auranovel/index/main/keiyoushi.json"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Keiyoushi Community Hub")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (repoName.isNotBlank() && repoUrl.isNotBlank()) {
                            onAddRepository(repoName, repoUrl)
                            showAddRepoDialog = false
                        }
                    }
                ) {
                    Text("Add Repository")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRepoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // AI Translation Config Dialog
    if (showAiConfigDialog) {
        var selectedProvider by remember { mutableStateOf(translationConfig.provider) }
        var apiKey by remember { mutableStateOf(translationConfig.apiKey) }
        var endpoint by remember { mutableStateOf(translationConfig.customEndpointUrl) }
        var model by remember { mutableStateOf(translationConfig.modelName) }

        AlertDialog(
            onDismissRequest = { showAiConfigDialog = false },
            title = { Text("AI Translation Connectors") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Translation Engine:", style = MaterialTheme.typography.labelSmall)
                    TranslationProviderType.entries.forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedProvider = type }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = selectedProvider == type, onClick = { selectedProvider = type })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(type.displayName, fontSize = 13.sp)
                        }
                    }

                    if (selectedProvider == TranslationProviderType.OPENAI_COMPATIBLE || selectedProvider == TranslationProviderType.DEEPL) {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API Key") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (selectedProvider == TranslationProviderType.OPENAI_COMPATIBLE) {
                        OutlinedTextField(
                            value = endpoint,
                            onValueChange = { endpoint = it },
                            label = { Text("Endpoint URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Model Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateTranslationConfig(
                            translationConfig.copy(
                                provider = selectedProvider,
                                apiKey = apiKey,
                                customEndpointUrl = endpoint,
                                modelName = model
                            )
                        )
                        showAiConfigDialog = false
                    }
                ) {
                    Text("Save Config")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // GitHub Publishing Modal
    if (showGithubExportDialog) {
        AlertDialog(
            onDismissRequest = { showGithubExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = ArcaneGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publish to GitHub")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "The entire AuraNovel project is formatted, optimized, and ready for version control and GitHub release.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("How to publish in Google AI Studio:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("1. Open the top-right Settings/Menu in Google AI Studio", fontSize = 12.sp)
                            Text("2. Click 'Export to GitHub' or 'Download ZIP'", fontSize = 12.sp)
                            Text("3. Link your GitHub account and choose your repository", fontSize = 12.sp)
                            Text("4. All commits, Gradle configurations, and Android native assets will be synchronized immediately!", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showGithubExportDialog = false }) {
                    Text("Got it!")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = ArcanePrimary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ThemePill(
    name: String,
    bg: Color,
    fg: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        tonalElevation = if (isSelected) 8.dp else 2.dp,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, ArcaneGold) else null,
        modifier = Modifier.size(width = 62.dp, height = 44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(name, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
