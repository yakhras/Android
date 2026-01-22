# Research: Show Active Counters in Notification Shade

**Purpose**: Resolve technical unknowns and validate approach before Phase 1 design
**Date**: 2026-01-19
**Feature**: Show Active Counters in Notification Shade

## Research Questions & Findings

### 1. Android Foreground Notification Implementation

**Question**: What is the best practice for displaying real-time updating notifications on Android?

**Research**: Android documentation and community standards for background execution

**Decision**: Use Android 8.0+ Foreground Service with NotificationCompat
- **Why chosen**: Provides reliable background execution, respects Doze mode, persistent notification channel
- **Alternatives considered**:
  - JobScheduler: Better battery efficiency but less reliable for UI updates
  - AlarmManager: Excessive wakeups, deprecated for frequent tasks
- **Implementation approach**:
  - CounterNotificationService extends Service with notification manager
  - WorkManager for periodic updates (respects Android background limits)
  - Coroutines + LiveData/Flow for state management

**Rationale**: Foreground Service is the standard Android pattern for long-running background tasks that require real-time UI updates. Approved by Google Play Store policies when properly disclosed to users.

---

### 2. Real-Time Notification Updates (1-Second Granularity)

**Question**: How to efficiently update notification text every second without excessive battery drain?

**Research**: Android background execution constraints (Doze, Background Execution Limits)

**Decision**: Use WorkManager + Coroutines with 1-second interval updates
- **Why chosen**:
  - WorkManager batches updates efficiently while respecting system constraints
  - Coroutines provide lightweight threading (no thread per update)
  - 1-second update frequency is sustainable on modern devices
- **Alternatives considered**:
  - Handler.postDelayed: Blocks if app killed
  - Timer: Not lifecycle-aware, inconsistent with Jetpack
- **Implementation approach**:
  - Periodic work request: 1-second interval
  - Flow-based state updates to notification
  - Cancel work when no active counters

**Rationale**: Modern Android requires WorkManager for background tasks. 1-second updates are achievable with proper coroutine scheduling and won't trigger excessive battery drain.

---

### 3. Multiple Simultaneous Notifications

**Question**: Can Android support multiple separate notifications for different counters?

**Research**: Android notification stacking and grouping behavior

**Decision**: Use notification grouping with unique IDs per counter
- **Why chosen**:
  - Each counter gets unique notification ID (hashCode of counter UUID)
  - Optional: Group notifications under parent "TimeTrack" group
  - Users see all active counters as separate items in shade
- **Alternatives considered**:
  - Single notification with multiple lines: User can't interact with individual counters
  - Notification groups only: Still allows individual tap handlers
- **Implementation approach**:
  - Counter ID as notification tag/ID
  - Group summary notification (optional, for clarity)
  - Distinct tap intents per notification

**Rationale**: Android supports unlimited simultaneous notifications. Grouping improves organization but each counter needs tappable action. Typical use case is 2-3 simultaneous counters (rarely >5).

---

### 4. Counter Identification in Notification

**Question**: How to display meaningful counter names when user assigns them?

**Research**: User naming patterns and notification space constraints

**Decision**: Display "Counter Name" + truncate if >30 chars
- **Why chosen**: User-assigned names are intuitive (per spec requirement Q3: B)
- **Alternatives considered**:
  - Index-based ("Timer #1"): Less intuitive, breaks when counter deleted
  - Timestamp-based: Clutters notification
- **Implementation approach**:
  - Fetch counter name from existing Timer/Stopwatch model
  - Add fallback: "Unnamed Timer" or auto-generated based on creation time
  - Store name in counter model (already have unique ID)

**Rationale**: User-assigned names support the desired UX from specification clarification Q3. Names are already stored if timers/stopwatches support naming.

---

### 5. Notification Permissions & API Compatibility

**Question**: How to handle notification permission variations across Android versions?

**Research**: Android 13+ notification permission requirements

**Decision**:
- Request POST_NOTIFICATIONS permission (Android 13+)
- Graceful fallback for older versions
- Prompt user on first counter creation

**Why chosen**:
  - Required by Google Play Store for Android 13+
  - NotificationCompat handles backward compatibility
  - Early permission prompt prevents permission denied scenario

**Alternatives considered**:
  - Request at app startup: Intrusive, poor UX
  - Request only when needed: Spec requires notification immediately on counter start

**Implementation approach**:
  - Add POST_NOTIFICATIONS to AndroidManifest.xml
  - Runtime permission check before creating notification
  - Show rationale dialog if permission denied
  - Continue app function (notification silently skipped if denied)

**Rationale**: Spec assumes notifications available; permission handling is platform requirement, not feature limitation.

---

### 6. Background Execution & Doze Mode

**Question**: Will notifications update when app is backgrounded or device is in Doze mode?

**Research**: Android Doze mode, Background Execution Limits (API 31+)

**Decision**: Use exact alarms + foreground service exemption
- **Why chosen**:
  - Foreground service gets exemption from Doze restrictions
  - Exact alarm API (SCHEDULE_EXACT_ALARM permission) for precise 1-second updates
  - WorkManager automatically optimizes for background limits

**Alternatives considered**:
  - Inexact alarms: Updates may drift >1 second (violates SC-001)
  - Handler only: Stopped if app killed

**Implementation approach**:
  - Add SCHEDULE_EXACT_ALARM permission (non-critical, graceful degradation if denied)
  - Foreground service remains active while counters running
  - WorkManager handles batching for battery efficiency

**Rationale**: Feature requirement (SC-001) specifies 1-second update granularity. This requires precise timing, achievable via exact alarms + foreground service.

---

### 7. Counter Data Model Integration

**Question**: What counter data is needed from existing Timer/Stopwatch models?

**Research**: Existing app architecture for Timer and Stopwatch

**Decision**: Reuse existing Timer/Stopwatch models (no new entities)
- **Why chosen**: Spec states "Timer and stopwatch features already implemented"
- **Required fields from counter**:
  - ID: unique identifier
  - Name: user-assigned name (must be added if not present)
  - IsRunning: boolean status
  - ElapsedTime: current duration in milliseconds
  - Type: TIMER or STOPWATCH enum

**Implementation approach**:
  - Create interface/sealed class for common counter properties
  - Timer and Stopwatch implement this interface
  - NotificationManager queries active counters via this interface
  - No changes to existing Timer/Stopwatch data layer

**Rationale**: Simplicity principle - reuse existing models rather than creating notification-specific duplicates.

---

## Design Decisions Summary

| Decision | Chosen Approach | Key Constraint |
|----------|-----------------|-----------------|
| Background updates | WorkManager + Coroutines | Battery efficiency |
| Notification creation | Foreground Service | Android 8.0+ requirement |
| Multiple counters | Separate notifications with unique IDs | Platform limit ~10 reasonable |
| Counter identity | User-assigned name + type | Spec requirement Q3: B |
| Update frequency | 1-second periodic work | SC-001: within 1 second requirement |
| Permissions | Runtime check + graceful fallback | Android 13+ requirement |
| Data model | Reuse Timer/Stopwatch models | Simplicity, no duplication |
| API level | Android 26+ (API 8.0+) | Existing project target |

---

## Phase 1 Readiness

✅ All research questions resolved
✅ No [NEEDS CLARIFICATION] markers remain
✅ Technical approach validated against:
   - Android platform constraints
   - Constitution principles (testing, simplicity, code quality)
   - Specification requirements (1-second updates, multiple counters)

**Status**: Ready to proceed to Phase 1 (data model, contracts, quickstart)
