package com.example.ui.screens.browse

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CommunityRepository
import com.example.data.model.Novel
import com.example.data.model.ScraperExtension
import com.example.ui.theme.ArcaneGold
import com.example.ui.theme.ArcaneGreen
import com.example.ui.theme.ArcanePrimary

enum class BrowseTab {
    SOURCES,
    REPOSITORIES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    installedExtensions: List<ScraperExtension>,
    allCatalogExtensions: List<ScraperExtension>,
    activeRepositories: List<CommunityRepository>,
    searchResults: List<Novel>,
    isSearching: Boolean,
    onSearch: (ScraperExtension, String) -> Unit,
    onSelectNovel: (ScraperExtension, Novel) -> Unit,
    onToggleInstallExtension: (String, Boolean) -> Unit,
    onAddRepository: (name: String, url: String) -> Unit,
    onOpenSandbox: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(BrowseTab.SOURCES) }
    var selectedSource by remember(installedExtensions) {
        mutableStateOf(installedExtensions.firstOrNull())
    }
    var query by remember { mutableStateOf("magic") }
    var languageFilter by remember { mutableStateOf("All") }
    var repoSearchQuery by remember { mutableStateOf("") }
    var showAddRepoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Explore, contentDescription = null, tint = ArcanePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Browse & Repositories", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    if (selectedTab == BrowseTab.REPOSITORIES) {
                        IconButton(onClick = { showAddRepoDialog = true }) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Add Repository", tint = ArcaneGold)
                        }
                    }
                    IconButton(onClick = onOpenSandbox, modifier = Modifier.testTag("open_sandbox_from_browse")) {
                        Icon(Icons.Default.Build, contentDescription = "Extension Sandbox")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ArcaneGold
            ) {
                Tab(
                    selected = selectedTab == BrowseTab.SOURCES,
                    onClick = { selectedTab = BrowseTab.SOURCES },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sources (${installedExtensions.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == BrowseTab.REPOSITORIES,
                    onClick = { selectedTab = BrowseTab.REPOSITORIES },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Extensions Hub (${allCatalogExtensions.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            if (selectedTab == BrowseTab.SOURCES) {
                // SOURCES TAB
                Column(modifier = Modifier.fillMaxSize()) {
                    // Source Horizontal Picker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        installedExtensions.forEach { ext ->
                            FilterChip(
                                selected = selectedSource?.id == ext.id,
                                onClick = {
                                    selectedSource = ext
                                    onSearch(ext, query)
                                },
                                label = { Text(ext.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingIcon = {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selectedSource?.id == ext.id) ArcaneGold else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                }
                            )
                        }
                    }

                    // Search Field
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search online novels in ${selectedSource?.name ?: "source"}...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(
                                    onClick = { selectedSource?.let { onSearch(it, query) } },
                                    modifier = Modifier.testTag("browse_search_action_button")
                                ) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("browse_query_input")
                    )

                    // Results List
                    if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isSearching) "Querying live online catalog..." else "Explore novels from ${selectedSource?.name ?: "source"}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Type any title or keyword and tap search",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(searchResults, key = { it.id }) { novel ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedSource?.let { onSelectNovel(it, novel) } }
                                        .testTag("search_result_${novel.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(65.dp, 90.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            AsyncImage(
                                                model = novel.coverUrl,
                                                contentDescription = novel.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = novel.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = novel.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        FilledTonalIconButton(
                                            onClick = { selectedSource?.let { onSelectNovel(it, novel) } }
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add to Library", tint = ArcaneGold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // EXTENSION REPOSITORIES TAB (LNReader / Mihon Style)
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header info banner
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Community Plugin Ecosystem", style = MaterialTheme.typography.labelSmall, color = ArcaneGold, fontWeight = FontWeight.Bold)
                                Text("${activeRepositories.size} Repositories • ${allCatalogExtensions.size} Extensions", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = { showAddRepoDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Repo")
                            }
                        }
                    }

                    // Language Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "en" to "🇬🇧 English", "ja" to "🇯🇵 Japanese", "zh" to "🇨🇳 Chinese").forEach { item ->
                            val key = if (item is Pair<*, *>) item.first as String else item as String
                            val label = if (item is Pair<*, *>) item.second as String else item as String

                            FilterChip(
                                selected = languageFilter == key,
                                onClick = { languageFilter = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }

                    val filteredExtensions = remember(allCatalogExtensions, languageFilter, repoSearchQuery) {
                        allCatalogExtensions.filter { ext ->
                            val matchLang = languageFilter == "All" || ext.lang.equals(languageFilter, ignoreCase = true)
                            val matchQuery = repoSearchQuery.isBlank() || ext.name.contains(repoSearchQuery, ignoreCase = true)
                            matchLang && matchQuery
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredExtensions, key = { it.id }) { ext ->
                            ExtensionCatalogCard(
                                extension = ext,
                                onToggleInstall = { install ->
                                    onToggleInstallExtension(ext.id, install)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Repo Dialog
    if (showAddRepoDialog) {
        var name by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddRepoDialog = false },
            title = { Text("Add Extension Repository") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add an external GitHub manifest or NovelSourcery repository endpoint:", fontSize = 12.sp)
                    
                    // Quick Preset Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SuggestionChip(
                            onClick = {
                                name = "NovelSourcery Extensions"
                                url = "https://github.com/NovelSourcery/extensions/raw/repo/index.pb"
                            },
                            label = { Text("NovelSourcery (.pb)", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                name = "LNReader Community"
                                url = "https://raw.githubusercontent.com/auranovel/index/main/manifest.json"
                            },
                            label = { Text("LNReader (.json)", fontSize = 11.sp) }
                        )
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Repository Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Repository URL (.pb, .json)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && url.isNotBlank()) {
                            onAddRepository(name, url)
                            showAddRepoDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRepoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExtensionCatalogCard(
    extension: ScraperExtension,
    onToggleInstall: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with Language Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (extension.lang) {
                        "ja" -> "🇯🇵"
                        "zh" -> "🇨🇳"
                        "ko" -> "🇰🇷"
                        else -> "🇬🇧"
                    },
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = extension.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "v${extension.version}",
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = extension.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (extension.isInstalled) {
                OutlinedButton(
                    onClick = { onToggleInstall(false) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ArcaneGreen)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Installed", fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = { onToggleInstall(true) }
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Install", fontSize = 12.sp)
                }
            }
        }
    }
}
