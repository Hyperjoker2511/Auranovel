package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "novels")
data class Novel(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val description: String,
    val genres: String, // Comma-separated
    val status: String, // Ongoing, Completed
    val sourceId: String, // Extension ID or "local_epub", "local_sample"
    val sourceUrl: String,
    val inLibrary: Boolean = true,
    val lastReadChapterId: String? = null,
    val lastReadChapterTitle: String? = null,
    val lastReadTimestamp: Long = 0L,
    val totalChapters: Int = 0,
    val userRating: Float = 0f,
    val isFavorite: Boolean = false,
    val totalWords: Long = 0L
)

@Entity(tableName = "chapters")
data class Chapter(
    @PrimaryKey val id: String,
    val novelId: String,
    val chapterNumber: Float,
    val title: String,
    val url: String,
    val content: String,
    val translatedContent: String? = null,
    val isRead: Boolean = false,
    val isBookmarked: Boolean = false,
    val lastReadPosition: Int = 0, // Scroll offset or page index
    val isDownloaded: Boolean = false,
    val wordCount: Int = 0,
    val releaseDate: String = ""
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val username: String = "Arcane Wanderer",
    val level: Int = 1,
    val currentXp: Long = 120L,
    val equippedTitle: String = "Novice Reader",
    val totalMinutesRead: Long = 45L,
    val totalWordsRead: Long = 18500L,
    val currentStreakDays: Int = 3,
    val lastActiveDate: String = "",
    val favoriteGenre: String = "Fantasy",
    val unlockedTitles: String = "Novice Reader,Apprentice Scroller",
    val unlockedBadges: String = "FIRST_WORDS,NIGHT_OWL,PAGE_STORM"
)

@Entity(tableName = "scraper_extensions")
data class ScraperExtension(
    @PrimaryKey val id: String,
    val name: String,
    val baseUrl: String,
    val lang: String = "en",
    val iconUrl: String = "",
    val version: String = "1.0.0",
    val searchUrlPattern: String,
    val novelListSelector: String,
    val novelTitleSelector: String,
    val novelCoverSelector: String,
    val novelUrlSelector: String,
    val chapterListSelector: String,
    val chapterTitleSelector: String,
    val chapterUrlSelector: String,
    val chapterContentSelector: String,
    val customHeadersJson: String = "{}",
    val isBuiltIn: Boolean = false,
    val isEnabled: Boolean = true,
    val isInstalled: Boolean = true,
    val repoId: String = "official"
)

@Entity(tableName = "community_repositories")
data class CommunityRepository(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val description: String,
    val sourceCount: Int = 0,
    val isDefault: Boolean = false,
    val lastSyncTime: Long = 0L
)

@Entity(tableName = "tracker_records")
data class TrackerRecord(
    @PrimaryKey val novelId: String,
    val trackerType: String, // ANILIST, MAL, KITSU, NOVEL_UPDATES
    val remoteId: String,
    val status: String, // CURRENT, COMPLETED, ON_HOLD, DROPPED, PLANNING
    val progress: Int,
    val score: Float,
    val lastSyncedTime: Long
)

data class BadgeInfo(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val requiredXpOrStat: String,
    val isUnlocked: Boolean = false
)

enum class ReaderThemeMode(val title: String) {
    LIGHT("Paper Light"),
    SEPIA("Warm Parchment"),
    DARK("Midnight Purple"),
    AMOLED("Pure Black"),
    EMERALD("Forest Jade"),
    E_INK("E-Ink Monochrome")
}

enum class ReadingPagingMode {
    CONTINUOUS_SCROLL,
    PAGED_FLIP
}

@Entity(tableName = "reading_history")
data class HistoryEntry(
    @PrimaryKey val id: String, // e.g. "${novelId}_${chapterId}"
    val novelId: String,
    val chapterId: String,
    val novelTitle: String,
    val chapterTitle: String,
    val coverUrl: String,
    val readTimestamp: Long = System.currentTimeMillis(),
    val progressPercent: Int = 0,
    val totalChapters: Int = 0
)

data class ReaderSettings(
    val fontSizeSp: Int = 18,
    val lineHeightMultiplier: Float = 1.6f,
    val paragraphSpacingDp: Int = 12,
    val horizontalPaddingDp: Int = 20,
    val themeMode: ReaderThemeMode = ReaderThemeMode.DARK,
    val pagingMode: ReadingPagingMode = ReadingPagingMode.CONTINUOUS_SCROLL,
    val fontFamily: String = "Serif", // Serif, Sans, Monospace, Cursive
    val textAlign: String = "Left", // Left, Justify
    val autoScrollSpeed: Float = 0f, // 0 = stopped, 1..10
    val showBatteryAndClock: Boolean = true,
    val showProgressOverlay: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val antiAfkEnabled: Boolean = true,
    val eInkHighContrast: Boolean = false,
    val activeTranslationTargetLang: String = "EN"
)

