package com.yaxer.timetrack

import android.app.Application
import com.yaxer.timetrack.data.local.TimeTrackDatabase
import com.yaxer.timetrack.data.network.NetworkMonitor
import com.yaxer.timetrack.data.repository.TimeTrackRepository
import com.yaxer.timetrack.sync.SyncManager

/**
 * Application class for TimeTrack app.
 *
 * Initializes and provides access to:
 * - Room database instance
 * - Network connectivity monitor
 * - Repository for data operations
 */
class TimeTrackApplication : Application() {

    /**
     * Lazy-initialized Room database instance
     */
    val database: TimeTrackDatabase by lazy {
        TimeTrackDatabase.getInstance(this)
    }

    /**
     * Lazy-initialized network monitor for tracking online/offline state
     */
    val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(this)
    }

    /**
     * Lazy-initialized repository for data operations
     */
    val repository: TimeTrackRepository by lazy {
        TimeTrackRepository(database, networkMonitor)
    }

    /**
     * Lazy-initialized sync manager for background sync scheduling
     */
    val syncManager: SyncManager by lazy {
        SyncManager(this)
    }

    override fun onCreate() {
        super.onCreate()

        // Register network monitor to start tracking connectivity changes
        networkMonitor.register()

        // Trigger sync when network becomes available
        networkMonitor.setOnNetworkAvailableListener {
            syncManager.syncNow()
        }

        // Schedule periodic background sync
        syncManager.schedulePeriodicSync()
    }

    override fun onTerminate() {
        super.onTerminate()

        // Unregister network monitor to prevent memory leaks
        networkMonitor.unregister()
    }
}
