# TimeTrack - Application State Documentation

> Comprehensive codebase documentation for AI agents and developers
> Generated: 2026-01-27

---

## 1. Executive Summary

### Application Overview
- **Name:** TimeTrack
- **Package/ID:** `com.yaxer.timetrack`
- **Purpose:** Time tracking Android application that integrates with Odoo ERP for managing projects and time entries with offline support
- **Current Version:** 1.0 (versionCode: 1)

### Technology Stack
| Category | Technology | Version |
|----------|------------|---------|
| Language | Kotlin | 1.9.24 |
| Framework | Android SDK | 34 (compile), 26 (min) |
| Build Tool | Gradle | 8.12.0 |
| Database | Room | 2.6.1 |
| Async | Kotlin Coroutines | 1.7.3 |
| Navigation | AndroidX Navigation | 2.7.7 |
| Background Work | WorkManager | 2.9.0 |
| API Protocol | XML-RPC | 3.1.3 |
| JSON | Gson | 2.10.1 |

### Architecture Pattern
**MVVM (Model-View-ViewModel) with Repository Pattern**

The app follows a clean architecture approach with:
- **UI Layer**: Fragments + ViewModels
- **Data Layer**: Repository + Local DB + Remote API
- **Offline-First Design**: Local Room database as single source of truth with background sync

---

## 2. Project Structure

### Directory Tree
```
timetrack/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/yaxer/timetrack/
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/           # Room database
│   │   │   │   │   ├── network/         # Network monitoring
│   │   │   │   │   └── repository/      # Repository layer
│   │   │   │   ├── sync/                # Background sync
│   │   │   │   ├── ui/                  # ViewModels
│   │   │   │   │   ├── entries/
│   │   │   │   │   └── projects/
│   │   │   │   ├── util/                # Utilities
│   │   │   │   └── *.kt                 # Fragments, Adapters, API
│   │   │   └── res/
│   │   │       ├── layout/              # XML layouts
│   │   │       └── navigation/          # Nav graph
│   │   ├── test/                        # Unit tests
│   │   └── androidTest/                 # Instrumented tests
│   └── build.gradle.kts
├── build.gradle.kts                     # Root build config
├── settings.gradle.kts
└── gradle.properties
```

### Key Directories
| Directory | Purpose |
|-----------|---------|
| `app/src/main/java/com/yaxer/timetrack/` | Main application source code |
| `app/src/main/java/.../data/local/` | Room database entities, DAOs, converters |
| `app/src/main/java/.../data/repository/` | Repository pattern implementation |
| `app/src/main/java/.../sync/` | Background sync with WorkManager |
| `app/src/main/java/.../ui/` | ViewModels for MVVM |
| `app/src/main/res/layout/` | XML layout files |
| `app/src/main/res/navigation/` | Navigation graph |
| `app/src/test/` | JUnit unit tests |
| `app/src/androidTest/` | Espresso instrumented tests |

### Important Files
| File | Purpose | Location |
|------|---------|----------|
| TimeTrackApplication.kt | Application class, DI setup | `app/src/main/java/.../TimeTrackApplication.kt:17` |
| HostActivity.kt | Single activity host | `app/src/main/java/.../HostActivity.kt:9` |
| TimeTrackRepository.kt | Data layer coordinator | `app/src/main/java/.../data/repository/TimeTrackRepository.kt:31` |
| TimeTrackDatabase.kt | Room database definition | `app/src/main/java/.../data/local/TimeTrackDatabase.kt:29` |
| OdooApiClient.kt | Odoo XML-RPC API client | `app/src/main/java/.../Odooapiclient.kt:13` |
| nav_graph.xml | Navigation configuration | `app/src/main/res/navigation/nav_graph.xml:1` |

---

## 3. Build Configuration

### Build Tool
- **Tool:** Gradle with Kotlin DSL
- **Version:** 8.12.0 (Android Gradle Plugin)
- **Config File:** `app/build.gradle.kts`

### SDK Configuration
| Setting | Value |
|---------|-------|
| compileSdk | 34 |
| minSdk | 26 |
| targetSdk | 34 |
| Java Version | 17 |

### Dependencies
| Dependency | Version | Purpose |
|------------|---------|---------|
| androidx.core:core-ktx | 1.12.0 | Kotlin extensions for Android |
| androidx.appcompat:appcompat | 1.6.1 | Backward compatibility |
| com.google.android.material:material | 1.11.0 | Material Design components |
| kotlinx-coroutines-android | 1.7.3 | Async programming |
| androidx.lifecycle:lifecycle-viewmodel-ktx | 2.7.0 | ViewModel support |
| androidx.navigation:navigation-fragment-ktx | 2.7.7 | Fragment navigation |
| androidx.work:work-runtime-ktx | 2.9.0 | Background work scheduling |
| androidx.room:room-runtime | 2.6.1 | Local database |
| com.google.code.gson:gson | 2.10.1 | JSON serialization |
| org.apache.xmlrpc:xmlrpc-client | 3.1.3 | Odoo API communication |
| junit:junit | 4.13.2 | Unit testing |
| io.mockk:mockk | 1.13.5 | Mocking for tests |
| app.cash.turbine:turbine | 1.1.0 | Flow testing |
| androidx.test.espresso:espresso-core | 3.5.1 | UI testing |

### Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

---

## 4. Architecture Overview

### Pattern: MVVM + Repository

The application uses MVVM (Model-View-ViewModel) architecture with a Repository pattern for data management. It implements an **offline-first** strategy where local Room database serves as the single source of truth.

### Data Flow Diagram
```
┌─────────────────────────────────────────────────────────────────────────┐
│                              UI LAYER                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │  Stopwatch   │  │    Timer     │  │   Projects   │  │   Entries    │ │
│  │   Fragment   │  │   Fragment   │  │   Fragment   │  │   Fragment   │ │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘ │
│         │                 │                 │                 │         │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐ │
│  │  Stopwatch   │  │    Timer     │  │   Projects   │  │   Entries    │ │
│  │  ViewModel   │  │  ViewModel   │  │  ViewModel   │  │  ViewModel   │ │
│  └──────────────┘  └──────────────┘  └──────┬───────┘  └──────┬───────┘ │
└─────────────────────────────────────────────┼─────────────────┼─────────┘
                                              │                 │
┌─────────────────────────────────────────────▼─────────────────▼─────────┐
│                           DATA LAYER                                     │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    TimeTrackRepository                            │   │
│  │  - Cache-first with background refresh                           │   │
│  │  - Offline operations with sync queue                            │   │
│  └──────────────────────────┬───────────────────────────────────────┘   │
│                             │                                            │
│         ┌───────────────────┼───────────────────┐                       │
│         ▼                   ▼                   ▼                       │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐                 │
│  │    Room DB   │   │  OdooAPI     │   │  SyncQueue   │                 │
│  │  (Local)     │   │  (Remote)    │   │  (Pending)   │                 │
│  └──────────────┘   └──────────────┘   └──────────────┘                 │
└─────────────────────────────────────────────────────────────────────────┘
                                              │
┌─────────────────────────────────────────────▼───────────────────────────┐
│                         BACKGROUND LAYER                                 │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  SyncManager + SyncWorker (WorkManager)                          │   │
│  │  - Periodic sync every 15 minutes                                │   │
│  │  - Immediate sync when network becomes available                 │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### Component Relationships

1. **Application** (`TimeTrackApplication`) initializes:
   - Room Database (singleton)
   - NetworkMonitor (connectivity tracking)
   - Repository (data operations)
   - SyncManager (background sync scheduling)

2. **Fragments** observe:
   - StateFlow from ViewModels
   - Trigger actions through ViewModel methods

3. **ViewModels** interact with:
   - Repository for data operations
   - Expose StateFlow/SharedFlow for UI

4. **Repository** coordinates:
   - Local database (Room DAOs)
   - Remote API (OdooApiClient)
   - Sync queue for offline operations

### Dependency Injection
**Manual DI** using Application class as service locator:
- Database, Repository, NetworkMonitor, SyncManager instantiated in `TimeTrackApplication`
- ViewModels created via `ViewModelFactory` with repository injection
- Location: `app/src/main/java/.../ui/ViewModelFactory.kt`

---

## 5. Features & Screens

### Screen/Feature List
| Screen | Component | Path | Description |
|--------|-----------|------|-------------|
| Stopwatch | StopwatchFragment | `StopwatchFragment.kt:17` | Count-up stopwatch with lap times |
| Timer | TimerFragment | `TimerFragment.kt:27` | Countdown timer with project entry creation |
| Projects | ProjectsFragment | `ProjectsFragment.kt:22` | List and manage projects |
| Entries | EntriesFragment | `EntriesFragment.kt:25` | View and manage time entries |

### Navigation Flow
```
                    ┌─────────────────────┐
                    │    HostActivity     │
                    │   (NavHostFragment) │
                    └──────────┬──────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   Bottom Nav    │  │   Bottom Nav    │  │   Bottom Nav    │
│   Stopwatch     │  │     Timer       │  │    Projects     │
│  (Start Dest)   │  │                 │  │                 │
└─────────────────┘  └─────────────────┘  └────────┬────────┘
                                                   │
                                          ┌────────▼────────┐
                                          │   Bottom Nav    │
                                          │    Entries      │
                                          └─────────────────┘
```

### Feature Details

#### Stopwatch
- **Location:** `StopwatchFragment.kt:17`
- **ViewModel:** `StopwatchViewModel.kt:7`
- **Components:** Timer display, Start/Stop/Lap buttons, Lap times RecyclerView
- **Description:** Standard stopwatch with millisecond precision, lap recording, and pause/resume functionality

#### Timer (Countdown)
- **Location:** `TimerFragment.kt:27`
- **ViewModel:** `TimerViewModel.kt:7`
- **Components:** Hour/Minute/Second pickers, countdown display, project selection dialog
- **Description:** Countdown timer that can create time entries when completed or stopped

#### Projects
- **Location:** `ProjectsFragment.kt:22`
- **ViewModel:** `ProjectsViewModel.kt:21`
- **Components:** Projects RecyclerView, FAB for creating, status indicator
- **Description:** Lists all projects from Odoo with offline support for creation

#### Time Entries
- **Location:** `EntriesFragment.kt:25`
- **ViewModel:** `EntriesViewModel.kt:23`
- **Components:** Entries RecyclerView with timer controls, FAB for creating
- **Description:** Lists time entries with inline timer start/pause/stop functionality

---

## 6. Data Layer

### Database Schema

#### ProjectEntity (projects table)
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | Int | PRIMARY KEY | Odoo project ID (negative for offline) |
| name | String | NOT NULL | Project name |
| code | String? | NULLABLE | Optional project code |
| active | Boolean | NOT NULL | Whether project is active |
| syncStatus | SyncStatus | NOT NULL | SYNCED, PENDING_SYNC, SYNC_FAILED |
| lastSyncedAt | Long | NOT NULL | Timestamp of last sync |

**Location:** `app/src/main/java/.../data/local/Entities.kt:27`

#### TimeEntryEntity (time_entries table)
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | Int | PRIMARY KEY | Odoo entry ID (negative for offline) |
| projectId | Int | NOT NULL | FK to project |
| projectName | String | NOT NULL | Denormalized project name |
| description | String | NOT NULL | Entry description |
| date | String | NOT NULL | Date (YYYY-MM-DD) |
| unitAmount | Float | NOT NULL | Duration in hours |
| startTime | String? | NULLABLE | Start time (YYYY-MM-DD HH:mm:ss) |
| endTime | String? | NULLABLE | End time |
| isRunning | Boolean | NOT NULL | Timer running state |
| syncStatus | SyncStatus | NOT NULL | Sync status |
| localId | String? | NULLABLE | UUID for offline entries |
| lastSyncedAt | Long | NOT NULL | Timestamp |

**Location:** `app/src/main/java/.../data/local/Entities.kt:41`

#### SyncQueueEntity (sync_queue table)
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | Long | PRIMARY KEY, AUTO | Queue item ID |
| entityType | String | NOT NULL | "project", "time_entry", etc. |
| entityId | String | NOT NULL | Entity identifier |
| operation | SyncOperation | NOT NULL | CREATE, UPDATE, DELETE |
| payload | String | NOT NULL | JSON payload |
| createdAt | Long | NOT NULL | Queue timestamp |
| retryCount | Int | NOT NULL | Number of sync attempts |
| lastError | String? | NULLABLE | Last error message |

**Location:** `app/src/main/java/.../data/local/Entities.kt:63`

#### RunningTimerEntity (running_timers table)
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | Int | PRIMARY KEY | Always 1 (single timer) |
| projectId | Int | NOT NULL | Active project |
| projectName | String | NOT NULL | Project name |
| description | String | NOT NULL | Timer description |
| startTime | Long | NOT NULL | Start timestamp |
| elapsedSeconds | Long | NOT NULL | Elapsed time |
| isPaused | Boolean | NOT NULL | Pause state |

**Location:** `app/src/main/java/.../data/local/Entities.kt:79`

### Data Access Objects (DAOs)
| DAO | Entity | Key Methods | Location |
|-----|--------|-------------|----------|
| ProjectDao | ProjectEntity | insert, insertAll, getAll, getById, deleteAll | `Daos.kt:14` |
| TimeEntryDao | TimeEntryEntity | insert, getAll, getByDate, update, delete | `Daos.kt:41` |
| SyncQueueDao | SyncQueueEntity | insert, getAllSync, deleteById, getCount | `Daos.kt:82` |
| RunningTimerDao | RunningTimerEntity | insert, getActive, deleteAll | `Daos.kt:112` |

### Repositories
| Repository | Purpose | Data Sources | Location |
|------------|---------|--------------|----------|
| TimeTrackRepository | Single source of truth | Room DB, OdooApiClient, SyncQueue | `TimeTrackRepository.kt:31` |

**Repository Pattern:**
- **Read**: Returns Flow from local DB (immediate)
- **Refresh**: Fetches from API, updates local DB
- **Write**: Saves locally, attempts API if online, queues if offline

---

## 7. API Integration

### API Client
- **Implementation:** `OdooApiClient` (Singleton object)
- **Location:** `app/src/main/java/.../Odooapiclient.kt:13`
- **Protocol:** XML-RPC over HTTPS

### Endpoints
| Endpoint | Method | Purpose | Parameters |
|----------|--------|---------|------------|
| /xmlrpc/2/common | authenticate | User authentication | db, username, api_key |
| /xmlrpc/2/object | execute_kw | CRUD operations | model, method, args |

### API Operations
| Operation | Model | Method | Description |
|-----------|-------|--------|-------------|
| Fetch Projects | timetrack.project | search_read | Get active projects |
| Create Project | timetrack.project | create | Create new project |
| Fetch Entries | timetrack.entry | search_read | Get time entries |
| Create Entry | timetrack.entry | create | Create time entry |
| Update Entry | timetrack.entry | write | Update entry (timer) |
| Delete Entry | timetrack.entry | unlink | Delete entry |

### Authentication
- **Method:** API Key authentication via XML-RPC
- **Storage:** Hardcoded in `OdooConfig.kt` (should be moved to secure storage)
- **Session:** User ID stored in memory after authentication

### Error Handling
- All API calls wrapped in try-catch
- Returns null/false on failure
- Errors logged via `printStackTrace()`
- UI shows error messages via SharedFlow

---

## 8. State Management

### State Pattern
**StateFlow/SharedFlow** from Kotlin Coroutines for reactive state management:
- `StateFlow` for state that UI observes (projects list, loading state)
- `SharedFlow` for one-time events (errors, navigation)

### State Holders
| Component | State Type | Observables | Location |
|-----------|------------|-------------|----------|
| ProjectsViewModel | ViewModel | projects: StateFlow, isLoading: StateFlow, error: SharedFlow | `ProjectsViewModel.kt:21` |
| EntriesViewModel | ViewModel | entries: StateFlow, projects: StateFlow, isLoading: StateFlow | `EntriesViewModel.kt:23` |
| TimerViewModel | ViewModel | timerState: TimerState, remainingMillis, totalMillis | `TimerViewModel.kt:7` |
| StopwatchViewModel | ViewModel | state: StopwatchState, elapsedTimeMillis, lapTimes | `StopwatchViewModel.kt:7` |

### State Flow
```
┌─────────────────────────────────────────────────────────────┐
│                    ViewModel State                          │
│                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐     │
│  │  StateFlow  │    │  StateFlow  │    │ SharedFlow  │     │
│  │   projects  │    │  isLoading  │    │    error    │     │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘     │
└─────────┼──────────────────┼──────────────────┼─────────────┘
          │                  │                  │
          ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────┐
│                      Fragment (UI)                          │
│                                                             │
│  lifecycleScope.launch {                                    │
│      viewModel.projects.collect { updateList(it) }         │
│      viewModel.isLoading.collect { showProgress(it) }      │
│      viewModel.error.collect { showToast(it) }             │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 9. UI Components

### Adapters/List Components
| Adapter | Data Type | Layout | Location |
|---------|-----------|--------|----------|
| ProjectsAdapter | ProjectEntity | item_project.xml | `ProjectsAdapter.kt` |
| EntriesAdapter | TimeEntryEntity | item_entry.xml | `EntriesAdapter.kt:17` |
| LapTimesAdapter | LapTime | item_lap_time.xml | `LapTimesAdapter.kt` |

### Layouts/Views
| Layout | Purpose | Used By |
|--------|---------|---------|
| activity_host.xml | Main container with NavHost + BottomNav | HostActivity |
| fragment_stopwatch.xml | Stopwatch UI | StopwatchFragment |
| fragment_timer.xml | Timer UI with pickers | TimerFragment |
| fragment_projects.xml | Projects list | ProjectsFragment |
| fragment_entries.xml | Entries list | EntriesFragment |
| item_project.xml | Project list item | ProjectsAdapter |
| item_entry.xml | Entry list item with timer controls | EntriesAdapter |
| item_lap_time.xml | Lap time item | LapTimesAdapter |
| dialog_create_entry.xml | Create entry dialog | EntriesFragment |
| dialog_add_timer_entry.xml | Timer completion dialog | TimerFragment |

### Reusable Components
| Component | Purpose | Location |
|-----------|---------|----------|
| ViewModelFactory | Creates ViewModels with dependencies | `ui/ViewModelFactory.kt` |
| NetworkMonitor | Observes network connectivity | `data/network/NetworkMonitor.kt` |
| DurationFormatter | Formats time durations | `util/DurationFormatter.kt` |
| TimerDurationCalculator | Calculates timer durations | `util/TimerDurationCalculator.kt` |

---

## 10. Configuration

### Environment Configuration
| Setting | Value | Location |
|---------|-------|----------|
| SERVER_URL | https://stage.tvo-oil.com/ | `OdooConfig.kt:8` |
| DATABASE | stagetvo | `OdooConfig.kt:9` |
| USERNAME | admin | `OdooConfig.kt:10` |
| API_KEY | [redacted] | `OdooConfig.kt:11` |

**Note:** Credentials are hardcoded. Should be moved to secure storage or BuildConfig.

### Permissions
| Permission | Purpose |
|------------|---------|
| INTERNET | API communication with Odoo server |
| ACCESS_NETWORK_STATE | Monitor online/offline status |
| POST_NOTIFICATIONS | Show sync/timer notifications |
| FOREGROUND_SERVICE | Background timer operation |

### Feature Flags
| Flag | Default | Description |
|------|---------|-------------|
| android.usesCleartextTraffic | true | Allow HTTP (should be false in production) |

---

## 11. Testing

### Test Structure
| Type | Directory | Framework |
|------|-----------|-----------|
| Unit | app/src/test/java/com/yaxer/timetrack/ | JUnit 4, MockK, Turbine |
| Instrumented | app/src/androidTest/java/com/yaxer/timetrack/ | AndroidX Test, Espresso |

### Test Files
| Test | Tests For | Location |
|------|-----------|----------|
| TimerViewModelTest | TimerViewModel state management | `test/.../timer/TimerViewModelTest.kt` |
| TimerDurationCalculatorTest | Duration calculations | `test/.../timer/TimerDurationCalculatorTest.kt` |
| TimerBoundaryTest | Timer edge cases | `test/.../timer/TimerBoundaryTest.kt` |
| DurationFormatterTest | Duration formatting | `test/.../timer/DurationFormatterTest.kt` |
| OdooApiClientTest | API client functionality | `test/.../api/OdooApiClientTest.kt` |
| TimerFragmentUITest | Timer UI interactions | `androidTest/.../timer/TimerFragmentUITest.kt` |
| TimerStateTransitionUITest | Timer state transitions | `androidTest/.../timer/TimerStateTransitionUITest.kt` |
| AddEntryDialogUITest | Entry creation dialog | `androidTest/.../timer/AddEntryDialogUITest.kt` |

### Running Tests
```bash
# Unit tests
./gradlew test

# Unit tests with coverage
./gradlew testDebugUnitTest

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Single test class
./gradlew test --tests "*.TimerViewModelTest"
```

---

## 12. Known Limitations & Technical Debt

### Security Considerations
- **Hardcoded Credentials:** API key stored in `OdooConfig.kt` - should use secure storage or BuildConfig
- **Cleartext Traffic:** `usesCleartextTraffic=true` in manifest - should be false for production
- **No Certificate Pinning:** XML-RPC client doesn't implement certificate pinning

### Technical Debt
- [ ] Move credentials to secure storage (EncryptedSharedPreferences or BuildConfig)
- [ ] Implement proper error handling with sealed classes
- [ ] Add database migrations (currently uses destructive migration)
- [ ] Add ProGuard rules for release builds
- [ ] Implement proper dependency injection (Hilt/Koin)
- [ ] Add network retry logic with exponential backoff
- [ ] Implement proper logging framework (Timber)

### TODOs in Code
| Location | TODO |
|----------|------|
| OdooConfig.kt | Move credentials to secure storage |
| TimeTrackDatabase.kt:54 | Implement proper migrations |
| OdooApiClient.kt | Add retry logic for network failures |

---

## 13. Quick Reference

### Key File Paths
| Purpose | Path |
|---------|------|
| Entry Point | `app/src/main/java/.../HostActivity.kt` |
| Application | `app/src/main/java/.../TimeTrackApplication.kt` |
| Main Config | `app/build.gradle.kts` |
| Database | `app/src/main/java/.../data/local/TimeTrackDatabase.kt` |
| API Client | `app/src/main/java/.../Odooapiclient.kt` |
| Repository | `app/src/main/java/.../data/repository/TimeTrackRepository.kt` |
| Navigation | `app/src/main/res/navigation/nav_graph.xml` |
| Manifest | `app/src/main/AndroidManifest.xml` |

### Common Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run app
adb shell am start -n com.yaxer.timetrack/.HostActivity

# Run all tests
./gradlew test connectedAndroidTest

# Clean and rebuild
./gradlew clean assembleDebug

# Check dependencies
./gradlew dependencies
```

### Code Conventions
- **Kotlin Style:** Official Kotlin code style (`kotlin.code.style=official`)
- **Package Structure:** Feature-based (`data/`, `ui/`, `sync/`, `util/`)
- **Naming:**
  - Classes: PascalCase (e.g., `TimeTrackRepository`)
  - Functions: camelCase (e.g., `refreshProjects`)
  - Constants: SCREAMING_SNAKE_CASE (e.g., `DATABASE_NAME`)
- **State:** Use StateFlow for observable state, SharedFlow for events
- **Coroutines:** Use `viewModelScope` in ViewModels, `lifecycleScope` in Fragments

---

## Appendix: File Index

### Source Files (28 files)
| File | Purpose | Lines |
|------|---------|-------|
| HostActivity.kt | Main activity | 23 |
| TimeTrackApplication.kt | Application class | 69 |
| TimerFragment.kt | Timer UI | 359 |
| TimerViewModel.kt | Timer state | 13 |
| StopwatchFragment.kt | Stopwatch UI | 192 |
| StopwatchViewModel.kt | Stopwatch state | ~30 |
| ProjectsFragment.kt | Projects UI | 100 |
| EntriesFragment.kt | Entries UI | 180 |
| ProjectsAdapter.kt | Projects list adapter | ~50 |
| EntriesAdapter.kt | Entries list adapter | 244 |
| LapTimesAdapter.kt | Lap times adapter | ~40 |
| OdooApiClient.kt | API client | 573 |
| OdooConfig.kt | API config | 12 |
| TimeTrackRepository.kt | Repository | 554 |
| TimeTrackDatabase.kt | Room database | 59 |
| Entities.kt | Room entities | 90 |
| Daos.kt | Room DAOs | 130 |
| Converters.kt | Room type converters | ~20 |
| NetworkMonitor.kt | Connectivity tracking | ~50 |
| Mappers.kt | Entity mappers | ~40 |
| SyncManager.kt | WorkManager scheduler | 75 |
| SyncWorker.kt | Background sync worker | 32 |
| SyncPayloads.kt | Sync data classes | ~30 |
| ProjectsViewModel.kt | Projects ViewModel | 107 |
| EntriesViewModel.kt | Entries ViewModel | 181 |
| ViewModelFactory.kt | ViewModel factory | ~30 |
| DurationFormatter.kt | Duration utils | ~40 |
| TimerDurationCalculator.kt | Timer calculations | ~30 |

### Configuration Files
| File | Purpose |
|------|---------|
| build.gradle.kts (root) | Root build configuration |
| app/build.gradle.kts | App module configuration |
| settings.gradle.kts | Project settings |
| gradle.properties | Gradle configuration |
| AndroidManifest.xml | App manifest |

---

*Documentation generated by `/app-state` skill*
