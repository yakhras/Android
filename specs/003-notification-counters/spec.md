# Feature Specification: Show Active Counters in Notification Shade

**Feature Branch**: `003-notification-counters`
**Created**: 2026-01-19
**Status**: Draft
**Input**: User description: "Add all active counters (from timer or stopwatch) to be visible in the notification shade."

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories should be PRIORITIZED as user journeys ordered by importance.
  Each user story/journey must be INDEPENDENTLY TESTABLE - meaning if you implement just ONE of them,
  you should still have a viable MVP (Minimum Viable Product) that delivers value.
  
  Assign priorities (P1, P2, P3, etc.) to each story, where P1 is the most critical.
  Think of each story as a standalone slice of functionality that can be:
  - Developed independently
  - Tested independently
  - Deployed independently
  - Demonstrated to users independently
-->

### User Story 1 - Show Single Active Counter in Notification (Priority: P1)

When a user has an active timer or stopwatch running, a notification appears in the notification shade displaying the current elapsed time that updates in real-time. This keeps the user informed of running timers even when the app is not actively visible.

**Why this priority**: Essential UX feature - allows users to see their active timer/stopwatch while using other apps, enabling better time awareness without constantly switching back to the TimeTrack app.

**Independent Test**: Can be fully tested by starting a timer or stopwatch, pulling down the notification shade, and verifying the notification appears with the current time and updates every second. Delivers value immediately for single counter scenario.

**Acceptance Scenarios**:

1. **Given** a timer is running for 2 minutes, **When** the user pulls down the notification shade, **Then** a notification is displayed showing the timer with current elapsed time
2. **Given** a timer notification is visible, **When** 5 seconds pass, **Then** the notification updates to show the new elapsed time
3. **Given** a timer is running, **When** the user taps the notification, **Then** the app opens and shows the active timer
4. **Given** a timer notification is displayed, **When** the timer completes or is stopped, **Then** the notification is removed or updated accordingly

---

### User Story 2 - Show Multiple Active Counters in Notification (Priority: P2)

When multiple timers or stopwatches are running simultaneously, the notification shade displays all active counters, each with their current elapsed time and identification (to distinguish which counter is which).

**Why this priority**: Supports advanced use cases where users run multiple timers concurrently for different tasks. Enables users to track multiple simultaneous activities without switching between app windows.

**Independent Test**: Can be fully tested by starting multiple timers/stopwatches, pulling down notification shade, and verifying all counters appear separately with correct times. Each counter works independently and can be tapped to control individually.

**Acceptance Scenarios**:

1. **Given** 2 timers are running with user-assigned names ("Lunch Break" and "Code Review"), **When** the user pulls down notification shade, **Then** both notifications display with their names and current elapsed times
2. **Given** multiple counter notifications are visible, **When** the user taps the "Code Review" notification, **Then** only that specific counter is brought to focus
3. **Given** 3 active counters exist, **When** one completes, **Then** only the completed counter is removed while others continue to show
4. **Given** timer has no user-assigned name, **When** timer is created, **Then** system prompts user to assign a name OR provides a default name for notification display

---

### Edge Cases

- What happens if app crashes while notification is visible? Does notification remain?
- How does the system handle the notification when stopwatch contains laps?
- What if user has notifications disabled or notification permission denied?
- If a counter duration exceeds 24 hours, how is it displayed in the notification?
- User-assigned names disambiguate counters in the notification shade (user responsibility to choose unique names)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST create a foreground notification when a timer or stopwatch starts running
- **FR-002**: System MUST display the current elapsed time in the notification and update it in real-time (minimum every 1 second)
- **FR-003**: System MUST include a clear identifier/name for each active counter in the notification
- **FR-004**: System MUST display ALL currently active counters (whether timer or stopwatch)
- **FR-005**: Tapping a counter notification MUST open the app and display that specific counter
- **FR-006**: System MUST remove the notification when the last active counter is stopped or completed
- **FR-007**: System MUST handle multiple simultaneous notifications (one per active counter)
- **FR-008**: Notifications MUST remain visible even when app is backgrounded or minimized
- **FR-009**: System MUST respect user's notification permissions and settings
- **FR-010**: Counter identification in notification MUST show: user-assigned name + counter type (e.g., "Break Timer", "Code Review Stopwatch"). If no name assigned, system SHOULD provide a default name or prompt user to name the counter

### Key Entities

- **Counter Notification**: Displays active timer/stopwatch with elapsed time, identifier, and action buttons
- **Active Counter**: Timer or Stopwatch currently in running state that requires notification display

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Notification updates show current elapsed time within 1 second of actual time (updates at least every second)
- **SC-002**: Multiple active counters each display in the notification shade without overlap or confusion (user can distinguish which is which)
- **SC-003**: 95% of users can identify and tap the correct counter notification to control a specific timer/stopwatch
- **SC-004**: Notification persists accurately while app is backgrounded - no loss of time tracking data
- **SC-005**: All active counters display in notification shade within 2 seconds of counter start

## Assumptions

- Foreground notifications are permitted by Android OS (required for background time tracking)
- User's Android version supports modern notification APIs
- Users have granted notification permissions to the app (or prompted on first counter creation)
- Each counter has a unique identifier that can be displayed in notifications
- Timer and stopwatch features are already implemented and running when this feature is added
