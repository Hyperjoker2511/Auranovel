package com.example.ui.screens.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.data.model.Novel
import com.example.ui.theme.ArcaneGold
import com.example.ui.theme.ArcanePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    novels: List<Novel>,
    onNovelClick: (Novel) -> Unit,
    onResumeReading: (String) -> Unit,
    onImportEpub: (Uri) -> Unit,
    onImportTxt: (Uri, String) -> Unit,
    onOpenSearch: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("All") }

    val epubPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportEpub(it) }
    }

    val txtPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportTxt(it, "Imported_Novel.txt") }
    }

    val universalPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportEpub(it) }
    }

    var showImportMenu by remember { mutableStateOf(false) }

    var activeCategory by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("Recent") } // Recent, Title, Chapters

    val filteredNovels = remember(novels, searchQuery, selectedGenre, activeCategory, sortBy) {
        var list = novels.filter { novel ->
            val matchesQuery = novel.title.contains(searchQuery, ignoreCase = true) ||
                    novel.author.contains(searchQuery, ignoreCase = true)
            val matchesGenre = selectedGenre == "All" || novel.genres.contains(selectedGenre, ignoreCase = true)
            val matchesCat = when (activeCategory) {
                "Reading" -> novel.lastReadChapterId != null
                "Favorites" -> novel.isFavorite
                "Completed" -> novel.status.equals("Completed", ignoreCase = true)
                "Local Files" -> novel.sourceId.startsWith("local")
                else -> true
            }
            matchesQuery && matchesGenre && matchesCat
        }
        when (sortBy) {
            "Title" -> list.sortedBy { it.title }
            "Chapters" -> list.sortedByDescending { it.totalChapters }
            else -> list.sortedByDescending { it.lastReadTimestamp }
        }
    }

    val mostRecentNovel = remember(novels) {
        novels.maxByOrNull { it.lastReadTimestamp }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = ArcanePrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AuraNovel",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier.testTag("library_search_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search Online")
                    }

                    Box {
                        IconButton(
                            onClick = { showImportMenu = true },
                            modifier = Modifier.testTag("import_files_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Import Files")
                        }

                        DropdownMenu(
                            expanded = showImportMenu,
                            onDismissRequest = { showImportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open Document (All Formats)") },
                                onClick = {
                                    showImportMenu = false
                                    universalPickerLauncher.launch(arrayOf(
                                        "application/epub+zip",
                                        "application/pdf",
                                        "application/zip",
                                        "application/x-cbz",
                                        "text/plain",
                                        "text/markdown",
                                        "text/html",
                                        "*/*"
                                    ))
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = ArcaneGold)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import .EPUB Book") },
                                onClick = {
                                    showImportMenu = false
                                    epubPickerLauncher.launch(arrayOf("application/epub+zip", "*/*"))
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Book, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import .PDF Document") },
                                onClick = {
                                    showImportMenu = false
                                    universalPickerLauncher.launch(arrayOf("application/pdf", "*/*"))
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import .CBZ / Comic Archive") },
                                onClick = {
                                    showImportMenu = false
                                    universalPickerLauncher.launch(arrayOf("application/zip", "application/x-cbz", "*/*"))
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Collections, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import .TXT / .MD / .HTML") },
                                onClick = {
                                    showImportMenu = false
                                    txtPickerLauncher.launch(arrayOf("text/plain", "text/markdown", "text/html", "*/*"))
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Description, contentDescription = null)
                                }
                            )
                        }
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
            // Mihon / LNReader Style Category Tabs
            val categories = listOf("All", "Reading", "Favorites", "Completed", "Local Files")
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(activeCategory).coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ArcaneGold
            ) {
                categories.forEach { cat ->
                    Tab(
                        selected = activeCategory == cat,
                        onClick = { activeCategory = cat },
                        text = { Text(cat, fontWeight = if (activeCategory == cat) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("library_filter_input"),
                placeholder = { Text("Filter your sanctuary...") },
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Genre Chips
            val genresList = listOf("All", "Fantasy", "Cultivation", "Steampunk", "Isekai", "Local")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genresList.forEach { genre ->
                    FilterChip(
                        selected = selectedGenre == genre,
                        onClick = { selectedGenre = genre },
                        label = { Text(genre, fontSize = 12.sp) }
                    )
                }
            }

            // Continue Reading Banner
            if (mostRecentNovel != null && mostRecentNovel.lastReadChapterId != null && searchQuery.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            onResumeReading(mostRecentNovel.lastReadChapterId)
                        }
                        .testTag("continue_reading_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp, 65.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            AsyncImage(
                                model = mostRecentNovel.coverUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CONTINUE READING",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArcaneGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = mostRecentNovel.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = mostRecentNovel.lastReadChapterTitle ?: "Chapter in progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            tint = ArcaneGold,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Novel Grid
            if (filteredNovels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No novels in your library",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Import EPUBs or explore extensions to begin reading",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 135.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredNovels, key = { it.id }) { novel ->
                        NovelGridCard(
                            novel = novel,
                            onClick = { onNovelClick(novel) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NovelGridCard(
    novel: Novel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("novel_card_${novel.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AsyncImage(
                    model = novel.coverUrl,
                    contentDescription = novel.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Gradient shadow overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 120f
                            )
                        )
                )

                // Chapter count badge
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "${novel.totalChapters} Ch",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (novel.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = Color.Red,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = novel.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = novel.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
