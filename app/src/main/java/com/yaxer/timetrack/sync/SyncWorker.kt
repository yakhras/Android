package com.yaxer.timetrack.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yaxer.timetrack.TimeTrackApplication

/**
 * Worker that processes the sync queue in the background.
 * Called by WorkManager when network is available.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as TimeTrackApplication

        return try {
            app.repository.syncPendingChanges()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
