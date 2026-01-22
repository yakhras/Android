# Feature Specification: Add Time to Entry from Stopwatch

**Feature Branch**: `002-stopwatch-add-time`
**Created**: 2026-01-19
**Status**: Draft
**Input**: User description: "When stopwatch resets, enable user to add the time to entry. If stopwatch contains laps, enable user to assign each lap time duration to an entry."

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

### User Story 1 - Add Stopwatch Time on Reset (Priority: P1)

User runs a stopwatch to track activity duration. When the user presses the reset button, the app shows an interface allowing the user to add the accumulated stopwatch time to a time tracking entry before the timer is cleared.

**Why this priority**: Core functionality - enables users to capture continuous time measurements from stopwatch activity into time entries quickly.

**Independent Test**: Can be fully tested by starting a stopwatch, letting it run for several seconds, pressing reset, and verifying the add-to-entry interface appears with the accumulated time. Delivers immediate value for basic stopwatch-to-entry workflow.

**Acceptance Scenarios**:

1. **Given** a stopwatch has accumulated 5 minutes and 30 seconds, **When** the user presses the reset button, **Then** the app displays an add-to-entry interface with the accumulated time
2. **Given** the add-to-entry interface is displayed for stopwatch time, **When** the user selects an entry, **Then** the time is added to that entry and the stopwatch resets to 00:00
3. **Given** the add-to-entry interface is displayed, **When** the user taps "Cancel" or dismisses, **Then** the stopwatch returns to its current state without adding time

---

### User Story 2 - Assign Individual Lap Times to Entries (Priority: P2)

When a stopwatch contains multiple recorded laps, the user can assign each lap's duration to different time entries, providing granular control over multi-segment time tracking.

**Why this priority**: Enables advanced use cases where users need to track multiple activities within one stopwatch session (e.g., time spent on different tasks in one work session).

**Independent Test**: Can be fully tested by recording multiple laps on the stopwatch, pressing reset, and verifying each lap can be assigned independently to entries. Each lap assignment should work independently without affecting others.

**Acceptance Scenarios**:

1. **Given** a stopwatch has 3 recorded laps (Lap 1: 2m, Lap 2: 3m, Lap 3: 1.5m), **When** user presses reset, **Then** the app displays an add-to-entries interface showing each lap separately
2. **Given** the lap assignment interface is displayed, **When** the user selects different entries for each lap, **Then** each lap's time is added to its assigned entry
3. **Given** lap assignment interface is showing, **When** the user assigns only some laps and skips others, **Then** only assigned laps are added to entries
4. **Given** lap assignment interface shows 3 laps, **When** the user assigns multiple laps to the SAME entry, **Then** all selected laps accumulate in that entry

---

### Edge Cases

- What happens if user presses reset with 0 accumulated time on stopwatch?
- What if stopwatch has laps but one lap shows 0 duration?
- What happens if user exits the lap assignment interface without assigning all laps?
- How should the system handle very long lap durations (e.g., 10+ hours)?
- What if the same stopwatch session contains both a running lap and completed laps when reset is pressed?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST track stopwatch elapsed time with lap capability
- **FR-002**: System MUST record individual lap times with start/stop timestamps
- **FR-003**: System MUST display add-to-entry interface when reset button is pressed
- **FR-004**: System MUST display accumulated stopwatch time in the add-to-entry interface
- **FR-005**: Users MUST be able to add stopwatch total time to a single entry (without laps)
- **FR-006**: When stopwatch contains laps, system MUST display each lap individually in the interface
- **FR-007**: Users MUST be able to assign each lap independently - same entry can receive multiple laps OR each lap can go to a different entry (user chooses per lap)
- **FR-008**: System MUST validate that assigned times are added correctly to entries and persisted
- **FR-009**: System MUST clear stopwatch to 00:00 and reset laps after successful addition to entries
- **FR-010**: Users MUST have option to cancel without adding any time to entries

### Key Entities

- **Stopwatch**: Tracks total elapsed time and individual lap times with timestamps for each lap
- **Lap**: Records individual time segment within a stopwatch session (start time, end time, duration)
- **Time Entry**: Represents a time tracking record that accumulates time from timer or stopwatch additions

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: User can add stopwatch time to entry in under 5 seconds from reset button press
- **SC-002**: When stopwatch contains laps, user can assign all laps to entries in under 10 seconds
- **SC-003**: 85% of users successfully complete lap assignment on first attempt
- **SC-004**: No lap time data is lost - all lap durations are accurately recorded and displayed
- **SC-005**: Assigned times accumulate correctly - sum of multiple lap additions equals original stopwatch total

## Assumptions

- Stopwatch functionality is already implemented (this feature builds on top of it)
- Users have at least one time entry available when adding stopwatch time
- Laps are recorded automatically by the stopwatch feature (not part of this feature)
- User can manually start/stop the stopwatch to create laps
