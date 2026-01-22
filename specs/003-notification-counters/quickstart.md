# Quickstart: Show Active Counters in Notification Shade

**Feature**: Show Active Counters in Notification Shade
**Status**: Ready for Development
**Duration**: ~2-3 weeks (P1: ~1 week, P2: ~1 week, testing & refinement: ~1 week)

## Setup

### Prerequisites

- Android Studio 2022.1+
- Kotlin 1.9+
- Project already has: Timer/Stopwatch features, AndroidX dependencies
- Target API 26+ (existing project setting)

### Dependencies to Add (if not present)

Add to `app/build.gradle.kts`:

```kotlin
dependencies {
    // Notification (compat for API 26+)
    implementation("androidx.core:core:1.10.1")

    // Background execution
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    // Already present (assumed):
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // Testing (for notification tests)
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("androidx.test:core:1.5.0")
}
```

### Manifest Permissions

Add to `AndroidManifest.xml`:

```xml
<!-- Notifications -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Foreground service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<!-- In application block -->
<service
    android:name="com.yaxer.timetrack.notification.CounterNotificationService"
    android:foregroundServiceType="shortService"
    android:exported="false" />
```

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    TimerViewModel / StopwatchViewModel       │
│  (observes counter state, calls notification manager)        │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│            CounterNotificationManager (Interface)            │
│  ✓ startNotification(counter)                                │
│  ✓ updateNotificationTime(counterId, elapsedMs)              │
│  ✓ stopNotification(counterId)                               │
│  ✓ getActiveNotifications()                                  │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        ▼                         ▼
┌──────────────────────┐  ┌──────────────────────┐
│ CounterNotification  │  │ NotificationUpdate   │
│ Service              │  │ Worker               │
│ (foreground)         │  │ (periodic updates)   │
│                      │  │ (WorkManager)        │
└──────────────────────┘  └──────────────────────┘
        │                         │
        └────────────┬────────────┘
                     ▼
        ┌─────────────────────────┐
        │  Android Notification   │
        │  Manager                │
        └─────────────────────────┘
                     │
                     ▼
        ┌─────────────────────────┐
        │  Notification Shade     │
        │  (user visible)         │
        └─────────────────────────┘
```

---

## Phase 1: Single Counter Notifications (MVP)

### Goal
User can see a single active counter in the notification shade with real-time updates.

### Implementation Steps

#### Step 1: Create Counter Interface (Reuse if exists)
**File**: `app/src/main/java/com/yaxer/timetrack/notification/Counter.kt`

```kotlin
sealed class Counter {
    abstract val id: String
    abstract val name: String
    abstract val elapsedTimeMs: Long
    abstract val isRunning: Boolean
    abstract val type: CounterType
    abstract val createdAtMs: Long
}

enum class CounterType {
    TIMER, STOPWATCH
}
```

#### Step 2: Create CounterNotificationManager Interface
**File**: `app/src/main/java/com/yaxer/timetrack/notification/CounterNotificationManager.kt`

See: `/contracts/notification-manager-interface.md`

#### Step 3: Implement CounterNotificationManager
**File**: `app/src/main/java/com/yaxer/timetrack/notification/CounterNotificationManagerImpl.kt`

Key implementation:
```kotlin
class CounterNotificationManagerImpl(
    private val context: Context,
    private val notificationManager: NotificationManager
) : CounterNotificationManager {

    companion object {
        private const val CHANNEL_ID = "active_counters"
        private const val CHANNEL_NAME = "Active Timers & Stopwatches"
    }

    init {
        createNotificationChannel()
    }

    override suspend fun startNotification(counter: Counter): Result<Int> {
        // Check permission first
        if (!hasNotificationPermission()) {
            return Result.failure(SecurityException("No notification permission"))
        }

        // Create notification ID from counter ID
        val notificationId = counter.id.hashCode()

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(counter.name)
            .setContentText(formatElapsedTime(counter.elapsedTimeMs))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)  // Can't swipe away
            .setAutoCancel(false)
            .setContentIntent(getPendingIntent(counter.id))
            .setStyle(NotificationCompat.BigTextStyle())
            .build()

        // Show notification
        try {
            notificationManager.notify(notificationId, notification)
            return Result.success(notificationId)
        } catch (e: SecurityException) {
            return Result.failure(e)
        }
    }

    override suspend fun updateNotificationTime(
        counterId: String,
        newElapsedTimeMs: Long
    ): Result<Unit> {
        val notificationId = counterId.hashCode()
        val counter = getCounterForUpdate(counterId, newElapsedTimeMs)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentText(formatElapsedTime(newElapsedTimeMs))
            .setOngoing(true)
            .build()

        return try {
            notificationManager.notify(notificationId, notification)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stopNotification(counterId: String): Result<Unit> {
        val notificationId = counterId.hashCode()
        return try {
            notificationManager.cancel(notificationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatElapsedTime(elapsedMs: Long): String {
        val totalSeconds = elapsedMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active timers and stopwatches"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    // ... other methods
}
```

#### Step 4: Create NotificationUpdateWorker (Background Updates)
**File**: `app/src/main/java/com/yaxer/timetrack/notification/NotificationUpdateWorker.kt`

```kotlin
class NotificationUpdateWorker(
    context: Context,
    params: WorkerParameters,
    private val notificationManager: CounterNotificationManager,
    private val timerRepository: TimerRepository,  // Existing repo
    private val stopwatchRepository: StopwatchRepository  // Existing repo
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.Main) {
        try {
            // Get all active counters
            val activeTimers = timerRepository.getActiveTimers()
            val activeStopwatches = stopwatchRepository.getActiveStopwatches()

            // Update each notification
            activeTimers.forEach { timer ->
                notificationManager.updateNotificationTime(timer.id, timer.elapsedTimeMs)
            }
            activeStopwatches.forEach { stopwatch ->
                notificationManager.updateNotificationTime(stopwatch.id, stopwatch.elapsedTimeMs)
            }

            Result.retry()  // Keep worker running
        } catch (e: Exception) {
            Log.e("NotificationUpdateWorker", "Error updating notifications", e)
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val updateWork = PeriodicWorkRequestBuilder<NotificationUpdateWorker>(
                1, TimeUnit.SECONDS  // Update every 1 second
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "counter_notification_updates",
                ExistingPeriodicWorkPolicy.KEEP,
                updateWork
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork("counter_notification_updates")
        }
    }
}
```

#### Step 5: Hook into TimerViewModel/Stopwatch Start/Stop

**File**: `app/src/.../TimerViewModel.kt` (modify existing)

```kotlin
class TimerViewModel(
    private val timerRepository: TimerRepository,
    private val notificationManager: CounterNotificationManager
) : ViewModel() {

    fun startTimer(name: String, durationMs: Long) {
        viewModelScope.launch {
            val timer = timerRepository.createTimer(name, durationMs)

            // Show notification
            notificationManager.startNotification(timer)

            // Start background updates
            NotificationUpdateWorker.schedule(context)
        }
    }

    fun stopTimer(timerId: String) {
        viewModelScope.launch {
            timerRepository.stopTimer(timerId)

            // Remove notification
            notificationManager.stopNotification(timerId)

            // If no more active counters, stop worker
            if (isAnyCounterActive().not()) {
                NotificationUpdateWorker.cancel(context)
            }
        }
    }
}
```

---

## Phase 2: Multiple Counter Notifications

### Goal
User can see multiple active counters in the notification shade simultaneously.

### Changes

1. **Notification Grouping** (optional):
   ```kotlin
   // In CounterNotificationManagerImpl
   .setGroup("active_counters")
   .setGroupSummary(false)  // Don't show summary notification
   ```

2. **Counter Identification**:
   - Each counter gets unique notificationId (hashCode of counter ID)
   - Display name includes user-assigned name
   - Example: "Lunch Break Timer", "Code Review Stopwatch"

3. **Multiple Tap Handlers**:
   ```kotlin
   // Each counter has distinct PendingIntent
   private fun getPendingIntent(counterId: String): PendingIntent {
       return PendingIntent.getActivity(
           context,
           counterId.hashCode(),  // Unique requestCode per counter
           Intent(context, MainActivity::class.java).apply {
               putExtra("focus_counter_id", counterId)
           },
           PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
       )
   }
   ```

4. **No Major Code Changes** - Implementation reuses Phase 1 code
   - Just handle multiple notifications in same maps/state
   - WorkManager naturally updates all active counters

---

## Testing

### Unit Tests
- **File**: `app/src/test/java/com/yaxer/timetrack/notification/CounterNotificationManagerTest.kt`

```kotlin
class CounterNotificationManagerTest {

    @Test
    fun startNotification_WithValidCounter_UpdatesNotificationManager() {
        // Given
        val timer = mockTimer(id = "123", name = "Workout")

        // When
        val result = runBlocking {
            notificationManager.startNotification(timer)
        }

        // Then
        assert(result.isSuccess)
        verify(mockNotificationManager).notify(any(), any())
    }

    @Test
    fun updateNotificationTime_UpdatesDisplayedTime() {
        // Given
        val newElapsedMs = 5000L

        // When
        val result = runBlocking {
            notificationManager.updateNotificationTime("123", newElapsedMs)
        }

        // Then
        assert(result.isSuccess)
        // Verify notification was updated with new time
    }
}
```

### Integration Tests
- **File**: `app/src/androidTest/java/com/yaxer/timetrack/notification/CounterNotificationServiceTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class CounterNotificationServiceTest {

    @Test
    fun notification_AppearsInNotificationShade_WhenCounterStarts() {
        // Given
        val timer = createTestTimer("Workout")

        // When
        notificationManager.startNotification(timer)

        // Then
        // Assert notification visible in shadow (use onView matcher)
        onView(withText("Workout")).check(matches(isDisplayed()))
    }

    @Test
    fun notification_UpdatesEverySecond() {
        // Given - timer running
        notificationManager.startNotification(testTimer)

        // When - wait 2 seconds
        Thread.sleep(2000)

        // Then - verify update was called twice
        // Assert elapsed time increased
    }
}
```

---

## Validation Checklist

✅ **P1: Single Counter**
- [ ] Notification appears when timer/stopwatch starts
- [ ] Notification displays correct elapsed time
- [ ] Elapsed time updates every 1 second
- [ ] Tapping notification opens app
- [ ] Notification removed when counter stops
- [ ] No lint warnings in notification code
- [ ] Unit tests pass (60% coverage)
- [ ] Instrumentation tests pass

✅ **P2: Multiple Counters**
- [ ] Multiple notifications display simultaneously
- [ ] Each notification updates independently
- [ ] Each notification can be tapped individually
- [ ] Notifications removed when respective counters stop
- [ ] No UI overlap or confusion

✅ **General**
- [ ] Permissions handled gracefully
- [ ] App doesn't crash if permission denied
- [ ] Notification persists when app backgrounded
- [ ] WorkManager respects background limits
- [ ] Battery impact minimal
- [ ] No memory leaks (verify with ProfilerView)

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| **Notification not appearing** | Check: (1) Permission granted (2) Counter.isRunning = true (3) API 26+ |
| **Updates stop after 5 mins** | WorkManager constraints may pause work. Check Doze mode settings. |
| **Multiple notifications overlap** | Use unique IDs per counter. Notification grouping optional. |
| **Crashes on permission deny** | Always check `hasNotificationPermission()` before startNotification() |
| **Memory leak in notifications** | Ensure PendingIntent has FLAG_IMMUTABLE + proper cleanup in onDestroy |

---

## Success Metrics

By end of development, verify:

- ✅ SC-001: Updates within 1 second of actual time
- ✅ SC-002: Multiple counters display without confusion
- ✅ SC-003: 95% users identify correct counter on tap
- ✅ SC-004: No data loss while backgrounded
- ✅ SC-005: All counters display within 2 seconds of start

---

## Files to Create/Modify

| File | Type | Status |
|------|------|--------|
| `notification/Counter.kt` | New | P1 |
| `notification/CounterNotificationManager.kt` | New (interface) | P1 |
| `notification/CounterNotificationManagerImpl.kt` | New | P1 |
| `notification/NotificationUpdateWorker.kt` | New | P1 |
| `TimerViewModel.kt` | Modify | P1 |
| `StopwatchViewModel.kt` | Modify | P1 |
| `AndroidManifest.xml` | Modify | P1 |
| `app/build.gradle.kts` | Modify | P1 |
| Test files (3) | New | P1/P2 |

---

## Timeline Estimate

- P1 (Single counter): ~1 week
  - Core notification infrastructure
  - Timer/Stopwatch integration
  - Basic testing

- P2 (Multiple counters): ~4-5 days
  - Minimal code changes (reuse P1)
  - Multiple notification handling
  - Integration testing

- Polish & Testing: ~3-5 days
  - Edge cases
  - Performance optimization
  - Final QA

**Total**: ~2-3 weeks for full feature (MVP + advanced + polish)
