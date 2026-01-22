# Implementation Plan: Show Active Counters in Notification Shade

**Branch**: `003-notification-counters` | **Date**: 2026-01-19 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/003-notification-counters/spec.md`

## Summary

Enable users to see all active timers and stopwatches in the Android notification shade with real-time elapsed time updates. When a timer or stopwatch starts, a foreground notification appears displaying the counter name and current time, updating every second. Users can tap a notification to focus on that specific counter. Supports multiple simultaneous counters, each displayed separately with user-assigned names for clear identification.

**MVP (P1)**: Single counter notification with real-time updates
**Advanced (P2)**: Multiple simultaneous counter notifications

## Technical Context

**Language/Version**: Kotlin 1.9+ with Android API 26+ (API 34 target)
**Primary Dependencies**:
- AndroidX Lifecycle (for background-safe operations)
- AndroidX Core (for NotificationCompat API compatibility)
- Coroutines + Flow (for real-time updates)

**Storage**: Local (SharedPreferences or Room DB for counter metadata)
**Testing**: JUnit 4 + Mockito (unit), Espresso (instrumentation)
**Target Platform**: Android API 26-34
**Project Type**: Mobile (single Android app)
**Performance Goals**: Notifications update within 1 second of actual elapsed time
**Constraints**:
- Foreground notification required (Android 8.0+)
- Must work when app is backgrounded
- Battery-efficient background updates (avoid excessive wakeups)
- Respect user notification permissions

**Scale/Scope**: Single device, local time tracking, up to ~10 simultaneous counters

## Constitution Check

✅ **Code Quality Principle**: Notification implementation MUST follow Android best practices, no lint warnings
- Notification API usage follows AndroidX compat patterns
- No deprecated NotificationManager APIs

✅ **Testing Discipline**: Notification updates MUST be tested
- Unit tests for notification creation/updates
- Instrumentation tests for foreground service lifecycle
- Target: 60% coverage for notification service logic

✅ **Simplicity Principle**: Keep notification implementation focused
- Single responsibility: display active counters
- Reuse existing timer/stopwatch data models
- No new architectural complexity

**Status**: ✅ All principles satisfied - proceed to design phase

## Project Structure

### Documentation (this feature)

```text
specs/003-notification-counters/
├── plan.md              # This file
├── research.md          # Phase 0 (research findings)
├── data-model.md        # Phase 1 (data entities)
├── quickstart.md        # Phase 1 (setup guide)
├── contracts/           # Phase 1 (notification contracts)
└── checklists/
    └── requirements.md  # Quality checklist
```

### Source Code (repository root)

```text
app/src/main/java/com/yaxer/timetrack/
├── notification/
│   ├── CounterNotificationManager.kt      # Manages notification lifecycle
│   ├── CounterNotificationService.kt      # Foreground service for notifications
│   └── NotificationUpdateWorker.kt        # Background updates via WorkManager
│
└── (existing timer/stopwatch code reused)

app/src/test/java/com/yaxer/timetrack/notification/
├── CounterNotificationManagerTest.kt
└── NotificationUpdateWorkerTest.kt

app/src/androidTest/java/com/yaxer/timetrack/notification/
└── CounterNotificationServiceTest.kt
```

**Structure Decision**: Single mobile app architecture - notifications integrate with existing timer/stopwatch modules. No new data layer needed (reuses existing counter data). Foreground service pattern used for background notifications.

## Complexity Tracking

**No Constitution violations identified.** Feature is straightforward notification display built on existing counter infrastructure.

---

## Phase 0: Research & Decisions ✅ COMPLETE

**Output**: `research.md`

Research resolved:
1. Android foreground notifications + WorkManager for real-time updates
2. Battery-efficient 1-second update frequency using coroutines
3. Multiple simultaneous notifications via unique IDs per counter
4. User-assigned names for counter identification
5. Runtime permission handling for Android 13+
6. Background execution compliance (Doze mode, battery optimization)
7. Data model reuse (no new storage needed)

**Status**: ✅ All research complete, no unresolved clarifications

---

## Phase 1: Design & Contracts ✅ COMPLETE

**Output**: `research.md`, `data-model.md`, `quickstart.md`, `contracts/notification-manager-interface.md`

### 1. Data Model (`data-model.md`)

Entities defined:
- **Counter** (sealed class): Reusable interface for Timer/Stopwatch
  - Properties: id, name, elapsedTimeMs, isRunning, type, createdAtMs
  - Validation: Non-empty ID/name, non-negative time
  - State transitions: Idle → Running → Paused → Stopped

- **CounterNotification**: Represents a displayed notification
  - Properties: counter, notificationId, displayName, lastUpdatedMs
  - Lifecycle: Created when counter starts, updated every 1s, removed when stopped
  - Formatting: "Name Type" (e.g., "Lunch Break Timer")

- **LapTime**: Supporting entity for stopwatch laps (basic support)
  - Properties: id, durationMs, startTimeMs, endTimeMs

Relationships:
- 1 Counter → 0..1 CounterNotification (1 if running, 0 if stopped)
- N active counters → N notifications (independent)

Display rules:
- Name + Type format (max 30 chars)
- Elapsed time: HH:MM:SS (updated every 1 second)
- No UI overlap with grouping support

**Status**: ✅ All entities defined, relationships mapped, validation rules specified

### 2. API Contracts (`contracts/notification-manager-interface.md`)

Interface: `CounterNotificationManager`

Core methods:
- `startNotification(counter)` → Result<Int> (notification ID)
- `updateNotificationTime(counterId, elapsedMs)` → Result<Unit>
- `stopNotification(counterId)` → Result<Unit>
- `getActiveNotifications()` → List<CounterNotification>
- `isNotificationActive(counterId)` → Boolean
- `stopAllNotifications()` → Result<Unit>

Usage contracts:
- Scenarios documented: user starts timer, background updates, notification tap, multiple counters
- Error handling: permission denied, counter not found, invalid state
- Performance: notification creation <500ms, update <100ms, accuracy ±1s
- Threading: All methods are suspend functions (coroutine-safe)

**Status**: ✅ Interface fully specified with usage scenarios and test expectations

### 3. Implementation Guide (`quickstart.md`)

Phase 1 (MVP): Single counter notifications
- Prerequisites & dependencies: AndroidX Core, WorkManager, Coroutines
- Manifest permissions: POST_NOTIFICATIONS, FOREGROUND_SERVICE
- 5-step implementation:
  1. Counter interface
  2. CounterNotificationManager interface
  3. CounterNotificationManagerImpl (notification creation, updates)
  4. NotificationUpdateWorker (background updates every 1 second)
  5. Integration into TimerViewModel/StopwatchViewModel

Phase 2: Multiple simultaneous counter notifications
- Minimal changes: reuse Phase 1 code
- Unique notification IDs per counter
- Multiple tap handlers

Testing:
- Unit tests: startNotification, updateNotificationTime, stopNotification, error cases
- Integration tests: notification appearance, real-time updates, backgrounded persistence

Validation checklist: 13 items across P1, P2, and general categories

Timeline: P1 (~1 week) + P2 (~4-5 days) + Polish (~3-5 days) = 2-3 weeks

**Status**: ✅ Complete implementation roadmap with code examples and timeline

---

## Constitution Check: Re-evaluation (Post-Design)

✅ **Code Quality Principle**: CONFIRMED
- Notification API usage follows AndroidX compat (NotificationCompat)
- No deprecated APIs used
- Kotlin conventions maintained
- Public API fully documented with KDoc

✅ **Testing Discipline**: CONFIRMED
- Unit tests specified: notification creation, updates, removal
- Integration tests: real-time updates, background persistence, multiple counters
- Target: 60% coverage for CounterNotificationManager + Worker
- Contract includes test expectations

✅ **Simplicity Principle**: CONFIRMED
- Single responsibility: display active counters
- Reuses existing Timer/Stopwatch models (no duplication)
- No new architectural complexity
- CounterNotificationManager is focused interface
- Background updates delegated to WorkManager (system responsibility)

**Status**: ✅ All principles maintained through design phase

---

## Artifacts Generated

Documentation:
- ✅ `research.md` - Research findings and technical decisions
- ✅ `data-model.md` - Entity definitions and relationships
- ✅ `contracts/notification-manager-interface.md` - API specification
- ✅ `quickstart.md` - Implementation guide with code examples

Source code structure (to be created in Phase 2):
```
app/src/main/java/com/yaxer/timetrack/notification/
├── Counter.kt
├── CounterNotificationManager.kt (interface)
├── CounterNotificationManagerImpl.kt
└── NotificationUpdateWorker.kt

app/src/test/java/com/yaxer/timetrack/notification/
├── CounterNotificationManagerTest.kt
└── NotificationUpdateWorkerTest.kt

app/src/androidTest/java/com/yaxer/timetrack/notification/
└── CounterNotificationServiceTest.kt

Modified:
├── TimerViewModel.kt
├── StopwatchViewModel.kt
├── AndroidManifest.xml
└── app/build.gradle.kts
```

---

## Phase 2: Implementation (NOT in this command)

The `/speckit.plan` command is complete. Next step: `/speckit.tasks`

Tasks will be generated to cover:
- P1 (MVP): Single notification implementation + tests
- P2 (Advanced): Multiple simultaneous notifications
- Polish: Edge cases, performance, final QA

**Status**: Ready for `/speckit.tasks` command to generate detailed task list
