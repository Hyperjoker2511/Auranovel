package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ReaderThemeMode
import com.example.ui.components.CloudflareSolverDialog
import com.example.ui.components.LevelUpCelebrationDialog
import com.example.ui.screens.browse.BrowseScreen
import com.example.ui.screens.discover.DiscoverScreen
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.library.NovelDetailScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.reader.ReaderScreen
import com.example.ui.screens.sandbox.ExtensionBuilderScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.ArcaneGold
import com.example.ui.theme.AuraNovelTheme
import com.example.ui.viewmodel.MainViewModel

enum class MainDestination(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    LIBRARY("Library", Icons.Default.AutoStories),
    DISCOVER("Discover", Icons.Default.Explore),
    HISTORY("History", Icons.Default.History),
    BROWSE("Extensions", Icons.Default.Extension),
    PROFILE("Me", Icons.Default.Person)
}


class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val readerSettings by viewModel.readerSettings.collectAsState()
            val isEInk = readerSettings.themeMode == ReaderThemeMode.E_INK

            AuraNovelTheme(
                darkTheme = readerSettings.themeMode != ReaderThemeMode.LIGHT && readerSettings.themeMode != ReaderThemeMode.SEPIA,
                isEInkMode = isEInk
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuraNovelApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AuraNovelApp(viewModel: MainViewModel) {
    var currentDestination by remember { mutableStateOf(MainDestination.LIBRARY) }
    var showSettingsSubScreen by remember { mutableStateOf(false) }
    var showSandboxSubScreen by remember { mutableStateOf(false) }

    val libraryNovels by viewModel.libraryNovels.collectAsState()
    val featuredNovels by viewModel.featuredNovels.collectAsState()
    val readingHistory by viewModel.readingHistory.collectAsState()
    val selectedNovel by viewModel.selectedNovel.collectAsState()
    val selectedNovelChapters by viewModel.selectedNovelChapters.collectAsState()
    val currentReadingChapter by viewModel.currentReadingChapter.collectAsState()
    val readerSettings by viewModel.readerSettings.collectAsState()
    val showingTranslated by viewModel.showingTranslated.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()

    val userProfile by viewModel.userProfile.collectAsState()
    val activeLevelUp by viewModel.activeLevelUpEvent.collectAsState()

    val extensions by viewModel.extensions.collectAsState()
    val installedExtensions by viewModel.installedExtensions.collectAsState()
    val activeRepositories by viewModel.activeRepositories.collectAsState()
    val translationConfig by viewModel.translationConfig.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    val sandboxResult by viewModel.sandboxTestResult.collectAsState()
    val isTestingSandbox by viewModel.isTestingSandbox.collectAsState()

    val cloudflareChallenge by viewModel.cloudflareChallenge.collectAsState()
    val trackerAccounts by viewModel.trackerAccounts.collectAsState()
    val syncToast by viewModel.syncToastMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(syncToast) {
        syncToast?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSyncToast()
        }
    }

    // Cloudflare Dialog
    if (cloudflareChallenge != null) {
        CloudflareSolverDialog(
            challenge = cloudflareChallenge!!,
            onResolved = { cookies ->
                viewModel.resolveCloudflareChallenge(cookies)
            },
            onDismiss = {
                viewModel.dismissCloudflareChallenge()
            }
        )
    }

    // Level-Up Celebration Dialog
    if (activeLevelUp != null) {
        LevelUpCelebrationDialog(
            event = activeLevelUp!!,
            onEquipTitle = { title ->
                viewModel.equipTitle(title)
            },
            onDismiss = {
                viewModel.dismissLevelUp()
            }
        )
    }

    // READER FULL-SCREEN OVERLAY
    if (currentReadingChapter != null) {
        BackHandler {
            viewModel.closeReader()
        }

        ReaderScreen(
            chapter = currentReadingChapter!!,
            settings = readerSettings,
            ttsManager = viewModel.ttsManager,
            showingTranslated = showingTranslated,
            isTranslating = isTranslating,
            onBack = { viewModel.closeReader() },
            onNextChapter = { viewModel.nextChapter() },
            onPreviousChapter = { viewModel.previousChapter() },
            onTranslateToggle = {
                if (showingTranslated) {
                    viewModel.toggleOriginalVsTranslated()
                } else {
                    viewModel.translateCurrentChapter(readerSettings.activeTranslationTargetLang)
                }
            },
            onSettingsChanged = { viewModel.updateReaderSettings(it) },
            onUserInteraction = { viewModel.recordUserInteraction() },
            onReadingProgress = { pos, completed ->
                viewModel.updateReadingPosition(pos, completed)
            }
        )
        return
    }

    // NOVEL DETAIL SUB-SCREEN (when selected and on compact screen)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp

        if (selectedNovel != null && !isWideScreen) {
            BackHandler {
                viewModel.selectNovel("")
            }

            NovelDetailScreen(
                novel = selectedNovel!!,
                chapters = selectedNovelChapters,
                onBack = { viewModel.selectNovel("") },
                onChapterClick = { ch -> viewModel.openChapter(ch.id) },
                onToggleLibrary = { viewModel.toggleLibrary(selectedNovel!!) },
                onToggleFavorite = { viewModel.toggleFavorite(selectedNovel!!) },
                onToggleBookmark = { id -> viewModel.toggleBookmark(id) },
                onExportEpub = { file, cb -> viewModel.exportCurrentNovelToEpub(file, cb) },
                onBatchDownload = { count, progress -> viewModel.batchDownloadChapters(selectedNovel!!.id, count, progress) }
            )
            return@BoxWithConstraints
        }

        // ADAPTIVE LAYOUT
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        modifier = Modifier.testTag("mobile_bottom_navigation")
                    ) {
                        MainDestination.entries.forEach { dest ->
                            NavigationBarItem(
                                selected = currentDestination == dest,
                                onClick = { currentDestination = dest },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("nav_item_${dest.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        modifier = Modifier.testTag("tablet_navigation_rail"),
                        header = {
                            Icon(
                                Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = ArcaneGold,
                                modifier = Modifier.padding(vertical = 16.dp).size(32.dp)
                            )
                        }
                    ) {
                        MainDestination.entries.forEach { dest ->
                            NavigationRailItem(
                                selected = currentDestination == dest,
                                onClick = { currentDestination = dest },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label) }
                            )
                        }
                    }
                }

                // Wide screen Master-Detail or Standard Page
                if (isWideScreen && currentDestination == MainDestination.LIBRARY && selectedNovel != null) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            LibraryScreen(
                                novels = libraryNovels,
                                onNovelClick = { novel -> viewModel.selectNovel(novel.id) },
                                onResumeReading = { chId -> viewModel.openChapter(chId) },
                                onImportEpub = { uri -> viewModel.importEpub(uri) {} },
                                onImportTxt = { uri, name -> viewModel.importTxt(uri, name) {} },
                                onOpenSearch = { currentDestination = MainDestination.BROWSE }
                            )
                        }
                        VerticalDivider()
                        Box(modifier = Modifier.weight(1.2f)) {
                            NovelDetailScreen(
                                novel = selectedNovel!!,
                                chapters = selectedNovelChapters,
                                onBack = { viewModel.selectNovel("") },
                                onChapterClick = { ch -> viewModel.openChapter(ch.id) },
                                onToggleLibrary = { viewModel.toggleLibrary(selectedNovel!!) },
                                onToggleFavorite = { viewModel.toggleFavorite(selectedNovel!!) },
                                onToggleBookmark = { id -> viewModel.toggleBookmark(id) },
                                onExportEpub = { file, cb -> viewModel.exportCurrentNovelToEpub(file, cb) },
                                onBatchDownload = { count, progress -> viewModel.batchDownloadChapters(selectedNovel!!.id, count, progress) }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (showSettingsSubScreen) {
                            BackHandler { showSettingsSubScreen = false }
                            SettingsScreen(
                                readerSettings = readerSettings,
                                translationConfig = translationConfig,
                                activeRepositories = activeRepositories,
                                onUpdateReaderSettings = { viewModel.updateReaderSettings(it) },
                                onUpdateTranslationConfig = { viewModel.updateTranslationConfig(it) },
                                onAddRepository = { name, url -> viewModel.addRepository(name, url) },
                                onDeleteRepository = { viewModel.deleteRepository(it) },
                                onExportBackup = {
                                    viewModel.epubManager.exportBackupJson(libraryNovels, selectedNovelChapters)
                                }
                            )
                        } else if (showSandboxSubScreen) {
                            BackHandler { showSandboxSubScreen = false }
                            ExtensionBuilderScreen(
                                testResult = sandboxResult,
                                isTesting = isTestingSandbox,
                                onTestScraper = { testUrl, headers, itemSel, titleSel, coverSel, urlSel, chListSel, chContentSel ->
                                    viewModel.testSandboxScraper(testUrl, headers, itemSel, titleSel, coverSel, urlSel, chListSel, chContentSel)
                                },
                                onSaveExtension = { ext -> viewModel.saveCustomScraper(ext) },
                                onImportCommunityRepo = { url, cb -> viewModel.importRepositoryManifest(url, cb) }
                            )
                        } else {
                            when (currentDestination) {
                                MainDestination.LIBRARY -> {
                                    LibraryScreen(
                                        novels = libraryNovels,
                                        onNovelClick = { novel -> viewModel.selectNovel(novel.id) },
                                        onResumeReading = { chId -> viewModel.openChapter(chId) },
                                        onImportEpub = { uri -> viewModel.importUniversalDocument(uri, null) {} },
                                        onImportTxt = { uri, name -> viewModel.importUniversalDocument(uri, name) {} },
                                        onOpenSearch = { currentDestination = MainDestination.DISCOVER }
                                    )
                                }
                                MainDestination.DISCOVER -> {
                                    DiscoverScreen(
                                        featuredNovels = featuredNovels,
                                        installedExtensions = installedExtensions,
                                        searchResults = searchResults,
                                        isSearching = isSearching,
                                        onSearch = { ext, q ->
                                            if (ext != null) {
                                                viewModel.searchRemote(ext, q)
                                            } else {
                                                val firstExt = installedExtensions.firstOrNull() ?: extensions.firstOrNull()
                                                if (firstExt != null) viewModel.searchRemote(firstExt, q)
                                            }
                                        },
                                        onNovelClick = { novel -> viewModel.selectNovel(novel.id) },
                                        onAddFavorite = { novel -> viewModel.toggleFavorite(novel) },
                                        onOpenExtensions = { currentDestination = MainDestination.BROWSE }
                                    )
                                }
                                MainDestination.HISTORY -> {
                                    HistoryScreen(
                                        historyList = readingHistory,
                                        userProfile = userProfile,
                                        onResumeReading = { novelId, chapterId ->
                                            viewModel.selectNovel(novelId)
                                            viewModel.openChapter(chapterId)
                                        },
                                        onNovelClick = { novelId -> viewModel.selectNovel(novelId) },
                                        onDeleteEntry = { id -> viewModel.deleteHistoryEntry(id) },
                                        onClearAllHistory = { viewModel.clearAllHistory() }
                                    )
                                }
                                MainDestination.BROWSE -> {
                                    BrowseScreen(
                                        installedExtensions = installedExtensions,
                                        allCatalogExtensions = extensions,
                                        activeRepositories = activeRepositories,
                                        searchResults = searchResults,
                                        isSearching = isSearching,
                                        onSearch = { ext, q -> viewModel.searchRemote(ext, q) },
                                        onSelectNovel = { ext, n ->
                                            viewModel.loadNovelFromSource(ext, n) { id ->
                                                viewModel.selectNovel(id)
                                            }
                                        },
                                        onToggleInstallExtension = { id, install ->
                                            viewModel.toggleInstallExtension(id, install)
                                        },
                                        onAddRepository = { name, url ->
                                            viewModel.addRepository(name, url)
                                        },
                                        onOpenSandbox = {
                                            showSandboxSubScreen = true
                                        }
                                    )
                                }
                                MainDestination.PROFILE -> {
                                    ProfileScreen(
                                        profile = userProfile,
                                        gamificationEngine = viewModel.gamificationEngine,
                                        trackerAccounts = trackerAccounts,
                                        onEquipTitle = { title -> viewModel.equipTitle(title) },
                                        onToggleTracker = { type, conn -> viewModel.trackerManager.toggleConnection(type, conn) },
                                        onBackupLibrary = {
                                            viewModel.epubManager.exportBackupJson(libraryNovels, selectedNovelChapters)
                                        },
                                        onOpenSettings = {
                                            showSettingsSubScreen = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                }
            }
        }
    }
}
