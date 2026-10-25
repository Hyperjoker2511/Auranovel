package com.example.data.gamification

import com.example.data.db.UserProfileDao
import com.example.data.model.BadgeInfo
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Calendar
import kotlin.math.sqrt

data class LevelUpEvent(
    val newLevel: Int,
    val newlyUnlockedTitle: String?,
    val xp: Long
)

class GamificationEngine(private val profileDao: UserProfileDao) {

    private val _levelUpFlow = MutableSharedFlow<LevelUpEvent>(extraBufferCapacity = 2)
    val levelUpFlow: SharedFlow<LevelUpEvent> = _levelUpFlow.asSharedFlow()

    private var lastUserActivityTimestamp = System.currentTimeMillis()
    private val AFK_TIMEOUT_MS = 60_000L // 60 seconds idle pauses XP

    fun recordUserInteraction() {
        lastUserActivityTimestamp = System.currentTimeMillis()
    }

    fun isUserActive(): Boolean {
        return (System.currentTimeMillis() - lastUserActivityTimestamp) < AFK_TIMEOUT_MS
    }

    fun calculateLevel(xp: Long): Int {
        return (sqrt(xp.toDouble() / 100.0)).toInt() + 1
    }

    fun xpRequiredForLevel(level: Int): Long {
        return ((level - 1) * (level - 1) * 100).toLong()
    }

    fun xpRequiredForNextLevel(currentLevel: Int): Long {
        return (currentLevel * currentLevel * 100).toLong()
    }

    suspend fun addActiveReadingMinute(wordsRead: Int = 250) {
        if (!isUserActive()) return

        val profile = profileDao.getProfile() ?: UserProfile()
        val xpGain = 15L
        val newXp = profile.currentXp + xpGain
        val newWords = profile.totalWordsRead + wordsRead
        val newMinutes = profile.totalMinutesRead + 1

        val oldLevel = profile.level
        val newLevel = calculateLevel(newXp)

        var unlockedTitles = profile.unlockedTitles.split(",").map { it.trim() }.toMutableList()
        var newTitleUnlocked: String? = null

        val availableTitles = getTitleForLevel(newLevel)
        if (availableTitles != null && !unlockedTitles.contains(availableTitles)) {
            unlockedTitles.add(availableTitles)
            newTitleUnlocked = availableTitles
        }

        // Night Owl badge check (midnight to 4 am)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        var unlockedBadges = profile.unlockedBadges.split(",").map { it.trim() }.toMutableList()
        if (hour in 0..4 && !unlockedBadges.contains("NIGHT_OWL")) {
            unlockedBadges.add("NIGHT_OWL")
        }
        if (newWords >= 10000 && !unlockedBadges.contains("PAGE_STORM")) {
            unlockedBadges.add("PAGE_STORM")
        }
        if (newMinutes >= 60 && !unlockedBadges.contains("MARATHON")) {
            unlockedBadges.add("MARATHON")
        }

        val updated = profile.copy(
            level = newLevel,
            currentXp = newXp,
            totalMinutesRead = newMinutes,
            totalWordsRead = newWords,
            unlockedTitles = unlockedTitles.joinToString(","),
            unlockedBadges = unlockedBadges.joinToString(",")
        )
        profileDao.insertOrUpdateProfile(updated)

        if (newLevel > oldLevel) {
            _levelUpFlow.tryEmit(LevelUpEvent(newLevel, newTitleUnlocked, newXp))
        }
    }

    suspend fun recordChapterCompletion(wordsInChapter: Int) {
        val profile = profileDao.getProfile() ?: UserProfile()
        val xpGain = 80L
        val newXp = profile.currentXp + xpGain
        val oldLevel = profile.level
        val newLevel = calculateLevel(newXp)

        var unlockedBadges = profile.unlockedBadges.split(",").map { it.trim() }.toMutableList()
        if (!unlockedBadges.contains("FIRST_WORDS")) {
            unlockedBadges.add("FIRST_WORDS")
        }

        val updated = profile.copy(
            level = newLevel,
            currentXp = newXp,
            totalWordsRead = profile.totalWordsRead + wordsInChapter,
            unlockedBadges = unlockedBadges.joinToString(",")
        )
        profileDao.insertOrUpdateProfile(updated)

        if (newLevel > oldLevel) {
            val newTitle = getTitleForLevel(newLevel)
            _levelUpFlow.tryEmit(LevelUpEvent(newLevel, newTitle, newXp))
        }
    }

    suspend fun awardBadge(badgeId: String) {
        val profile = profileDao.getProfile() ?: UserProfile()
        val unlockedBadges = profile.unlockedBadges.split(",").map { it.trim() }.toMutableList()
        if (!unlockedBadges.contains(badgeId)) {
            unlockedBadges.add(badgeId)
            profileDao.insertOrUpdateProfile(
                profile.copy(
                    unlockedBadges = unlockedBadges.joinToString(","),
                    currentXp = profile.currentXp + 50L
                )
            )
        }
    }

    fun getAllBadges(unlockedIds: Set<String>): List<BadgeInfo> {
        return listOf(
            BadgeInfo("FIRST_WORDS", "First Words", "Complete your first novel chapter", "📖", "Read 1 Ch", unlockedIds.contains("FIRST_WORDS")),
            BadgeInfo("PAGE_STORM", "Page Storm", "Surpass 10,000 total words read", "⚡", "10,000 Words", unlockedIds.contains("PAGE_STORM")),
            BadgeInfo("NIGHT_OWL", "Night Owl", "Cultivate the arcane between midnight & 4 AM", "🌙", "Midnight Read", unlockedIds.contains("NIGHT_OWL")),
            BadgeInfo("MARATHON", "Marathoner", "Read for more than 60 total minutes", "🏃", "60 Mins Active", unlockedIds.contains("MARATHON")),
            BadgeInfo("POLYGLOT", "Polyglot", "Translate any foreign chapter with AI Translator", "🌐", "Use Translator", unlockedIds.contains("POLYGLOT")),
            BadgeInfo("ARCHITECT", "Extension Architect", "Test or build a scraper in Extension Sandbox", "🛠️", "Build Scraper", unlockedIds.contains("ARCHITECT")),
            BadgeInfo("E_INK_MASTER", "E-Ink Master", "Read novels with E-Ink high-contrast display mode", "🔲", "E-Ink Theme", unlockedIds.contains("E_INK_MASTER")),
            BadgeInfo("LIBRARIAN", "Imperial Librarian", "Collect 5 or more novels into your sanctuary library", "📚", "5 Novels", unlockedIds.contains("LIBRARIAN"))
        )
    }

    private fun getTitleForLevel(level: Int): String? {
        return when {
            level >= 30 -> "Omniscient Reader"
            level >= 20 -> "Grand Sovereign"
            level >= 15 -> "Domain Master"
            level >= 10 -> "Arcane Scholar"
            level >= 8 -> "Page Stormer"
            level >= 5 -> "Night-Shift Cultivator"
            level >= 3 -> "Apprentice Scroller"
            level >= 1 -> "Novice Reader"
            else -> null
        }
    }
}
