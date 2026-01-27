package com.yaxer.timetrack.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Manages scheduling of sync operations using WorkManager.
 */
class SyncManager(private val context: Context) {

    companion object {
        private const val SYNC_WORK_NAME = "timetrack_sync"
        private const val SYNC_NOW_WORK_NAME = "timetrack_sync_now"
        private const val SYNC_INTERVAL_MINUTES = 15L
    }

    /**
     * Schedule periodic background sync every 15 minutes.
     * Only runs when network is available.
     */
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    /**
     * Trigger immediate sync (for pull-to-refresh).
     * Only runs when network is available.
     * Uses unique work to prevent duplicate sync operations.
     */
    fun syncNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            SYNC_NOW_WORK_NAME,
            ExistingWorkPolicy.KEEP,  // Skip if already running
            request
        )
    }

    /**
     * Cancel all scheduled sync work.
     */
    fun cancelAll() {
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
    }
}
