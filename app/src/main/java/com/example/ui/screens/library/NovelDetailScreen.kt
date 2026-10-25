package com.example.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Chapter
import com.example.data.model.Novel
import com.example.ui.theme.ArcaneGold
import com.example.ui.theme.ArcaneGreen
import com.example.ui.theme.ArcanePrimary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelDetailScreen(
    novel: Novel,
    chapters: List<Chapter>,
    onBack: () -> Unit,
    onChapterClick: (Chapter) -> Unit,
    onToggleLibrary: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleBookmark: (String) -> Unit,
    onExportEpub: (File, (Boolean) -> Unit) -> Unit,
    onBatchDownload: (count: Int, onProgress: (Int, Int) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var chapterFilter by remember { mutableStateOf("") }
    var isExporting by remember { mutableStateOf(false) }
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }

    var showDownloadMenu by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0 to 0) }

    val filteredChapters = remember(chapters, chapterFilter) {
        if (chapterFilter.isBlank()) chapters
        else chapters.filter { it.title.contains(chapterFilter, ignoreCase = true) }
    }

    val firstUnreadChapter = remember(chapters) {
        chapters.firstOrNull { !it.isRead } ?: chapters.firstOrNull()
    }

    val downloadedCount = remember(chapters) {
        chapters.count { it.isDownloaded }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(novel.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("novel_detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.testTag("novel_detail_favorite")) {
                        Icon(
                            imageVector = if (novel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (novel.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Batch Download Dropdown (QuickNovel style)
                    Box {
                        IconButton(onClick = { showDownloadMenu = true }, modifier = Modifier.testTag("batch_download_button")) {
                            Icon(Icons.Default.DownloadForOffline, contentDescription = "Batch Download", tint = ArcaneGold)
                        }

                        DropdownMenu(
                            expanded = showDownloadMenu,
                            onDismissRequest = { showDownloadMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Download Next 5 Chapters") },
                                onClick = {
                                    showDownloadMenu = false
                                    isDownloading = true
                                    onBatchDownload(5) { cur, total ->
                                        downloadProgress = cur to total
                                        if (cur >= total) isDownloading = false
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Download Next 10 Chapters") },
                                onClick = {
                                    showDownloadMenu = false
                                    isDownloading = true
                                    onBatchDownload(10) { cur, total ->
                                        downloadProgress = cur to total
                                        if (cur >= total) isDownloading = false
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Download All Chapters (${chapters.size})") },
                                onClick = {
                                    showDownloadMenu = false
                                    isDownloading = true
                                    onBatchDownload(-1) { cur, total ->
                                        downloadProgress = cur to total
                                        if (cur >= total) isDownloading = false
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null) }
                            )
                        }
                    }

                    // QuickNovel / Mihon EPUB Compiler Export
                    IconButton(
                        onClick = {
                            isExporting = true
                            val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
                            val file = File(outputDir, "${novel.title.replace(" ", "_")}.epub")
                            onExportEpub(file) { success ->
                                isExporting = false
                                exportSuccessMessage = if (success) "Exported as standalone .EPUB book to ${file.name}" else "Export failed"
                            }
                        },
                        modifier = Modifier.testTag("export_epub_button")
                    ) {
                        Icon(Icons.Default.LibraryBooks, contentDescription = "Export Standalone EPUB")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header Info Card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp, 160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = novel.coverUrl,
                            contentDescription = novel.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = novel.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "By ${novel.author}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = ArcaneGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${novel.userRating}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = novel.status,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${chapters.size} Chapters • $downloadedCount Offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Download Progress Banner (QuickNovel style)
            if (isDownloading) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Downloading chapters for offline reading...", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text("${downloadProgress.first} / ${downloadProgress.second}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ArcaneGold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val frac = if (downloadProgress.second > 0) downloadProgress.first.toFloat() / downloadProgress.second.toFloat() else 0f
                            LinearProgressIndicator(
                                progress = { frac },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = ArcaneGold
                            )
                        }
                    }
                }
            }

            // Action Buttons Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            firstUnreadChapter?.let { onChapterClick(it) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("read_novel_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (firstUnreadChapter?.chapterNumber == 1f) "Start Reading" else "Resume")
                    }

                    OutlinedButton(
                        onClick = onToggleLibrary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_library_button")
                    ) {
                        Icon(
                            imageVector = if (novel.inLibrary) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (novel.inLibrary) "In Library" else "Add to Library")
                    }
                }
            }

            // Export notification
            if (exportSuccessMessage != null) {
                item {
                    Text(
                        text = exportSuccessMessage!!,
                        color = ArcaneGold,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Synopsis Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Synopsis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = novel.description,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Genres: ${novel.genres}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Chapters Header & Search
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chapters (${chapters.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$downloadedCount downloaded",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = chapterFilter,
                    onValueChange = { chapterFilter = it },
                    placeholder = { Text("Filter chapters...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Chapter items with reading time estimation
            items(filteredChapters, key = { it.id }) { chapter ->
                val readingTimeMins = (chapter.wordCount / 200).coerceAtLeast(1)
                ListItem(
                    headlineContent = {
                        Text(
                            text = chapter.title,
                            fontWeight = if (chapter.isRead) FontWeight.Normal else FontWeight.SemiBold,
                            color = if (chapter.isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    supportingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "~$readingTimeMins min read • ${chapter.wordCount} words", fontSize = 11.sp)
                            if (chapter.isDownloaded) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.CheckCircle, contentDescription = "Offline Available", tint = ArcaneGreen, modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    leadingContent = {
                        Icon(
                            imageVector = if (chapter.isRead) Icons.Default.CheckCircle else Icons.Default.Circle,
                            contentDescription = if (chapter.isRead) "Read" else "Unread",
                            tint = if (chapter.isRead) ArcanePrimary else MaterialTheme.colorScheme.outline
                        )
                    },
                    trailingContent = {
                        IconButton(
                            onClick = { onToggleBookmark(chapter.id) }
                        ) {
                            Icon(
                                imageVector = if (chapter.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (chapter.isBookmarked) ArcaneGold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier
                        .clickable { onChapterClick(chapter) }
                        .testTag("chapter_item_${chapter.id}")
                )
                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }
    }
}
