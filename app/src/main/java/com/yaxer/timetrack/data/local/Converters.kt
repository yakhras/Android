package com.yaxer.timetrack.data.local

import androidx.room.TypeConverter

/**
 * Type converters for Room database
 */
class Converters {

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String {
        return status.name
    }

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return SyncStatus.valueOf(value)
    }

    @TypeConverter
    fun fromSyncOperation(operation: SyncOperation): String {
        return operation.name
    }

    @TypeConverter
    fun toSyncOperation(value: String): SyncOperation {
        return SyncOperation.valueOf(value)
    }
}
