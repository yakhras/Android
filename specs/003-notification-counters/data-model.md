# Data Model: Show Active Counters in Notification Shade

**Purpose**: Define entities and data structures for notification feature
**Date**: 2026-01-19
**Feature**: Show Active Counters in Notification Shade

## Entity Overview

This feature reuses existing Timer/Stopwatch entities and defines new notification-specific entities.

---

## Entities

### 1. Counter (Interface/Sealed Class)

Abstract representation of Timer or Stopwatch with common properties.

```kotlin
sealed class Counter {
    abstract val id: String              // Unique identifier (UUID)
    abstract val name: String            // User-assigned name (e.g., "Lunch Break")
    abstract val elapsedTimeMs: Long     // Current elapsed time in milliseconds
    abstract val isRunning: Boolean      // Current state
    abstract val type: CounterType       // TIMER or STOPWATCH
    abstract val createdAtMs: Long       // Creation timestamp for fallback naming

    data class Timer(
        override val id: String,
        override val name: String,
        override val elapsedTimeMs: Long,
        override val isRunning: Boolean,
        override val type: CounterType = CounterType.TIMER,
        override val createdAtMs: Long,
        val targetDurationMs: Long      // Original timer duration
    ) : Counter()

    data class Stopwatch(
        override val id: String,
        override val name: String,
        override val elapsedTimeMs: Long,
        override val isRunning: Boolean,
        override val type: CounterType = CounterType.STOPWATCH,
        override val createdAtMs: Long,
        val laps: List<LapTime> = emptyList()  // Lap records (if feature implemented)
    ) : Counter()
}

enum class CounterType {
    TIMER,
    STOPWATCH
}
```

**Validation Rules**:
- ID: Non-empty, immutable after creation
- Name: Non-empty, max 30 characters (for notification display)
- ElapsedTimeMs: Non-negative integer
- IsRunning: Boolean state
- CreatedAtMs: Valid timestamp

**State Transitions**:
```
[Idle] --start--> [Running] --pause--> [Paused] --resume--> [Running]
                                          |
                                       --stop--> [Idle]
       (notification only shows when isRunning = true)
```

---

### 2. CounterNotification

Represents a single notification in the notification shade.

```kotlin
data class CounterNotification(
    val counter: Counter,
    val notificationId: Int,            // Android notification ID (hash of counter.id)
    val displayName: String,            // Name + Type for display (e.g., "Lunch Break Timer")
    val lastUpdatedMs: Long,            // Timestamp of last update
    val updateFrequencyMs: Int = 1000   // Update interval (1 second)
) {
    // Computed property
    val isValid: Boolean get() = counter.isRunning && displayName.isNotEmpty()

    fun formattedElapsedTime(): String {
        // Format: HH:MM:SS
        val totalSeconds = counter.elapsedTimeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
```

**Validation Rules**:
- NotificationId: Unique positive integer (hash of counter ID)
- DisplayName: Non-empty, max 30 chars
- UpdateFrequencyMs: Must be >= 1000 (1 second minimum)
- Counter: Must have isRunning = true

**Lifecycle**:
```
Counter.isRunning = true
        |
        v
CounterNotification.create()
        |
        v
[Active - updating every 1 second]
        |
        v
Counter.isRunning = false
        |
        v
CounterNotification.cancel()
```

---

### 3. LapTime (Supporting Entity)

Records individual lap segments within a stopwatch session.

```kotlin
data class LapTime(
    val id: String,                     // Unique lap ID
    val durationMs: Long,               // Lap duration
    val startTimeMs: Long,              // When lap started
    val endTimeMs: Long                 // When lap ended
) {
    val displayName: String get() = "Lap ${id.hashCode() % 100}"
}
```

**Validation Rules**:
- DurationMs: startTimeMs must be < endTimeMs
- All timestamps: Valid Unix milliseconds

**Note**: Lap display in notifications is basic (only shows total for simplicity in Phase 1)

---

## Relationships

```
┌─────────────────────────────────────────┐
│         Timer/Stopwatch (Existing)      │
│  - id: String                           │
│  - name: String                         │
│  - elapsedTimeMs: Long                  │
│  - isRunning: Boolean                   │
│  - type: CounterType                    │
│  - createdAtMs: Long                    │
└─────────────────────────────────────────┘
         │ (1:1 relationship)
         │ (only when isRunning = true)
         ▼
┌─────────────────────────────────────────┐
│    CounterNotification                  │
│  - counter: Counter                     │
│  - notificationId: Int                  │
│  - displayName: String                  │
│  - lastUpdatedMs: Long                  │
└─────────────────────────────────────────┘
         │ (created per active counter)
         │
         └─── Updates every 1 second
              (via WorkManager)
```

**Multiplicity**:
- 1 Timer/Stopwatch → 0 or 1 CounterNotification (1 if running, 0 if stopped)
- N active counters → N CounterNotifications (one per active counter)
- Stopwatch → 0..M LapTimes (internal lap records)

---

## State Management

### Counter Active Status

```kotlin
// Query: Get all active counters
fun getActiveCounters(): Flow<List<Counter>> {
    // Returns all counters where isRunning = true
    // Used to generate/update notifications
}

// Update: When counter starts/stops
fun updateCounterStatus(counterId: String, isRunning: Boolean) {
    // Triggers notification creation/cancellation
}
```

### Notification State

```kotlin
// Internal notification state
object NotificationState {
    val activeNotifications: MutableMap<String, CounterNotification> = mutableMapOf()

    fun addNotification(notification: CounterNotification) {
        activeNotifications[notification.counter.id] = notification
    }

    fun removeNotification(counterId: String) {
        activeNotifications.remove(counterId)
    }

    fun getNotificationForCounter(counterId: String): CounterNotification? {
        return activeNotifications[counterId]
    }
}
```

---

## Display Rules

### Notification Display Name

```
Format: "{UserAssignedName} {CounterType}"

Examples:
- "Lunch Break Timer"
- "Code Review Stopwatch"
- "Meeting (auto-generated)" [if unnamed]

Rules:
- Max 30 characters total
- If name > 25 chars: Truncate + ellipsis
- CounterType always appended (space-separated)
- Fallback if no name: "Unnamed {Type}" or timestamp-based
```

### Elapsed Time Display

```
Format: HH:MM:SS

Display Rules:
- Always 2-digit zero-padded
- Updates every 1 second
- Example: 00:05:30 (5 minutes, 30 seconds)
- Large duration (24+ hours): Still shows HH:MM:SS (e.g., 25:00:00)
```

---

## Data Persistence

### Required Storage

**In-Memory** (during app session):
- Active counter references
- Current notification IDs
- Last update timestamps

**Persistent Storage** (across sessions):
- Counter names (must add if not in existing model)
- Counter IDs (already stored)
- User preferences (notification enabled/disabled)

### Storage Implementation

```kotlin
// Shared preferences for counter names/preferences
object CounterPreferences {
    fun saveCounterName(counterId: String, name: String)
    fun getCounterName(counterId: String): String?
    fun getDefaultName(counter: Counter): String
}

// Room Database (if counter data already stored)
// Notification manager queries Timer/Stopwatch DAO for active counters
```

---

## Edge Cases & Validation

| Case | Handling |
|------|----------|
| Counter with no name | Use fallback: "Unnamed {Type}" or prompt user |
| Multiple counters same name | Display is still distinct (only user sees internally) |
| Counter name > 30 chars | Truncate to 30 chars in notification |
| App crashes while notification active | Notification remains (Android OS manages) |
| Stopwatch with laps | Display total time only (lap breakdown future) |
| Notification permission denied | Skip notification creation (app continues) |
| Counter > 24 hours | Display as HH:MM:SS (e.g., 25:01:15) |
| Simultaneous tap on 2 counters | Each tap opens respective counter independently |

---

## Assumptions

- Timer/Stopwatch entities already exist with ID, name, isRunning, elapsedTimeMs, type
- If name not in existing model, will be added (required for feature)
- Android provides unique notification IDs (via hash)
- WorkManager available (already AndroidX dependency)
- Flow/LiveData patterns used (standard modern Android)

---

## Phase 2 Readiness

✅ All entities defined
✅ Relationships mapped
✅ State transitions documented
✅ Validation rules specified
✅ Edge cases handled

**Status**: Ready for Phase 1 artifact generation (contracts, quickstart)
