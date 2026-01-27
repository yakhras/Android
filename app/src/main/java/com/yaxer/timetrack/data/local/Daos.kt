package com.yaxer.timetrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Project operations
 */
@Dao
interface ProjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<ProjectEntity>)

    @Query("SELECT * FROM projects WHERE active = 1 ORDER BY name ASC")
    fun getAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE active = 1 ORDER BY name ASC")
    suspend fun getAllSync(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: Int): ProjectEntity?

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM projects")
    suspend fun deleteAll()
}

/**
 * DAO for TimeEntry operations
 */
@Dao
interface TimeEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TimeEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<TimeEntryEntity>)

    @Query("SELECT * FROM time_entries ORDER BY date DESC, id DESC")
    fun getAll(): Flow<List<TimeEntryEntity>>

    @Query("SELECT * FROM time_entries ORDER BY date DESC, id DESC")
    suspend fun getAllSync(): List<TimeEntryEntity>

    @Query("SELECT * FROM time_entries WHERE date = :date ORDER BY id DESC")
    fun getByDate(date: String): Flow<List<TimeEntryEntity>>

    @Query("SELECT * FROM time_entries WHERE date = :date ORDER BY id DESC")
    suspend fun getByDateSync(date: String): List<TimeEntryEntity>

    @Query("SELECT * FROM time_entries WHERE id = :id")
    suspend fun getById(id: Int): TimeEntryEntity?

    @Query("SELECT * FROM time_entries WHERE localId = :localId")
    suspend fun getByLocalId(localId: String): TimeEntryEntity?

    @Update
    suspend fun update(entry: TimeEntryEntity)

    @Query("DELETE FROM time_entries WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM time_entries")
    suspend fun deleteAll()
}

/**
 * DAO for SyncQueue operations
 */
@Dao
interface SyncQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    fun getAll(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getAllSync(): List<SyncQueueEntity>

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getCountSync(): Int

    @Update
    suspend fun update(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue")
    suspend fun deleteAll()
}

/**
 * DAO for RunningTimer operations
 */
@Dao
interface RunningTimerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(timer: RunningTimerEntity)

    @Query("SELECT * FROM running_timers")
    fun getAll(): Flow<List<RunningTimerEntity>>

    @Query("SELECT * FROM running_timers LIMIT 1")
    suspend fun getActive(): RunningTimerEntity?

    @Query("DELETE FROM running_timers")
    suspend fun deleteAll()

    @Query("DELETE FROM running_timers WHERE id = :id")
    suspend fun deleteById(id: Int)
}
