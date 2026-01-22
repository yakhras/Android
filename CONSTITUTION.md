# CONSTITUTION.md - TimeTrack Android Application

## Do NOT:
- **OVER EACH CONVERSATION DO NOT EVER GENERATE LONG MESSAGES.**
- **DO NOT IMPLEMENT WITHOUT TESTING.**
## Document Control
- **Created**: January 22, 2026
- **Purpose**: Establish engineering standards, architecture conventions, and feature planning framework
- **Scope**: Applies to all current and future feature development
- **Review Cycle**: Quarterly or when principles change

---

## 1. Project Overview

### Project Identity
- **Name**: TimeTrack
- **Type**: Android Mobile Application
- **Platform**: Android API 26-34 (minimum API 26, target API 34)
- **Language**: Kotlin 1.9+
- **Architecture**: Single Activity (HostActivity) with Fragment-based Navigation
- **Backend**: Odoo ERP system (XML-RPC integration)

### Core Purpose
TimeTrack enables users to accurately track time spent on projects and tasks. Users can:
- Create time tracking projects in Odoo
- Create and manage time entries
- Use built-in Timer (countdown) for scheduled tasks
- Use built-in Stopwatch (elapsed time) for ad-hoc timing
- Record lap times during stopwatch sessions
- Quickly add elapsed time from timers/stopwatches to entries
- View active timers/stopwatches in notification shade

### Current Version
- **Version**: 1.0 (Development)
- **Release Date**: TBD
- **Status**: In Active Development (3 planned features in design phase)

---

## 2. Technical Stack

### Build & Dependency Management
- **Build System**: Gradle 8.2.0
- **Kotlin Version**: 1.9.0
- **Java Target**: Java 11
- **Gradle Wrapper**: Available

### Core Dependencies

#### AndroidX & Framework
- `androidx.core:core-ktx:1.10.1` - Core Kotlin extensions
- `androidx.appcompat:appcompat:1.6.1` - Backward compatibility
- `androidx.activity:activity:1.8.0` - Activity Kotlin extensions
- `androidx.constraintlayout:constraintlayout:2.1.4` - Layout management

#### Navigation & UI
- `androidx.navigation:navigation-fragment-ktx:2.7.5` - Fragment navigation
- `androidx.navigation:navigation-ui-ktx:2.7.5` - Navigation UI helpers
- `com.google.android.material:material:1.10.0` - Material Design components

#### State Management & Lifecycle
- `androidx.lifecycle:lifecycle-runtime-ktx:2.6.2` - Lifecycle utilities
- `androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2` - ViewModel support
- `androidx.fragment:fragment-ktx:1.6.2` - Fragment Kotlin extensions

#### Concurrency
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3` - Coroutines for Android

#### Backend Integration
- `org.apache.xmlrpc:xmlrpc-client:3.1.3` - Odoo XML-RPC API client

#### Testing
- `junit:junit:4.13.2` - Unit testing framework
- `androidx.test.ext:junit:1.1.5` - AndroidX JUnit extension
- `androidx.test.espresso:espresso-core:3.5.1` - UI testing

### Backend Integration
- **Odoo Server**: `https://stage.tvo-oil.com/` (Stage environment)
- **Database**: `stagetvo`
- **API Protocol**: XML-RPC 2.0
- **Authentication**: API Key-based (configured in OdooConfig.kt)
- **Key Entities**: Projects (`timetrack.project`), Entries (`timetrack.entry`)

---

## 3. Architecture & Code Structure

### High-Level Architecture
```
┌─────────────────────────────────────┐
│     HostActivity (Single Entry)     │
│  - Navigation Host Fragment         │
│  - Bottom Navigation               │
└─────────────────┬───────────────────┘
                  │
        ┌─────────┴──────────┐
        │                    │
    ┌───▼────────┐    ┌─────▼────┐
    │ Fragments  │    │ViewModels │
    │ - Timer    │    │ - Timer   │
    │ - Stopwatch│    │ - Stopwatch
    │ - Projects │    │           │
    │ - Entries  │    └─────┬─────┘
    └─────┬──────┘          │
          │         ┌───────▼──────┐
          │         │  Repositories │
          │         │ - Odoo Client │
          │         │ (XML-RPC)    │
          └─────────┴───────────────┘
```

### Directory Structure

```
app/src/main/
├── java/com/yaxer/timetrack/
│   ├── HostActivity.kt              # Single host activity
│   ├── MainActivity.kt              # Legacy (may refactor)
│   │
│   ├── UI Fragments:
│   │   ├── TimerFragment.kt
│   │   ├── StopwatchFragment.kt
│   │   ├── ProjectsFragment.kt
│   │   └── EntriesFragment.kt
│   │
│   ├── ViewModels:
│   │   ├── TimerViewModel.kt
│   │   └── StopwatchViewModel.kt
│   │
│   ├── Adapters:
│   │   ├── ProjectsAdapter.kt
│   │   ├── EntriesAdapter.kt
│   │   └── LapTimesAdapter.kt
│   │
│   ├── Data Models:
│   │   └── LapTime.kt
│   │
│   ├── Backend Integration:
│   │   ├── OdooApiClient.kt         # Singleton API client
│   │   └── OdooConfig.kt            # Configuration
│   │
│   └── (Additional Activities for legacy screens)
│       ├── ProjectsActivity.kt
│       ├── EntriesActivity.kt
│       └── StopwatchActivity.kt
│
└── res/
    ├── layout/
    │   ├── activity_host.xml
    │   ├── fragment_timer.xml
    │   ├── fragment_stopwatch.xml
    │   ├── fragment_projects.xml
    │   ├── fragment_entries.xml
    │   ├── item_project.xml
    │   ├── item_entry.xml
    │   └── item_lap_time.xml
    │
    ├── navigation/
    │   └── nav_graph.xml             # Fragment navigation graph
    │
    └── values/
        └── (strings, colors, dimens, styles)
```

### Key Architectural Patterns

#### 1. Single Activity + Fragment Navigation
- **Pattern**: One HostActivity hosts NavHostFragment
- **Navigation**: Bottom Navigation drives Fragment replacement
- **Benefits**: Shared state across fragments, cleaner lifecycle management
- **Reference**: `HostActivity.kt` uses AndroidX Navigation

#### 2. ViewModel-Based State Management
- **Pattern**: Each major feature has a ViewModel (TimerViewModel, StopwatchViewModel)
- **Scope**: Fragment-scoped ViewModels survive configuration changes
- **Usage**: Fragment.viewModels() delegate for lifecycle-aware instantiation
- **Persistence**: Minimal persistence (most state in-memory or in fragments)

#### 3. Coroutine-Based Async Operations
- **Pattern**: Suspend functions in repository layer, launched in lifecycleScope
- **Context**: Dispatchers.IO for network calls, Dispatchers.Main for UI
- **Benefits**: Non-blocking UI, proper cancellation on lifecycle events

#### 4. Singleton API Client
- **Pattern**: OdooApiClient is a Kotlin object (singleton)
- **Authentication**: Stores userId after successful authenticate()
- **Methods**: Async functions for projects, entries, create operations
- **Error Handling**: Try-catch with logging, returns empty/null on failure

#### 5. Adapter Pattern for Lists
- **Pattern**: RecyclerView.Adapter implementations (ProjectsAdapter, EntriesAdapter, etc.)
- **Data**: Receive immutable lists in constructor
- **Callbacks**: Optional lambdas for user interactions
- **Limitation**: Current adapters don't implement DiffUtil (opportunity for optimization)

---

## 4. Implemented Features

### Feature 1: Timer (Countdown Timer)
**Status**: ✅ Implemented

**Functionality**:
- User selects hours, minutes, seconds via NumberPickers
- User taps "Start" to begin countdown
- Progress bar shows visual progress
- Display updates every second (MM:SS format)
- User can Pause/Resume during countdown
- User can Stop to cancel
- Timer survives activity recreation (state in ViewModel)
- **TODO**: Add-to-Entry flow (Spec 001 in progress)
- **TODO**: Notifications (Spec 003 in progress)

**Implementation Details**:
- `TimerFragment.kt` - UI with NumberPickers and buttons
- `TimerViewModel.kt` - State container (timerState, remainingMillis, totalMillis)
- `CountDownTimer` - Android system timer for countdown
- `SystemClock.elapsedRealtime()` - Accurate time tracking across pauses

**Testing**: Manual testing only; automated tests planned for add-to-entry feature

---

### Feature 2: Stopwatch (Elapsed Time Tracker)
**Status**: ✅ Implemented

**Functionality**:
- User taps "Start" to begin time accumulation
- Display shows elapsed time (MM:SS.ms format)
- User can record lap times (captures lap duration and total)
- Lap list shows all recorded laps with durations
- User can Pause/Resume
- User can Reset (clears elapsed time and laps)
- Stopwatch state persists across activity recreation
- **TODO**: Add-to-Entry flow with lap assignment (Spec 002 in progress)
- **TODO**: Notifications (Spec 003 in progress)

**Implementation Details**:
- `StopwatchFragment.kt` - UI with start/stop, lap, and reset buttons
- `StopwatchViewModel.kt` - State container (state, startTimeMillis, elapsedTimeMillis, lapTimes[])
- `LapTime.kt` - Data class: (lapNumber, lapDuration, totalTime)
- `LapTimesAdapter.kt` - RecyclerView adapter for lap list
- `Handler + Looper` - Timer callback every 10ms for millisecond precision

**Testing**: Manual testing only; automated tests planned for add-to-entry feature

---

### Feature 3: Projects Management
**Status**: ✅ Implemented (Read-Only MVP)

**Functionality**:
- User navigates to Projects tab
- App fetches active projects from Odoo
- List shows project name and ID
- User can tap "+" FAB to create new project
- New project is saved to Odoo immediately
- List refreshes after creation
- **TODO**: Edit/Delete operations

**Implementation Details**:
- `ProjectsFragment.kt` - UI with RecyclerView and FAB
- `ProjectsAdapter.kt` - Displays project list
- `OdooApiClient.fetchProjects()` - Fetches from Odoo
- `OdooApiClient.createProject(name)` - Creates new project via XML-RPC

**Dependencies**: 
- Odoo must be authenticated (OdooApiClient.authenticate() called first)
- Network connectivity required

---

### Feature 4: Entries Management
**Status**: ✅ Implemented (Read-Only MVP)

**Functionality**:
- User navigates to Entries tab
- App fetches time entries from Odoo (optionally filtered by date)
- List shows entry details (date, project, start/end time, duration, description)
- Each entry has inline timer controls (Start/Pause/Resume/Stop)
- User can add new entry via FAB
- **TODO**: Edit/Delete operations
- **TODO**: Add time from Timer/Stopwatch (Specs 001-002)

**Implementation Details**:
- `EntriesFragment.kt` - UI with RecyclerView and FAB
- `EntriesAdapter.kt` - Displays entries with inline timer controls
- `OdooApiClient.fetchTimeEntries()` - Fetches from Odoo (today only or all)
- Entry creation UI (dialog with form fields)

**Inline Timer**: Each entry has a timer that counts UP (elapsed time) when Start is pressed
- Separate from main Stopwatch feature
- Stores elapsed time in entry object

---

### Feature 5: Odoo Backend Integration
**Status**: ✅ Implemented

**Functionality**:
- Authenticate with Odoo server using credentials
- Fetch projects list
- Fetch time entries list
- Create new projects
- Create new time entries
- Update entry duration (ready for add-to-entry features)

**Implementation Details**:
- `OdooApiClient.kt` - Singleton Kotlin object with suspend functions
- `OdooConfig.kt` - Centralized configuration (SERVER_URL, DATABASE, USERNAME, API_KEY)
- XML-RPC Protocol: Apache xmlrpc-client library
- Error Handling: Try-catch blocks, null/empty returns on failure

**Authentication Flow**:
```kotlin
// Authenticate once, store userId
val userId = OdooApiClient.authenticate()

// Use for all subsequent calls
OdooApiClient.fetchProjects()  // Uses stored userId
```

**Constraints**:
- Credentials hard-coded in OdooConfig.kt (TODO: Move to secure storage)
- No token refresh (session dies if app killed)
- No retry logic (single attempt on failure)

---

## 5. Planned Features (In Design Phase)

### Feature Spec 001: Add Elapsed Time to Entry from Timer
**Status**: Design Phase (Research Complete, Design Complete)
**Target Branch**: `001-timer-add-time`
**Priority**: P1

**Summary**: When timer completes or user stops it, show dialog to add elapsed time to a time tracking entry. Support creating new entry on-the-fly if none exist.

**Planning Phase Status**:
- ✅ Research: Technical decisions made
- ✅ Data Model: Entities defined
- ✅ Contracts: UI and service interfaces specified
- ✅ Quickstart: Implementation guide created
- ⏳ Implementation: Ready to start

**Key Files** (to be created):
- `AddTimerTimeFragment.kt` - Main UI
- `AddTimerTimeViewModel.kt` - Business logic
- `EntrySelectionAdapter.kt` - Entry list

---

### Feature Spec 002: Add Time to Entry from Stopwatch (with Laps)
**Status**: Design Phase (Research Complete, Design Complete)
**Target Branch**: `002-stopwatch-add-time`
**Priority**: P1 (MVP), P2 (advanced lap assignment)

**Summary**: When stopwatch resets, offer to add elapsed time to entry. If stopwatch contains laps, allow user to assign each lap to different (or same) entries for granular tracking.

**Planning Phase Status**:
- ✅ Research: Technical decisions made
- ✅ Data Model: Entities and relationships defined
- ✅ Contracts: Service interfaces specified
- ✅ Quickstart: Implementation guide created
- ⏳ Implementation: Ready to start

**Key Files** (to be created):
- `AddStopwatchTimeFragment.kt` - Main UI
- `AddStopwatchTimeViewModel.kt` - Business logic
- `LapAssignmentAdapter.kt` - Lap selection UI

---

### Feature Spec 003: Show Active Counters in Notification Shade
**Status**: Design Phase (Research Complete, Design Complete)
**Target Branch**: `003-notification-counters`
**Priority**: P1 (single counter), P2 (multiple counters)

**Summary**: Display active timers/stopwatches in system notification shade with real-time elapsed time updates. Keep users aware of running timers when app is not visible.

**Planning Phase Status**:
- ✅ Research: Technical decisions made
- ✅ Data Model: Notification structure defined
- ✅ Contracts: Service interfaces specified
- ✅ Quickstart: Implementation guide created
- ⏳ Implementation: Ready to start

**Key Files** (to be created):
- `NotificationManager.kt` - Notification lifecycle
- `TimerNotificationService.kt` - Background service for updates
- Manifest permissions: POST_NOTIFICATIONS (API 33+)

---

## 6. Design Principles & Engineering Standards

### Principle 1: Code Quality
**Statement**: All code MUST follow Android framework patterns and Kotlin language conventions.

**Standards**:
1. **Architecture Patterns**:
   - Use ViewModel for state management (Fragment-scoped with viewModels() delegate)
   - Use Repository pattern for data access (abstract from UI)
   - Use Fragments for reusable UI components (not Activities)
   - Use Data classes for model entities with immutability
   - Use sealed classes for state representations

2. **API Design**:
   - All public functions MUST have KDoc comments
   - Suspend functions for async operations (not Callbacks)
   - Avoid null returns; use sealed Result<T> or Optional<T> patterns
   - No raw types; use generics consistently

3. **No Deprecated APIs**:
   - AndroidX recommended APIs only (no android.app.Fragment)
   - No LiveData where Flow is sufficient (new code uses Flow)
   - No RxJava (Coroutines is primary async library)
   - API level checks for API 33+ features

4. **Code Style**:
   - Kotlin Official style guide
   - CamelCase for variables/functions
   - UPPER_SNAKE_CASE for constants
   - One class per file (except sealed class variants)
   - Max line length: 120 characters

**Enforcement**:
- Code review checklist includes architecture pattern review
- Lint checks enabled (Material Design warnings, deprecation warnings)
- IDE inspections enabled in Android Studio

**Example - Good**:
```kotlin
class MyViewModel : ViewModel() {
    private val _state = MutableStateFlow<MyState>(MyState.Idle)
    val state: StateFlow<MyState> = _state.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            try {
                val data = repository.fetch()
                _state.value = MyState.Loaded(data)
            } catch (e: Exception) {
                _state.value = MyState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

---

### Principle 2: Testing Discipline
**Statement**: Feature logic MUST be tested. Target minimum 60% code coverage for business logic.

**Standards**:
1. **Unit Tests** (JUnit 4 + Mockito):
   - Test all ViewModel public methods
   - Test repository operations (mocked API client)
   - Test data transformations and calculations
   - Mock external dependencies (OdooApiClient, databases)

2. **UI Tests** (Espresso):
   - Test critical user journeys (happy path)
   - Test error states and user feedback
   - Use IdlingResource for async operation synchronization
   - No network calls in UI tests (mocked)

3. **Integration Tests** (optional):
   - Test Fragment + ViewModel interaction
   - Test navigation flows
   - Test end-to-end scenarios per spec

4. **Coverage Goals**:
   - Business logic (ViewModels, Repositories): 60%+
   - UI layer (Fragments, Adapters): 30%+
   - Backend integration (API Client): Mock-tested, minimal direct tests

5. **Test Organization**:
   - Unit tests: `app/src/test/java/com/yaxer/timetrack/`
   - UI tests: `app/src/androidTest/java/com/yaxer/timetrack/`
   - Test names: `{Class}{Method}{Scenario}Test`
   - Example: `TimerViewModelStartTimerTest.kt`

**Enforcement**:
- Pull request requires test changes for new features
- Code review includes test coverage assessment
- CI/CD pipeline runs tests on every commit (future)

**Example - Good Unit Test**:
```kotlin
class TimerViewModelStartTest {
    private val viewModel = TimerViewModel()

    @Test
    fun `startTimer sets state to RUNNING`() {
        viewModel.startTimer(hours = 1, minutes = 30, seconds = 45)
        
        assertEquals(TimerState.RUNNING, viewModel.timerState)
        assertEquals(5400000 + 1745000, viewModel.totalMillis) // 1:30:45 in ms
    }

    @Test
    fun `startTimer with zero duration throws error`() {
        assertThrows<IllegalArgumentException> {
            viewModel.startTimer(0, 0, 0)
        }
    }
}
```

---

### Principle 3: Simplicity
**Statement**: Favor simplicity over flexibility. Reuse existing patterns, models, and infrastructure. Avoid premature generalization.

**Standards**:
1. **Model Reuse**:
   - Extend existing data classes rather than creating new ones when possible
   - Do NOT create new database tables without architect review
   - Share ViewModels between related features if sensible

2. **Feature Scope**:
   - Each feature should have a single primary responsibility
   - Complex features broken into independent P1/P2 phases
   - Defer "nice-to-have" to P2 or later if MVP works without it

3. **Code Organization**:
   - No "utils" package; place helpers near their consumers
   - No god classes (if >500 lines, split responsibility)
   - No deep nesting (max 3 levels of indentation)
   - Avoid generic base classes unless used by 3+ classes

4. **Dependency Injection**:
   - Constructor injection preferred
   - No service locator pattern
   - Hilt planned for future (not yet in scope)

5. **UI Complexity**:
   - Use standard components (Fragment, RecyclerView, Dialog)
   - Avoid custom drawing unless necessary
   - Compose planned for future UX improvements

**Enforcement**:
- Code review challenges complex solutions; require justification
- Architecture review for new tables/models
- Complexity metrics checked (McCabe complexity < 10 per function)

**Example - Simple Solution**:
```kotlin
// Good: Direct, reuses existing data
data class TimerAddTimeState(
    val timer: Timer,
    val entries: List<TimeEntry>,
    val selectedEntry: TimeEntry? = null,
    val isLoading: Boolean = false
)

// Bad: Over-abstracted, creates new models
sealed class TimerAddTimeViewState {
    object Loading : TimerAddTimeViewState()
    data class Content(val vm: TimerAddTimeViewModel, ...) : ...
    // etc - unnecessary indirection
}
```

---

## 7. Feature Planning Framework

### Overview
All features follow a structured 3-phase approach:

### Phase 0: Requirements Gathering (Planning)
**Deliverables**: Feature specification document

**Activities**:
- Gather user stories and acceptance criteria
- Define MVP (P1) vs advanced features (P2+)
- Identify dependencies on existing features
- Document success metrics

**Outputs**:
- `specs/{NUMBER}-{feature-slug}/spec.md`

### Phase 1: Design & Research
**Deliverables**: Research findings, data model, contracts, quickstart guide

**Activities**:

1. **Research** (`research.md`):
   - Answer technical unknowns
   - Validate approach with prototypes
   - Document design decisions
   - Example: "Should we use RecyclerView or ListView for lap list?" → Decided: RecyclerView

2. **Data Model** (`data-model.md`):
   - Define/extend entities
   - Document relationships
   - Specify validation rules
   - Identify persistence needs

3. **Contracts** (`contracts/{feature}.md`):
   - Define ViewModel/Service public APIs
   - Specify state transitions
   - Document error scenarios

4. **Quickstart** (`quickstart.md`):
   - Step-by-step implementation roadmap
   - Code snippets (reference)
   - Testing approach

**Outputs**:
- `specs/{NUMBER}-{feature-slug}/research.md`
- `specs/{NUMBER}-{feature-slug}/data-model.md`
- `specs/{NUMBER}-{feature-slug}/contracts/*.md`
- `specs/{NUMBER}-{feature-slug}/quickstart.md`
- `specs/{NUMBER}-{feature-slug}/plan.md` (updated with design decisions)

**Exit Criteria**:
- ✅ All technical decisions documented and reviewed
- ✅ Data model reviewed and approved
- ✅ API contracts reviewed and approved
- ✅ Implementation roadmap is clear
- ✅ No unresolved architectural questions

### Phase 2: Implementation
**Deliverables**: Source code, tests, documentation updates

**Activities**:
- Create feature branch from latest main
- Implement per quickstart roadmap
- Write unit + UI tests (target 60% coverage)
- Document public APIs with KDoc
- Update spec plan.md with implementation status

**Testing Requirements**:
- All unit tests pass locally
- All UI tests pass on emulator/device
- Manual testing of acceptance scenarios from spec
- Code review by second engineer

**Outputs**:
- Feature branch with commits
- Pull request with test evidence
- Updated documentation

**Exit Criteria**:
- ✅ All acceptance scenarios pass
- ✅ Tests written and passing (60%+ coverage for business logic)
- ✅ Code follows style/architecture standards
- ✅ KDoc on all public APIs
- ✅ Feature flag / manual toggle ready
- ✅ Merge to main branch

### Phase 3: Release & Monitoring
**Deliverables**: Merged code, release notes, monitoring dashboard

**Activities**:
- Merge to main branch
- Create release notes
- Tag release version
- Monitor crash logs and usage metrics
- Iterate on feedback

**Outputs**:
- Released APK/AAB
- Release notes
- Analytics dashboard
- Known issues list

---

## 8. Dependency Management

### Adding New Dependencies

**Process**:
1. Identify library need and research alternatives
2. Add to `app/build.gradle.kts` in appropriate section
3. Document in this CONSTITUTION under Technical Stack section
4. Run `./gradlew build` to verify no conflicts
5. Update this CONSTITUTION file

**Guidelines**:
- Prefer AndroidX / JetBrains maintained libraries
- Minimize external dependencies (smaller APK)
- Use version alignment; avoid mixing major versions
- Document why library was chosen

**Forbidden Libraries** (unless approved):
- RxJava (use Coroutines instead)
- Retrofit (currently using XML-RPC; standardize if adding REST)
- Traditional LiveData in new code (use Flow)
- Apache Commons Lang (reinvent simple functions)

---

## 9. Configuration & Secrets Management

### Development Configuration
- **Build Config**: `app/build.gradle.kts`
- **Gradle Properties**: `gradle.properties`
- **Local Properties**: `local.properties` (git-ignored, environment-specific)

### Secrets & Credentials
**Current Status**: ⚠️ Hard-coded in OdooConfig.kt (NOT SECURE)

**OdooConfig.kt**:
```kotlin
object OdooConfig {
    const val SERVER_URL = "https://stage.tvo-oil.com/"
    const val DATABASE = "stagetvo"
    const val USERNAME = "admin"
    const val API_KEY = "aa50268024ee322258a299a8f389f2fd1c3bfdf1"
}
```

**Future Improvement**:
- Move to `local.properties` (git-ignored)
- Use BuildConfig fields injected at compile time
- Encrypt at rest on device (EncryptedSharedPreferences)
- Different credentials for dev/stage/prod builds

**Immediate Action**:
- Do NOT commit API_KEY changes to version control
- Rotate API_KEY regularly
- Use separate dev credentials if possible

---

## 10. Manifest & Permissions

### Current Permissions
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### Planned Permissions (for Spec 003 - Notifications)
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### Application Structure
- **Main Activity**: `HostActivity` (android:exported="true", MAIN/LAUNCHER)
- **Target API**: 34
- **Min API**: 26
- **Backup**: Enabled (uses `data_extraction_rules.xml`)

---

## 11. Build Variants & Configuration

### Current Build Types
- **Debug**: Development build, no optimizations
- **Release**: Production build, ProGuard enabled

### ProGuard Configuration
- Rules in `app/proguard-rules.pro`
- Keep Odoo API client classes (unobfuscated for XML-RPC)
- Shrinking enabled for Release builds

### Future Build Variants
- **dev**: Debug variant, uses dev Odoo instance
- **staging**: Release-like, uses stage Odoo instance
- **production**: Release variant, uses production Odoo instance

---

## 12. Documentation Standards

### Code Documentation
1. **KDoc Comments** (all public APIs):
   ```kotlin
   /**
    * Authenticate with Odoo server.
    *
    * @return User ID if successful, null if authentication failed
    * @throws ConnectionException if server unreachable
    */
   suspend fun authenticate(): Int?
   ```

2. **Inline Comments**:
   - Explain "why" not "what"
   - Use sparingly; code should be self-documenting

3. **TODO Comments**:
   - Format: `// TODO(feature-number): Description`
   - Example: `// TODO(001-timer-add-time): Add time to entry`

### Feature Documentation
1. **Specification** (`spec.md`):
   - User stories with acceptance criteria
   - Success metrics
   - MVP vs advanced features

2. **Research** (`research.md`):
   - Technical decisions and rationale
   - Alternatives considered
   - Dependencies identified

3. **Data Model** (`data-model.md`):
   - Entity definitions
   - Relationships
   - Validation rules

4. **Implementation Plan** (`plan.md`):
   - Architecture overview
   - Complexity analysis
   - Phase breakdown

5. **Quickstart** (`quickstart.md`):
   - Step-by-step implementation guide
   - Code snippets
   - Testing approach

---

## 13. Git & Version Control

### Branch Naming
- Main branch: `main` (production-ready)
- Feature branches: `{spec-number}-{feature-slug}`
  - Example: `001-timer-add-time`, `002-stopwatch-add-time`
- Hotfix branches: `hotfix/{issue-number}`

### Commit Message Format
- Format: `{Type}: {Description}`
- Types: `feat`, `fix`, `test`, `docs`, `refactor`, `chore`
- Example: `feat(001): Add timer completion detection`

### Pull Request Process
1. Create feature branch from `main`
2. Implement feature following design phase documents
3. Write tests (target 60% coverage)
4. Create PR with description referencing spec
5. Code review + approval
6. Merge to `main` with squash option (if preferred)

---

## 14. Quality Assurance & Testing

### Testing Pyramid
```
        /\
       /  \    UI Tests (Espresso)
      /____\   - Critical user journeys
     /      \  - ~20% of tests
    /________\

    /        \
   /          \  Integration Tests
  /____________\ - Feature + ViewModel
 /              \ - ~20% of tests
/________________\

/                  \
/                    \  Unit Tests (JUnit + Mockito)
/____________________\   - Business logic, calculations
- ~60% of tests
```

### Testing Checklist
- [ ] All unit tests pass: `./gradlew test`
- [ ] All UI tests pass: `./gradlew connectedAndroidTest`
- [ ] Code coverage ≥ 60% for business logic
- [ ] Manual testing of spec acceptance scenarios
- [ ] No lint warnings (warnings treated as errors)
- [ ] No deprecated API usage
- [ ] KDoc on all public APIs

### Test Environment
- **Target Emulator**: Android API 34, 1080x1920 (Pixel)
- **Physical Device** (optional): Any API 26+

---

## 15. Known Limitations & Technical Debt

### Current Limitations
1. **Hard-coded Credentials**: OdooConfig.kt contains unencrypted API key
2. **No Token Refresh**: Session expires if app is backgrounded too long
3. **No Retry Logic**: API calls fail immediately on network error
4. **No Offline Support**: All features require live internet
5. **No Database Caching**: Projects/Entries fetched fresh each time
6. **No Analytics**: No crash reporting or usage metrics
7. **Adapter Optimization**: RecyclerView adapters don't use DiffUtil
8. **Legacy Activities**: ProjectsActivity, EntriesActivity, StopwatchActivity exist alongside Fragments

### Planned Improvements
- **Phase 1.5** (post-003): Migrate legacy activities to fragments
- **Phase 2**: Add Room database for local caching
- **Phase 3**: Implement Firebase Crashlytics for monitoring
- **Phase 4**: Add Hilt dependency injection
- **Phase 5**: Migrate UI to Jetpack Compose
- **Phase 6**: Add offline-first sync capability

---

## 16. Update History

| Date | Author | Changes |
|------|--------|---------|
| 2026-01-22 | GitHub Copilot | Initial Constitution created; documented current architecture, technical stack, design principles, and feature planning framework |

---

## 17. Contact & Governance

### Maintainers
- **Project Owner**: Yaser (Yaxer)
- **Architecture Review**: (to be assigned)

### Document Review Cycle
- **Frequency**: Quarterly or when principles change
- **Last Reviewed**: 2026-01-22
- **Next Review**: 2026-04-22

### How to Update This Document
1. Create branch: `docs/constitution-update-{date}`
2. Make changes to `CONSTITUTION.md`
3. Create PR with detailed description
4. Request review from architecture team
5. Merge after approval

---

## Appendix A: Feature Numbering & Naming

Features are numbered sequentially starting from 001. Format: `{NUMBER}-{slug}`

**Current Features**:
- `001-timer-add-time`: Add time from Timer to Entry
- `002-stopwatch-add-time`: Add time from Stopwatch to Entry (with lap support)
- `003-notification-counters`: Show active timers/stopwatches in notification shade

**Next Features** (Proposed, not yet in design):
- `004-offline-sync`: Local database sync with Odoo
- `005-multi-project-stats`: Dashboard with project summaries
- `006-voice-tracking`: Voice input for quick time entry
- (Future additions)

---

## Appendix B: Architecture Decision Record (ADR)

### ADR-001: Single Activity with Fragments
- **Decision**: Use one HostActivity with Fragment-based navigation
- **Date**: Pre-project (current architecture)
- **Reasoning**: Simpler lifecycle management, shared state, standard Android pattern
- **Status**: ✅ Approved

### ADR-002: ViewModel for State Management
- **Decision**: Use Fragment-scoped ViewModels instead of retained fragments
- **Date**: Project inception
- **Reasoning**: Better lifecycle integration, type-safe, official recommendation
- **Status**: ✅ Approved

### ADR-003: Coroutines over RxJava
- **Decision**: Use Kotlin Coroutines for all async operations
- **Date**: Project inception
- **Reasoning**: First-class language support, simpler syntax, no additional dependency
- **Status**: ✅ Approved

### ADR-004: XML-RPC for Odoo Integration
- **Decision**: Use Apache XML-RPC client for Odoo API
- **Date**: Project inception
- **Reasoning**: Odoo standard protocol, no REST API in legacy systems
- **Status**: ⚠️ Temporary (consider REST API if available in Odoo 17+)

---

**End of CONSTITUTION.md**
