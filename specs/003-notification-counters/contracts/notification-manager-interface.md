# Contract: CounterNotificationManager Interface

**Purpose**: Define the public API for managing counter notifications
**Feature**: Show Active Counters in Notification Shade
**Type**: Kotlin Interface Contract

## Interface Definition

```kotlin
/**
 * Manages the lifecycle of notifications for active timers/stopwatches.
 *
 * Responsibilities:
 * - Create notifications when counters start
 * - Update notification display text every 1 second
 * - Handle counter-specific tap actions
 * - Remove notifications when counters stop
 * - Respect user notification permissions
 */
interface CounterNotificationManager {

    /**
     * Start displaying a notification for an active counter.
     *
     * @param counter The Counter (Timer or Stopwatch) to display
     * @return Result.success(notificationId) if created successfully
     *         Result.failure if notification permission denied or invalid counter
     *
     * Contract:
     * - Notification appears in shade within 1 second
     * - Notification display name format: "{name} {type}"
     * - Notification includes elapsed time in HH:MM:SS format
     * - Tapping notification opens app and focuses this counter
     */
    suspend fun startNotification(counter: Counter): Result<Int>

    /**
     * Update a notification's elapsed time display.
     *
     * Called every 1 second by background update mechanism.
     *
     * @param counterId The unique counter ID
     * @param newElapsedTimeMs Updated elapsed time in milliseconds
     * @return Result.success if updated, Result.failure if counter not found
     *
     * Contract:
     * - Update occurs within 100ms of call
     * - Elapsed time display format: HH:MM:SS
     * - No permission requests or user interactions
     */
    suspend fun updateNotificationTime(
        counterId: String,
        newElapsedTimeMs: Long
    ): Result<Unit>

    /**
     * Stop displaying a notification for a counter.
     *
     * @param counterId The unique counter ID
     * @return Result.success if removed, Result.failure if not found
     *
     * Contract:
     * - Notification removed from shade immediately
     * - Android handles animation/cleanup
     */
    suspend fun stopNotification(counterId: String): Result<Unit>

    /**
     * Get all currently displayed notifications.
     *
     * @return List of CounterNotification objects
     *
     * Contract:
     * - Returns only notifications for running counters
     * - List reflects current state
     * - Empty list if no active counters
     */
    fun getActiveNotifications(): List<CounterNotification>

    /**
     * Check if notification is displayed for specific counter.
     *
     * @param counterId The counter ID to check
     * @return true if notification is currently active, false otherwise
     */
    fun isNotificationActive(counterId: String): Boolean

    /**
     * Register callback for notification tap events.
     *
     * @param counterId The counter to monitor
     * @param callback Invoked when user taps the notification
     */
    fun onNotificationTapped(counterId: String, callback: () -> Unit)

    /**
     * Stop all active notifications.
     *
     * Contract:
     * - All notifications removed from shade
     * - Called when last counter stops or app exits
     */
    suspend fun stopAllNotifications(): Result<Unit>

    /**
     * Request notification permission from user.
     *
     * @return Result.success(true) if permission granted
     *         Result.success(false) if user denied
     *         Result.failure if system error
     *
     * Contract:
     * - Only call if permission not already granted
     * - Called once per app session
     */
    suspend fun requestNotificationPermission(): Result<Boolean>

    /**
     * Check if app has notification permission.
     *
     * @return true if POST_NOTIFICATIONS permission granted
     */
    fun hasNotificationPermission(): Boolean
}
```

---

## Usage Contracts

### Scenario 1: User Starts a Timer

```
USER ACTION: User taps "Start Timer" button
  │
  └─> Timer.start() called
      │
      └─> TimerViewModel observes counter state change
          │
          └─> Calls: counterNotificationManager.startNotification(timer)
              │
              └─> Result.success(notificationId: 12345)
                  │
                  └─> Notification appears in shade with:
                      - Display name: "Workout Timer"
                      - Elapsed time: "00:00:15"
                      - Updates every 1 second
```

### Scenario 2: Background Update Loop (Every 1 Second)

```
SYSTEM: WorkManager periodic job triggers
  │
  └─> NotificationUpdateWorker.doWork()
      │
      └─> Gets active counters: [Timer("Workout"), Stopwatch("Break")]
          │
          ├─> counterNotificationManager.updateNotificationTime("timer-1", 75000)
          │   └─> Result.success()
          │
          └─> counterNotificationManager.updateNotificationTime("stopwatch-1", 125000)
              └─> Result.success()
                  │
                  └─> Notifications update to:
                      - Timer: "00:01:15"
                      - Stopwatch: "00:02:05"
```

### Scenario 3: User Taps Notification

```
USER ACTION: User taps "Workout Timer" notification in shade
  │
  └─> Android Intent fires (registered by CounterNotificationManager)
      │
      └─> App opens
          │
          └─> CounterNotificationManager.onNotificationTapped("timer-1")
              │
              └─> Callback executes: Focus on Timer UI
                  └─> Show Timer details, controls, elapsed time
```

### Scenario 4: User Stops Timer

```
USER ACTION: User taps Stop button
  │
  └─> Timer.stop() called (isRunning = false)
      │
      └─> TimerViewModel observes state change
          │
          └─> Calls: counterNotificationManager.stopNotification("timer-1")
              │
              └─> Result.success()
                  │
                  └─> Notification removed from shade immediately
```

### Scenario 5: Multiple Counters Running

```
USER ACTION: User starts 2nd timer while first is running
  │
  └─> Timer2.start() called
      │
      └─> Calls: counterNotificationManager.startNotification(timer2)
          │
          └─> Result.success(notificationId: 12346)
              │
              └─> Notification shade shows:
                  - "Workout Timer" : "00:02:30"
                  - "Meeting Timer" : "00:00:05"
                  (each independently tappable and updatable)
```

---

## Error Handling Contracts

| Scenario | Expected Behavior |
|----------|------------------|
| **Permission Denied** | `Result.failure(PermissionDeniedException)` - App continues normally (no crash) |
| **Counter Not Found** | `Result.failure(CounterNotFoundException)` - Logged, operation ignored |
| **Invalid Counter State** | `Result.failure(InvalidStateException)` - Counter already running or stopped |
| **System Error** | `Result.failure(Exception)` - Logged to Crashlytics if available |
| **Update Called on Stopped Counter** | `Result.failure()` - Ignored gracefully |

---

## Performance Contracts

| Metric | Target | Measurement |
|--------|--------|-------------|
| Notification Creation | < 500ms | From startNotification() call to visible in shade |
| Time Update | < 100ms | From updateNotificationTime() call to display change |
| Elapsed Time Accuracy | ±1 second | Updated every 1 second, no drift >1 sec |
| Memory per Notification | < 50KB | In-memory state (excluding UI resources) |
| Notification Removal | Immediate | Appears removed when stopNotification() called |

---

## Threading Contracts

All interface methods are **suspend functions** (Kotlin coroutines).

```kotlin
// All operations assume Dispatcher.Main context
// Implementation must switch to IO if needed (e.g., database writes)

coroutineScope.launch {
    // Safe to call from UI thread
    counterNotificationManager.startNotification(counter)
}
```

---

## Permission Contracts

**Required Permission**: `android.permission.POST_NOTIFICATIONS` (Android 13+)

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

**Runtime Behavior**:
- Android 12 and below: No runtime permission needed (automatically granted)
- Android 13+: Must request at runtime, handle denial gracefully
- Denial does not crash app; notifications simply don't appear

---

## Lifecycle Contracts

```
APP START
  │
  ├─ hasNotificationPermission() checks permission
  │
  ├─ (if denied) requestNotificationPermission() called once
  │
  └─ Ready for notification operations

COUNTER CREATED & STARTS
  │
  └─ startNotification(counter) → notification appears

COUNTER RUNNING (every 1 second)
  │
  └─ updateNotificationTime() → elapsed time updates

COUNTER STOPS
  │
  └─ stopNotification() → notification removed

APP BACKGROUNDED
  │
  └─ Notifications persist (handled by Android OS)
     WorkManager continues periodic updates

APP DESTROYED
  │
  └─ stopAllNotifications() called (cleanup)
```

---

## Testing Contracts

### Unit Test Expectations

```kotlin
@Test
fun startNotification_WithValidCounter_ReturnsSuccessWithId()
fun startNotification_WithoutPermission_ReturnsFailure()
fun updateNotificationTime_WithActiveCounter_Updates()
fun stopNotification_WithActiveCounter_Removes()
fun stopAllNotifications_RemovesAll()
```

### Integration Test Expectations

```kotlin
@Test
fun notification_AppearsInNotificationShade_AndUpdatesEverySecond()
fun notification_Persists_WhenAppBackgrounded()
fun multipleNotifications_DisplaysAllIndependently()
```

---

## Implementation Notes

- Default implementation: `CounterNotificationManagerImpl`
- Uses Android NotificationManager + NotificationCompat
- Depends on: WorkManager, Coroutines, AndroidX Lifecycle
- Thread-safe: All state changes go through coroutines
- Lifecycle-aware: Respects app foreground/background state

**Ready for Implementation**: Phase 2 tasks will use this contract
