package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NovelDao {
    @Query("SELECT * FROM novels WHERE inLibrary = 1 ORDER BY lastReadTimestamp DESC, title ASC")
    fun getLibraryNovels(): Flow<List<Novel>>

    @Query("SELECT * FROM novels WHERE id = :id LIMIT 1")
    suspend fun getNovelById(id: String): Novel?

    @Query("SELECT * FROM novels WHERE id = :id LIMIT 1")
    fun observeNovelById(id: String): Flow<Novel?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNovel(novel: Novel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNovels(novels: List<Novel>)

    @Query("UPDATE novels SET inLibrary = :inLibrary WHERE id = :id")
    suspend fun updateLibraryStatus(id: String, inLibrary: Boolean)

    @Query("UPDATE novels SET lastReadChapterId = :chapterId, lastReadChapterTitle = :title, lastReadTimestamp = :timestamp WHERE id = :novelId")
    suspend fun updateLastRead(novelId: String, chapterId: String, title: String, timestamp: Long)

    @Query("DELETE FROM novels WHERE id = :id")
    suspend fun deleteNovel(id: String)
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY chapterNumber ASC")
    fun getChaptersForNovel(novelId: String): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY chapterNumber ASC")
    suspend fun getChaptersListForNovel(novelId: String): List<Chapter>

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    suspend fun getChapterById(id: String): Chapter?

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    fun observeChapterById(id: String): Flow<Chapter?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<Chapter>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChapter(chapter: Chapter)

    @Query("UPDATE chapters SET isRead = :isRead WHERE id = :id")
    suspend fun updateReadStatus(id: String, isRead: Boolean)

    @Query("UPDATE chapters SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmarkStatus(id: String, isBookmarked: Boolean)

    @Query("UPDATE chapters SET lastReadPosition = :position WHERE id = :id")
    suspend fun updateReadPosition(id: String, position: Int)

    @Query("UPDATE chapters SET translatedContent = :translated WHERE id = :id")
    suspend fun updateTranslatedContent(id: String, translated: String)

    @Query("UPDATE chapters SET isDownloaded = :isDownloaded, content = :content WHERE id = :id")
    suspend fun updateDownloadedContent(id: String, isDownloaded: Boolean, content: String)

    @Query("SELECT COUNT(*) FROM chapters WHERE novelId = :novelId AND isRead = 1")
    suspend fun getReadCount(novelId: String): Int

    @Query("SELECT COUNT(*) FROM chapters WHERE isRead = 1")
    suspend fun getTotalCompletedChaptersCount(): Int
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)

    @Query("UPDATE user_profile SET equippedTitle = :title WHERE id = 1")
    suspend fun equipTitle(title: String)
}

@Dao
interface ExtensionDao {
    @Query("SELECT * FROM scraper_extensions WHERE isInstalled = 1")
    fun getInstalledExtensions(): Flow<List<ScraperExtension>>

    @Query("SELECT * FROM scraper_extensions")
    fun getAllExtensions(): Flow<List<ScraperExtension>>

    @Query("SELECT * FROM scraper_extensions WHERE id = :id LIMIT 1")
    suspend fun getExtensionById(id: String): ScraperExtension?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateExtension(extension: ScraperExtension)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtensions(extensions: List<ScraperExtension>)

    @Query("UPDATE scraper_extensions SET isInstalled = :isInstalled WHERE id = :id")
    suspend fun updateInstallStatus(id: String, isInstalled: Boolean)

    @Delete
    suspend fun deleteExtension(extension: ScraperExtension)
}

@Dao
interface TrackerDao {
    @Query("SELECT * FROM tracker_records WHERE novelId = :novelId")
    suspend fun getRecordsForNovel(novelId: String): List<TrackerRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecord(record: TrackerRecord)
}

@Dao
interface RepositoryDao {
    @Query("SELECT * FROM community_repositories ORDER BY isDefault DESC, name ASC")
    fun getAllRepositories(): Flow<List<CommunityRepository>>

    @Query("SELECT * FROM community_repositories WHERE id = :id LIMIT 1")
    suspend fun getRepositoryById(id: String): CommunityRepository?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRepository(repo: CommunityRepository)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepositories(repos: List<CommunityRepository>)

    @Query("DELETE FROM community_repositories WHERE id = :id")
    suspend fun deleteRepositoryById(id: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM reading_history ORDER BY readTimestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordHistory(entry: HistoryEntry)

    @Query("DELETE FROM reading_history WHERE id = :id")
    suspend fun deleteHistory(id: String)

    @Query("DELETE FROM reading_history WHERE novelId = :novelId")
    suspend fun deleteHistoryByNovelId(novelId: String)

    @Query("DELETE FROM reading_history")
    suspend fun clearAllHistory()
}

