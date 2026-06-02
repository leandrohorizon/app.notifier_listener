# NotificationListener

A robust Android application designed to intercept, filter, and manage system notifications. It provides advanced features for silincing unwanted alerts, persisting notification history, and syncing data to a remote server.

## Project Overview

- **Core Functionality**: Intercepts notifications using `NotificationListenerService`, applies filtering logic, and stores them in a local SQLite database.
- **Key Features**:
    - **Notification Persistence**: Stores detailed notification data (title, text, package, metadata).
    - **Advanced Filtering**: Support for "Mute Rules" based on package names and regex keywords.
    - **Presets**: "Saved Filters" allow users to quickly view specific subsets of notifications.
    - **Deduplication**: Prevents spam by ignoring duplicate notifications within a short timeframe.
    - **Remote Sync**: Optionally sends captured notifications to a configured HTTP endpoint.
    - **Reliability**: Uses a Foreground Service and WorkManager (Watchdog) to ensure high availability.
- **Architecture**: Follows MVVM (Model-View-ViewModel) architecture with Jetpack Compose for the UI.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Database**: Room (SQLite)
- **Background Work**: `NotificationListenerService`, `ForegroundService`, `WorkManager`
- **Dependency Management**: Gradle (Version Catalog)
- **Serialization**: Kotlinx Serialization

## Building and Running

### Prerequisites
- Android Studio Ladybug or newer.
- Android SDK (Targeting API 36).
- A physical device or emulator running Android 8.0 (API 26) or higher.

### Commands
- **Build APK**: `./gradlew assembleDebug`
- **Run Unit Tests**: `./gradlew test`
- **Run Instrumented Tests**: `./gradlew connectedAndroidTest`
- **Lint**: `./gradlew lint`

### Installation & Setup
1. Build and install the app on a device.
2. **Mandatory**: Grant **Notification Access** permission to the app in the system settings.
3. (Optional) Configure a **Sync URL** in the settings for remote data export.

## Architecture & Core Components

- **`NotificationListener`**: The heart of the app. Extends `NotificationListenerService`. It handles `onNotificationPosted`, applies `MuteRuleEntity` logic, and persists data via `NotificationDao`.
- **`AppDatabase`**: Room database holding:
    - `NotificationEntity`: Captured notification records.
    - `MuteRuleEntity`: Rules for automatic silincing.
    - `SavedFilterEntity`: User-defined viewing presets.
    - `AppFilterEntity`: Package-level inclusion/exclusion.
- **`NotificationViewModel`**: Manages UI state, exposes data as `Flow`, and handles user actions like deleting, syncing, and updating filters.
- **`MainScreen`**: The primary UI, featuring tabs for Active/Muted notifications and horizontal filter chips for presets.
- **`FileLogger`**: Utility for writing system and processing errors to `app_errors.txt` for debugging.

## Development Conventions

- **UI**: Use Jetpack Compose for all new screens. Follow the existing dark theme defined in `Theme.kt`.
- **Concurrency**: Use Coroutines (`viewModelScope`) and `Flow` for reactive data streams.
- **Data Flow**: Use the Repository pattern (though currently simplified directly in ViewModel) and Room DAOs for data access.
- **Errors**: Log significant failures using `FileLogger.writeError` to ensure they can be diagnosed in production-like environments.
- **Versioning**: Database versioning is managed in `AppDatabase.kt`. Always provide migration paths when modifying entities.
