package com.example.data.tracker

import com.example.data.model.TrackerRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TrackerAccount(
    val trackerType: String,
    val name: String,
    val isConnected: Boolean,
    val username: String = ""
)

class TrackerManager {

    private val _accounts = MutableStateFlow(
        listOf(
            TrackerAccount("ANILIST", "AniList", true, "AuraReader"),
            TrackerAccount("MAL", "MyAnimeList", false, ""),
            TrackerAccount("KITSU", "Kitsu", false, ""),
            TrackerAccount("NOVEL_UPDATES", "NovelUpdates", true, "NovelistX")
        )
    )
    val accounts: StateFlow<List<TrackerAccount>> = _accounts.asStateFlow()

    fun toggleConnection(trackerType: String, connect: Boolean, username: String = "NovelLover") {
        _accounts.value = _accounts.value.map { acc ->
            if (acc.trackerType == trackerType) {
                acc.copy(
                    isConnected = connect,
                    username = if (connect) username else ""
                )
            } else acc
        }
    }

    suspend fun syncProgress(novelId: String, novelTitle: String, chapterNumber: Int): List<String> {
        val syncedTrackers = mutableListOf<String>()
        val currentAccounts = _accounts.value.filter { it.isConnected }

        for (acc in currentAccounts) {
            // Real background sync simulation to AniList / MAL / NovelUpdates API
            syncedTrackers.add("${acc.name}: Synced ch. $chapterNumber for '$novelTitle'")
        }
        return syncedTrackers
    }
}
