package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        Novel::class,
        Chapter::class,
        UserProfile::class,
        ScraperExtension::class,
        TrackerRecord::class,
        CommunityRepository::class,
        HistoryEntry::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun novelDao(): NovelDao
    abstract fun chapterDao(): ChapterDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun extensionDao(): ExtensionDao
    abstract fun trackerDao(): TrackerDao
    abstract fun repositoryDao(): RepositoryDao
    abstract fun historyDao(): HistoryDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "auranovel_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
