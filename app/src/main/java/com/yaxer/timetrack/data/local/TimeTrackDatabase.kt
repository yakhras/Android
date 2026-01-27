package com.yaxer.timetrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room database for TimeTrack app
 *
 * Contains tables for:
 * - Projects: Cached project data from Odoo
 * - Time Entries: Cached time entry data from Odoo
 * - Sync Queue: Pending offline operations to sync
 * - Running Timers: Persistent timer state
 */
@Database(
    entities = [
        ProjectEntity::class,
        TimeEntryEntity::class,
        SyncQueueEntity::class,
        RunningTimerEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TimeTrackDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun timeEntryDao(): TimeEntryDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun runningTimerDao(): RunningTimerDao

    companion object {
        private const val DATABASE_NAME = "timetrack.db"

        @Volatile
        private var INSTANCE: TimeTrackDatabase? = null

        fun getInstance(context: Context): TimeTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): TimeTrackDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                TimeTrackDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
