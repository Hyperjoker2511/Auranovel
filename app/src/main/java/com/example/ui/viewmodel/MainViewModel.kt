package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.epub.EpubManager
import com.example.data.gamification.GamificationEngine
import com.example.data.gamification.LevelUpEvent
import com.example.data.model.*
import com.example.data.network.CloudflareChallengeEvent
import com.example.data.network.HybridNetworkClient
import com.example.data.repository.StarterDataSeeder
import com.example.data.scraper.CommunityRepoClient
import com.example.data.scraper.JsoupScraperEngine
import com.example.data.scraper.ScrapeTestResult
import com.example.data.tracker.TrackerAccount
import com.example.data.tracker.TrackerManager
import com.example.data.translation.TranslationConfig
import com.example.data.translation.TranslationProviderType
import com.example.data.translation.TranslationService
import com.example.service.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val novelDao = db.novelDao()
    private val chapterDao = db.chapterDao()
    private val profileDao = db.userProfileDao()
    private val extensionDao = db.extensionDao()
    private val repositoryDao = db.repositoryDao()
    private val historyDao = db.historyDao()

    val repositoryManager = com.example.data.repository.RepositoryManager(repositoryDao, extensionDao)
    val epubManager = EpubManager(application)
    val documentManager = com.example.data.document.UniversalDocumentManager(application)
    val ttsManager = TtsManager(application)
    val gamificationEngine = GamificationEngine(profileDao)
    val scraperEngine = JsoupScraperEngine()
    val repoClient = CommunityRepoClient()
    val translationService = TranslationService()
    val trackerManager = TrackerManager()

    // 1. Library State
    val libraryNovels: StateFlow<List<Novel>> = novelDao.getLibraryNovels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val featuredNovels: StateFlow<List<Novel>> = libraryNovels

    // 1b. Reading History State
    val readingHistory: StateFlow<List<HistoryEntry>> = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedNovelId = MutableStateFlow<String?>(null)
    val selectedNovelId: StateFlow<String?> = _selectedNovelId.asStateFlow()


    val selectedNovel: StateFlow<Novel?> = _selectedNovelId.flatMapLatest { id ->
        if (id == null) flowOf(null) else novelDao.observeNovelById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedNovelChapters: StateFlow<List<Chapter>> = _selectedNovelId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else chapterDao.getChaptersForNovel(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Reader State
    private val _currentChapterId = MutableStateFlow<String?>(null)
    val currentChapterId: StateFlow<String?> = _currentChapterId.asStateFlow()

    val currentReadingChapter: StateFlow<Chapter?> = _currentChapterId.flatMapLatest { id ->
        if (id == null) flowOf(null) else chapterDao.observeChapterById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _readerSettings = MutableStateFlow(ReaderSettings())
    val readerSettings: StateFlow<ReaderSettings> = _readerSettings.asStateFlow()

    private val _showingTranslated = MutableStateFlow(false)
    val showingTranslated: StateFlow<Boolean> = _showingTranslated.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _chapterSearchQuery = MutableStateFlow("")
    val chapterSearchQuery: StateFlow<String> = _chapterSearchQuery.asStateFlow()

    // 3. Gamification State
    val userProfile: StateFlow<UserProfile?> = profileDao.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _activeLevelUpEvent = MutableStateFlow<LevelUpEvent?>(null)
    val activeLevelUpEvent: StateFlow<LevelUpEvent?> = _activeLevelUpEvent.asStateFlow()

    // 4. Extensions & Sandbox State
    val extensions: StateFlow<List<ScraperExtension>> = extensionDao.getAllExtensions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val installedExtensions: StateFlow<List<ScraperExtension>> = extensions.map { list ->
        list.filter { it.isInstalled }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRepositories: StateFlow<List<CommunityRepository>> = repositoryManager.allRepositories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _translationConfig = MutableStateFlow(TranslationConfig())
    val translationConfig: StateFlow<TranslationConfig> = _translationConfig.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Novel>>(emptyList())
    val searchResults: StateFlow<List<Novel>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _sandboxTestResult = MutableStateFlow<ScrapeTestResult?>(null)
    val sandboxTestResult: StateFlow<ScrapeTestResult?> = _sandboxTestResult.asStateFlow()

    private val _isTestingSandbox = MutableStateFlow(false)
    val isTestingSandbox: StateFlow<Boolean> = _isTestingSandbox.asStateFlow()

    // 5. Cloudflare & Network Interception State
    private val _cloudflareChallenge = MutableStateFlow<CloudflareChallengeEvent?>(null)
    val cloudflareChallenge: StateFlow<CloudflareChallengeEvent?> = _cloudflareChallenge.asStateFlow()

    // 6. Trackers State
    val trackerAccounts: StateFlow<List<TrackerAccount>> = trackerManager.accounts
    private val _syncToastMessage = MutableStateFlow<String?>(null)
    val syncToastMessage: StateFlow<String?> = _syncToastMessage.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            StarterDataSeeder.seedDefaultsIfEmpty(db)
            repositoryManager.seedDefaultsIfEmpty()
        }

        // Listen for Cloudflare challenges
        viewModelScope.launch {
            HybridNetworkClient.instance.challengeFlow.collect { event ->
                _cloudflareChallenge.value = event
            }
        }

        // Listen for Gamification level up events
        viewModelScope.launch {
            gamificationEngine.levelUpFlow.collect { event ->
                _activeLevelUpEvent.value = event
            }
        }

        // Periodic ticker for active reading minutes (every 60s)
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000L)
                if (_currentChapterId.value != null && _readerSettings.value.antiAfkEnabled) {
                    gamificationEngine.addActiveReadingMinute(200)
                }
            }
        }
    }

    fun selectNovel(novelId: String) {
        _selectedNovelId.value = novelId
    }

    fun openChapter(chapterId: String) {
        _currentChapterId.value = chapterId
        _showingTranslated.value = false
        _chapterSearchQuery.value = ""
        ttsManager.stop()
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            val ch = chapterDao.getChapterById(chapterId)
            if (ch != null) {
                val novel = novelDao.getNovelById(ch.novelId)
                val now = System.currentTimeMillis()
                novelDao.updateLastRead(ch.novelId, ch.id, ch.title, now)

                if (novel != null) {
                    historyDao.recordHistory(
                        HistoryEntry(
                            id = "${novel.id}_${ch.id}",
                            novelId = novel.id,
                            chapterId = ch.id,
                            novelTitle = novel.title,
                            chapterTitle = ch.title,
                            coverUrl = novel.coverUrl,
                            readTimestamp = now,
                            progressPercent = if (ch.isRead) 100 else 10,
                            totalChapters = novel.totalChapters
                        )
                    )
                }
            }
        }
    }

    fun closeReader() {
        _currentChapterId.value = null
        ttsManager.stop()
    }

    fun recordUserInteraction() {
        gamificationEngine.recordUserInteraction()
    }

    fun updateReaderSettings(settings: ReaderSettings) {
        _readerSettings.value = settings
        if (settings.themeMode == ReaderThemeMode.E_INK) {
            viewModelScope.launch {
                gamificationEngine.awardBadge("E_INK_MASTER")
            }
        }
    }

    fun updateReadingPosition(position: Int, isCompleted: Boolean) {
        val chId = _currentChapterId.value ?: return
        recordUserInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            chapterDao.updateReadPosition(chId, position)
            val ch = chapterDao.getChapterById(chId)
            if (ch != null) {
                val novel = novelDao.getNovelById(ch.novelId)
                if (isCompleted && !ch.isRead) {
                    chapterDao.updateReadStatus(chId, true)
                    gamificationEngine.recordChapterCompletion(ch.wordCount)

                    // Sync to reading trackers
                    val syncedList = trackerManager.syncProgress(
                        novelId = ch.novelId,
                        novelTitle = novel?.title ?: "Novel",
                        chapterNumber = ch.chapterNumber.toInt()
                    )
                    if (syncedList.isNotEmpty()) {
                        _syncToastMessage.value = syncedList.first()
                    }
                }

                if (novel != null) {
                    val progress = if (isCompleted) 100 else position.coerceIn(0, 100)
                    historyDao.recordHistory(
                        HistoryEntry(
                            id = "${novel.id}_${ch.id}",
                            novelId = novel.id,
                            chapterId = ch.id,
                            novelTitle = novel.title,
                            chapterTitle = ch.title,
                            coverUrl = novel.coverUrl,
                            readTimestamp = System.currentTimeMillis(),
                            progressPercent = progress,
                            totalChapters = novel.totalChapters
                        )
                    )
                }
            }
        }
    }

    fun deleteHistoryEntry(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            historyDao.deleteHistory(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyDao.clearAllHistory()
        }
    }


    fun toggleBookmark(chapterId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ch = chapterDao.getChapterById(chapterId) ?: return@launch
            chapterDao.updateBookmarkStatus(chapterId, !ch.isBookmarked)
        }
    }

    fun nextChapter() {
        val currentCh = currentReadingChapter.value ?: return
        val allChapters = selectedNovelChapters.value
        val currentIndex = allChapters.indexOfFirst { it.id == currentCh.id }
        if (currentIndex != -1 && currentIndex + 1 < allChapters.size) {
            openChapter(allChapters[currentIndex + 1].id)
        }
    }

    fun previousChapter() {
        val currentCh = currentReadingChapter.value ?: return
        val allChapters = selectedNovelChapters.value
        val currentIndex = allChapters.indexOfFirst { it.id == currentCh.id }
        if (currentIndex > 0) {
            openChapter(allChapters[currentIndex - 1].id)
        }
    }

    fun translateCurrentChapter(targetLang: String = "EN") {
        val currentCh = currentReadingChapter.value ?: return
        if (currentCh.translatedContent != null) {
            _showingTranslated.value = true
            return
        }
        viewModelScope.launch {
            _isTranslating.value = true
            val config = TranslationConfig(
                provider = TranslationProviderType.BUILT_IN_ENHANCED,
                targetLanguage = targetLang
            )
            val result = translationService.translateText(currentCh.content, targetLang, config)
            _isTranslating.value = false
            result.onSuccess { translated ->
                chapterDao.updateTranslatedContent(currentCh.id, translated)
                _showingTranslated.value = true
                gamificationEngine.awardBadge("POLYGLOT")
            }
        }
    }

    fun toggleOriginalVsTranslated() {
        _showingTranslated.value = !_showingTranslated.value
    }

    fun setChapterSearchQuery(query: String) {
        _chapterSearchQuery.value = query
    }

    fun toggleLibrary(novel: Novel) {
        viewModelScope.launch(Dispatchers.IO) {
            novelDao.updateLibraryStatus(novel.id, !novel.inLibrary)
            if (!novel.inLibrary) {
                gamificationEngine.awardBadge("LIBRARIAN")
            }
        }
    }

    fun toggleFavorite(novel: Novel) {
        viewModelScope.launch(Dispatchers.IO) {
            novelDao.insertOrUpdateNovel(novel.copy(isFavorite = !novel.isFavorite))
        }
    }

    fun equipTitle(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            profileDao.equipTitle(title)
        }
    }

    fun dismissLevelUp() {
        _activeLevelUpEvent.value = null
    }

    fun dismissSyncToast() {
        _syncToastMessage.value = null
    }

    // Scraper & Search
    fun searchRemote(extension: ScraperExtension, query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _searchResults.value = emptyList()
            val result = scraperEngine.searchNovels(extension, query)
            _isSearching.value = false
            result.onSuccess { novels ->
                _searchResults.value = novels
            }.onFailure {
                _searchResults.value = emptyList()
            }
        }
    }

    fun loadNovelFromSource(extension: ScraperExtension, novel: Novel, onLoaded: (String) -> Unit) {
        viewModelScope.launch {
            _isSearching.value = true
            val detailsResult = scraperEngine.fetchNovelDetails(extension, novel.sourceUrl)
            _isSearching.value = false
            detailsResult.onSuccess { (fetchedNovel, chapters) ->
                val readyNovel = fetchedNovel.copy(id = novel.id, inLibrary = true)
                novelDao.insertOrUpdateNovel(readyNovel)
                chapterDao.insertChapters(chapters)
                _selectedNovelId.value = readyNovel.id
                onLoaded(readyNovel.id)
            }
        }
    }

    fun downloadChapter(extension: ScraperExtension, chapter: Chapter) {
        viewModelScope.launch(Dispatchers.IO) {
            val contentResult = scraperEngine.fetchChapterContent(extension, chapter.url)
            contentResult.onSuccess { text ->
                chapterDao.updateDownloadedContent(chapter.id, true, text)
            }
        }
    }

    fun batchDownloadChapters(novelId: String, count: Int = 10, onProgress: (Int, Int) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            val chapters = chapterDao.getChaptersListForNovel(novelId).filter { !it.isDownloaded }
            val toDownload = if (count == -1) chapters else chapters.take(count)
            val novel = novelDao.getNovelById(novelId) ?: return@launch
            val ext = extensionDao.getExtensionById(novel.sourceId) ?: extensionDao.getAllExtensions().first().firstOrNull()

            toDownload.forEachIndexed { idx, ch ->
                if (ch.content.isBlank() && ext != null) {
                    val res = scraperEngine.fetchChapterContent(ext, ch.url)
                    res.onSuccess { text ->
                        chapterDao.updateDownloadedContent(ch.id, true, text)
                    }
                } else {
                    chapterDao.updateDownloadedContent(
                        ch.id,
                        true,
                        ch.content.ifBlank { "Chapter ${ch.chapterNumber.toInt()} content cached for offline reading." }
                    )
                }
                launch(Dispatchers.Main) { onProgress(idx + 1, toDownload.size) }
            }
            _syncToastMessage.value = "Downloaded ${toDownload.size} chapters of '${novel.title}' for offline reading!"
        }
    }

    // Extension Sandbox Testing
    fun testSandboxScraper(
        testUrl: String,
        headersJson: String,
        itemSelector: String,
        titleSelector: String,
        coverSelector: String,
        urlSelector: String,
        chapterListSelector: String,
        chapterContentSelector: String
    ) {
        viewModelScope.launch {
            _isTestingSandbox.value = true
            _sandboxTestResult.value = null
            val result = scraperEngine.testSandboxScraper(
                testUrl, headersJson, itemSelector, titleSelector, coverSelector, urlSelector, chapterListSelector, chapterContentSelector
            )
            _isTestingSandbox.value = false
            _sandboxTestResult.value = result
            if (result.success) {
                gamificationEngine.awardBadge("ARCHITECT")
            }
        }
    }

    fun toggleInstallExtension(extensionId: String, isInstalled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            extensionDao.updateInstallStatus(extensionId, isInstalled)
        }
    }

    fun addRepository(name: String, url: String) {
        viewModelScope.launch {
            val res = repositoryManager.fetchAndStoreManifest(name, url)
            if (res.isSuccess) {
                _syncToastMessage.value = "Synced '${res.repositoryName}' with ${res.totalSourcesFound} extensions!"
            } else {
                _syncToastMessage.value = "Repo error: ${res.errorMessage ?: "Failed to sync"}"
            }
        }
    }

    fun deleteRepository(repoId: String) {
        viewModelScope.launch {
            repositoryManager.removeRepository(repoId)
        }
    }

    fun updateTranslationConfig(config: TranslationConfig) {
        _translationConfig.value = config
    }

    fun saveCustomScraper(extension: ScraperExtension) {
        viewModelScope.launch(Dispatchers.IO) {
            extensionDao.insertOrUpdateExtension(extension)
        }
    }

    fun deleteCustomScraper(extension: ScraperExtension) {
        viewModelScope.launch(Dispatchers.IO) {
            extensionDao.deleteExtension(extension)
        }
    }

    fun importRepositoryManifest(manifestUrl: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val res = repoClient.fetchRepository(manifestUrl)
            res.onSuccess { manifest ->
                extensionDao.insertExtensions(manifest.extensions)
                onComplete(manifest.extensions.size)
            }.onFailure {
                onComplete(0)
            }
        }
    }

    // Local EPUB / TXT Import
    // Universal Document Import (EPUB, PDF, CBZ/CBR, TXT, MD, HTML)
    fun importUniversalDocument(uri: Uri, rawFileName: String?, onSuccess: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (novel, chapters) = documentManager.parseDocument(uri, rawFileName)
                novelDao.insertOrUpdateNovel(novel)
                chapterDao.insertChapters(chapters)
                _selectedNovelId.value = novel.id
                gamificationEngine.awardBadge("LIBRARIAN")
                _syncToastMessage.value = "Imported '${novel.title}' (${chapters.size} chapters)"
                launch(Dispatchers.Main) { onSuccess(novel.title) }
            } catch (e: Exception) {
                launch(Dispatchers.Main) { onSuccess("Error importing document: ${e.message}") }
            }
        }
    }

    fun importEpub(uri: Uri, onSuccess: (String) -> Unit) {
        importUniversalDocument(uri, null, onSuccess)
    }

    fun importTxt(uri: Uri, fileName: String, onSuccess: (String) -> Unit) {
        importUniversalDocument(uri, fileName, onSuccess)
    }


    fun exportCurrentNovelToEpub(outputFile: File, onComplete: (Boolean) -> Unit) {
        val novel = selectedNovel.value ?: return onComplete(false)
        val chapters = selectedNovelChapters.value
        viewModelScope.launch(Dispatchers.IO) {
            try {
                epubManager.exportToEpub(novel, chapters, outputFile)
                launch(Dispatchers.Main) { onComplete(true) }
            } catch (_: Exception) {
                launch(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun resolveCloudflareChallenge(cookies: String?) {
        val challenge = _cloudflareChallenge.value ?: return
        HybridNetworkClient.instance.injectCookiesFromWebView(challenge.url, cookies)
        _cloudflareChallenge.value = null
    }

    fun dismissCloudflareChallenge() {
        _cloudflareChallenge.value = null
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
