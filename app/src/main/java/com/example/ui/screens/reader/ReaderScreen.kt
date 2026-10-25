package com.example.ui.screens.reader

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Chapter
import com.example.data.model.ReaderSettings
import com.example.data.model.ReaderThemeMode
import com.example.data.model.ReadingPagingMode
import com.example.service.TtsManager
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chapter: Chapter,
    settings: ReaderSettings,
    ttsManager: TtsManager,
    showingTranslated: Boolean,
    isTranslating: Boolean,
    onBack: () -> Unit,
    onNextChapter: () -> Unit,
    onPreviousChapter: () -> Unit,
    onTranslateToggle: () -> Unit,
    onSettingsChanged: (ReaderSettings) -> Unit,
    onUserInteraction: () -> Unit,
    onReadingProgress: (position: Int, isCompleted: Boolean) -> Unit
) {
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showTtsBar by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showBars by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Determine content to display: translated or original
    val rawText = remember(chapter, showingTranslated) {
        if (showingTranslated && !chapter.translatedContent.isNullOrBlank()) {
            chapter.translatedContent
        } else {
            chapter.content
        }
    }

    val paragraphs = remember(rawText) {
        rawText.split("\n\n").map { it.trim() }.filter { it.isNotBlank() }
    }

    val ttsParagraphIndex by ttsManager.currentParagraphIndex.collectAsState()

    // Reader colors according to theme
    val (readerBg, readerFg, accentColor) = when (settings.themeMode) {
        ReaderThemeMode.LIGHT -> Triple(Color(0xFFFCFBF9), Color(0xFF1E1E1E), Color(0xFF6B46C1))
        ReaderThemeMode.SEPIA -> Triple(SepiaBackground, SepiaText, Color(0xFF8D6E63))
        ReaderThemeMode.DARK -> Triple(ArcaneBackground, ArcaneOnSurface, ArcanePrimary)
        ReaderThemeMode.AMOLED -> Triple(AmoledBackground, AmoledText, ArcanePrimary)
        ReaderThemeMode.EMERALD -> Triple(EmeraldBackground, EmeraldText, EmeraldPrimary)
        ReaderThemeMode.E_INK -> Triple(EInkWhite, EInkBlack, EInkBlack)
    }

    val readerFontFamily = when (settings.fontFamily) {
        "Sans" -> FontFamily.SansSerif
        "Monospace" -> FontFamily.Monospace
        else -> FontFamily.Serif
    }

    // Auto-scroll loop
    LaunchedEffect(settings.autoScrollSpeed, settings.pagingMode) {
        if (settings.autoScrollSpeed > 0f && settings.pagingMode == ReadingPagingMode.CONTINUOUS_SCROLL) {
            val stepDelay = (60f / settings.autoScrollSpeed).coerceIn(15f, 60f).toLong()
            while (true) {
                delay(stepDelay)
                listState.scrollBy(2f)
                onUserInteraction()
            }
        }
    }

    // Progress updates
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val total = paragraphs.size.coerceAtLeast(1)
        val index = listState.firstVisibleItemIndex
        val isFinished = index >= total - 2
        onReadingProgress(index, isFinished)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readerBg)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onUserInteraction()
                        showBars = !showBars
                    }
                )
            }
    ) {
        // CONTENT AREA: Continuous Scroll or Paged
        if (settings.pagingMode == ReadingPagingMode.CONTINUOUS_SCROLL) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = settings.horizontalPaddingDp.dp),
                contentPadding = PaddingValues(top = 80.dp, bottom = 120.dp)
            ) {
                item {
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = readerFontFamily
                        ),
                        color = readerFg,
                        modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
                    )
                }

                itemsIndexed(paragraphs) { index, paragraph ->
                    val isSpoken = showTtsBar && ttsParagraphIndex == index
                    val textAnnotated = remember(paragraph, searchQuery) {
                        buildAnnotatedString {
                            if (searchQuery.isNotBlank() && paragraph.contains(searchQuery, ignoreCase = true)) {
                                var startIndex = 0
                                while (startIndex < paragraph.length) {
                                    val matchIndex = paragraph.indexOf(searchQuery, startIndex, ignoreCase = true)
                                    if (matchIndex == -1) {
                                        append(paragraph.substring(startIndex))
                                        break
                                    }
                                    append(paragraph.substring(startIndex, matchIndex))
                                    withStyle(SpanStyle(background = ArcaneGold, color = Color.Black, fontWeight = FontWeight.Bold)) {
                                        append(paragraph.substring(matchIndex, matchIndex + searchQuery.length))
                                    }
                                    startIndex = matchIndex + searchQuery.length
                                }
                            } else {
                                append(paragraph)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = settings.paragraphSpacingDp.dp)
                            .then(
                                if (isSpoken) Modifier
                                    .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                                else Modifier
                            )
                    ) {
                        Text(
                            text = textAnnotated,
                            fontSize = settings.fontSizeSp.sp,
                            lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
                            fontFamily = readerFontFamily,
                            textAlign = if (settings.textAlign == "Justify") TextAlign.Justify else TextAlign.Start,
                            color = if (isSpoken) accentColor else readerFg
                        )
                    }
                }

                // End of Chapter Action
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "End of ${chapter.title}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = onPreviousChapter) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Previous")
                                }
                                Button(onClick = onNextChapter) {
                                    Text("Next Chapter")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.SkipNext, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // PAGED FLIP MODE
            val pages = remember(paragraphs) {
                // Group paragraphs into digestible pages (2 paragraphs per page)
                paragraphs.chunked(2)
            }
            val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

            LaunchedEffect(pagerState.currentPage) {
                val total = pages.size.coerceAtLeast(1)
                onReadingProgress(pagerState.currentPage, pagerState.currentPage >= total - 1)
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = settings.horizontalPaddingDp.dp)
            ) { pageIndex ->
                val pageParas = if (pageIndex in pages.indices) pages[pageIndex] else emptyList()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 90.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (pageIndex == 0) {
                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = readerFontFamily
                            ),
                            color = readerFg,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                    }

                    for (para in pageParas) {
                        Text(
                            text = para,
                            fontSize = settings.fontSizeSp.sp,
                            lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
                            fontFamily = readerFontFamily,
                            color = readerFg,
                            modifier = Modifier.padding(bottom = settings.paragraphSpacingDp.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Page ${pageIndex + 1} of ${pages.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = readerFg.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        // TOP BAR
        AnimatedVisibility(
            visible = showBars,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 6.dp
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = chapter.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (showingTranslated) {
                                Text(
                                    text = "AI Translated (${settings.activeTranslationTargetLang})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArcaneGold
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("reader_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Translation toggle button
                        IconButton(
                            onClick = onTranslateToggle,
                            modifier = Modifier.testTag("reader_translate_button")
                        ) {
                            if (isTranslating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "Translate",
                                    tint = if (showingTranslated) ArcaneGold else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // TTS narration trigger
                        IconButton(
                            onClick = {
                                showTtsBar = !showTtsBar
                                if (showTtsBar) {
                                    ttsManager.startReading(paragraphs, 0)
                                } else {
                                    ttsManager.stop()
                                }
                            },
                            modifier = Modifier.testTag("reader_tts_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "TTS Voice",
                                tint = if (showTtsBar) ArcaneGold else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Search inside chapter
                        IconButton(
                            onClick = { isSearchActive = !isSearchActive },
                            modifier = Modifier.testTag("reader_search_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search text")
                        }

                        // Aesthetics sheet button
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier.testTag("reader_settings_button")
                        ) {
                            Icon(Icons.Default.FormatSize, contentDescription = "Font & Display Settings")
                        }
                    }
                )
            }
        }

        // Search Bar Overlay
        if (isSearchActive && showBars) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 64.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Find words in chapter...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    IconButton(onClick = { isSearchActive = false; searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Close search")
                    }
                }
            }
        }

        // FLOATING TTS BAR
        if (showTtsBar) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                TtsControlBar(
                    ttsManager = ttsManager,
                    onClose = {
                        showTtsBar = false
                        ttsManager.stop()
                    }
                )
            }
        }

        // BOTTOM QUICK NAVIGATION
        AnimatedVisibility(
            visible = showBars && !showTtsBar,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onPreviousChapter) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = null)
                        Text("Prev Ch")
                    }

                    Text(
                        text = "Ch. ${chapter.chapterNumber.toInt()}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    TextButton(onClick = onNextChapter) {
                        Text("Next Ch")
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }

    // Settings Modal
    if (showSettingsSheet) {
        ReaderSettingsSheet(
            settings = settings,
            onSettingsChanged = onSettingsChanged,
            onDismiss = { showSettingsSheet = false }
        )
    }
}
