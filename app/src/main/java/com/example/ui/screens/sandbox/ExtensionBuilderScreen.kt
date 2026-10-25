package com.example.ui.screens.sandbox

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ScraperExtension
import com.example.data.scraper.ScrapeTestResult
import com.example.ui.theme.ArcaneGold
import com.example.ui.theme.ArcaneGreen
import com.example.ui.theme.ArcanePrimary

enum class PresetThemeType(val title: String, val subtitle: String, val emoji: String) {
    WORDPRESS_MADARA("WordPress / Madara Fiction", "Works for 80% of novel translation sites", "🚀"),
    WUXIA_GRID("Wuxia & Cultivation Grid", "Optimized for martial arts & serialized portals", "⚡"),
    CLEAN_CHAPTER_LIST("Classic Light Novel Archive", "Simple table-based chapter list", "📖"),
    PRO_CUSTOM("Custom CSS Selectors", "Fine-tune individual HTML DOM tags", "🛠️")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionBuilderScreen(
    testResult: ScrapeTestResult?,
    isTesting: Boolean,
    onTestScraper: (
        testUrl: String,
        headersJson: String,
        itemSelector: String,
        titleSelector: String,
        coverSelector: String,
        urlSelector: String,
        chapterListSelector: String,
        chapterContentSelector: String
    ) -> Unit,
    onSaveExtension: (ScraperExtension) -> Unit,
    onImportCommunityRepo: (String, (Int) -> Unit) -> Unit
) {
    val scrollState = rememberScrollState()

    var selectedPreset by remember { mutableStateOf(PresetThemeType.WORDPRESS_MADARA) }
    var providerName by remember { mutableStateOf("Arcane Fiction Hub") }
    var baseUrl by remember { mutableStateOf("https://www.royalroad.com") }
    var testUrl by remember { mutableStateOf("https://www.royalroad.com/fictions/search?title=arcane") }

    // Selectors initialized with sensible defaults
    var itemSelector by remember { mutableStateOf(".fiction-list-item") }
    var titleSelector by remember { mutableStateOf(".fiction-title a") }
    var coverSelector by remember { mutableStateOf("img") }
    var urlSelector by remember { mutableStateOf(".fiction-title a") }
    var chapterListSelector by remember { mutableStateOf("#chapters tbody tr a") }
    var chapterContentSelector by remember { mutableStateOf(".chapter-content") }
    var customHeaders by remember { mutableStateOf("{\"User-Agent\": \"Mozilla/5.0\"}") }

    var isSaved by remember { mutableStateOf(false) }

    fun applyPreset(preset: PresetThemeType) {
        selectedPreset = preset
        when (preset) {
            PresetThemeType.WORDPRESS_MADARA -> {
                itemSelector = ".c-tabs-item__content, .page-item-detail, .fiction-list-item"
                titleSelector = ".post-title a, h3 a, .fiction-title a"
                coverSelector = "img"
                urlSelector = ".post-title a, h3 a, a"
                chapterListSelector = ".wp-manga-chapter a, #chapters tbody tr a, .chapter-list a"
                chapterContentSelector = ".reading-content, .text-left, .chapter-content"
            }
            PresetThemeType.WUXIA_GRID -> {
                itemSelector = ".novel-item, .grid-item, .li-row"
                titleSelector = ".novel-title, .tit a"
                coverSelector = "img"
                urlSelector = "a"
                chapterListSelector = ".chapter-list a, #chapterlist a"
                chapterContentSelector = ".chapter-text, #article"
            }
            PresetThemeType.CLEAN_CHAPTER_LIST -> {
                itemSelector = "article, .novel-entry"
                titleSelector = "h2 a, .entry-title a"
                coverSelector = "img.entry-thumb, img"
                urlSelector = "a"
                chapterListSelector = "ul.chapters a, table a"
                chapterContentSelector = "article .entry-content, .post-content"
            }
            PresetThemeType.PRO_CUSTOM -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = ArcaneGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Visual Extension Wizard", fontWeight = FontWeight.Bold)
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
            // Visual Hero Guide
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🪄", fontSize = 36.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Friendly Scraper Builder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Add any novel website by choosing a template below. No code needed—the wizard handles HTML parsing automatically!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // STEP 1: Basic Site Info
            Text("Step 1: Website Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = providerName,
                onValueChange = { providerName = it; isSaved = false },
                label = { Text("Provider Name (e.g., RoyalRoad, WebNovel)") },
                modifier = Modifier.fillMaxWidth().testTag("sandbox_name_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it; isSaved = false },
                label = { Text("Base Website URL") },
                modifier = Modifier.fillMaxWidth().testTag("sandbox_base_url_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = testUrl,
                onValueChange = { testUrl = it },
                label = { Text("Target Test Page URL (search or listing page)") },
                modifier = Modifier.fillMaxWidth().testTag("sandbox_test_url_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // STEP 2: Choose Template Preset
            Text("Step 2: Pick Website Template Preset", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "Most novel sites follow known CMS layouts. Select one and the selectors will automatically fill in!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PresetThemeType.entries.forEach { preset ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { applyPreset(preset) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedPreset == preset) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (selectedPreset == preset) androidx.compose.foundation.BorderStroke(2.dp, ArcaneGold) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(preset.emoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.title,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedPreset == preset) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = preset.subtitle,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = selectedPreset == preset,
                                onClick = { applyPreset(preset) }
                            )
                        }
                    }
                }
            }

            // Pro Mode: Advanced Selectors if user expands
            if (selectedPreset == PresetThemeType.PRO_CUSTOM) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Custom CSS Selectors", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = itemSelector, onValueChange = { itemSelector = it }, label = { Text("Item Selector") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(value = titleSelector, onValueChange = { titleSelector = it }, label = { Text("Title Selector") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(value = chapterListSelector, onValueChange = { chapterListSelector = it }, label = { Text("Chapter List Selector") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(value = chapterContentSelector, onValueChange = { chapterContentSelector = it }, label = { Text("Content Selector") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // STEP 3: Test Connection & Run Sandbox
            Text("Step 3: Test & Verify", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    onTestScraper(
                        testUrl, customHeaders, itemSelector, titleSelector,
                        coverSelector, urlSelector, chapterListSelector, chapterContentSelector
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("run_sandbox_test_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ArcanePrimary)
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Fetching and Parsing DOM...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = ArcaneGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Scraper in Sandbox", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Sandbox Output & Live Preview
            if (testResult != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (testResult.success) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF331414)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (testResult.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (testResult.success) ArcaneGreen else Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (testResult.success) "Verification Passed! (${testResult.executionTimeMs}ms)" else "Parsing Warning",
                                    fontWeight = FontWeight.Bold,
                                    color = if (testResult.success) ArcaneGreen else Color.Red
                                )
                                Text(
                                    text = if (testResult.success) "Found ${testResult.itemsFound} novel items and ${testResult.sampleChapterCount} chapters" else "Check test URL and selector compatibility",
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (testResult.success && testResult.sampleTitles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Discovered Titles Preview:", style = MaterialTheme.typography.labelSmall, color = ArcaneGold, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            testResult.sampleTitles.take(4).forEach { title ->
                                Text("📚 $title", fontSize = 13.sp, maxLines = 1)
                            }
                        }

                        if (testResult.sampleContentPreview.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Preview: ${testResult.sampleContentPreview}",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // STEP 4: Save & Install to My Sources
            Button(
                onClick = {
                    val ext = ScraperExtension(
                        id = "custom_${providerName.lowercase().replace(" ", "_")}_${System.currentTimeMillis() % 1000}",
                        name = providerName,
                        baseUrl = baseUrl,
                        searchUrlPattern = if (baseUrl.endsWith("/")) "${baseUrl}search?q={query}" else "$baseUrl/search?q={query}",
                        novelListSelector = itemSelector,
                        novelTitleSelector = titleSelector,
                        novelCoverSelector = coverSelector,
                        novelUrlSelector = urlSelector,
                        chapterListSelector = chapterListSelector,
                        chapterTitleSelector = "",
                        chapterUrlSelector = "a",
                        chapterContentSelector = chapterContentSelector,
                        customHeadersJson = customHeaders,
                        isBuiltIn = false,
                        isInstalled = true
                    )
                    onSaveExtension(ext)
                    isSaved = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_custom_extension_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSaved) ArcaneGreen else MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(if (isSaved) Icons.Default.CheckCircle else Icons.Default.AddCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSaved) "Plugin Installed to Your Sources!" else "Save & Install to My Sources",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
