# Cnotifi: Advanced Android Notification Engine

Cnotifi is a highly customizable Android application designed to construct, orchestrate, and dispatch local notifications with precise granular control. Utilizing modern Android capabilities, the application offers an extensive toolset for developers and power users to generate tailored notification payloads, persist custom graphic assets, and trigger programmable shortcuts directly from the launcher.

## Architectural Overview

The application is built vertically utilizing modern declarative UI paradigms and reactive state management:

- Language Context: Kotlin strictly enforced for type safety and inter-operations.
- Declarative User Interface: Jetpack Compose with Material Design 3 guidelines for fluid and responsive component rendering.
- Asynchronous Operations: Kotlin Coroutines and StateFlow for non-blocking UI state transformations and concurrent execution pipelines.
- Persistent Storage: Shared Preferences for localized primitive state configurations, bypassing heavy SQLite overhead for simple configurations.
- Media Handling: Coil graphic pipeline integrated for memory-efficient multi-threaded image decoding, resizing, and caching.

## Core Technical Features

### 1. Payload Creation Engine
Cnotifi allows dynamic assembly of notification properties:
- Mutable text allocations for titles and message payload bodies.
- Programmable small icons using predefined scalable vector graphics.
- Support for bitmap decoding to allow customized avatars and assets parsed directly from device storage arrays for integration into the NotificationCompat builder.

### 2. Asset Storage and Processing Pipeline
The application features a built-in content resolver and image modification pipeline:
- Asynchronous File I/O for direct application-bound storage interactions.
- User-supplied images are intercepted via ActivityResultContracts, dynamically partitioned using Matrix-based affine transformations, and stored as lossless Portable Network Graphics (PNG) inside the isolated application context.
- Active cleanup cycle management via application user interface for orphaned asset removal to tightly preserve internal memory allocations.

### 3. Dynamic Launcher Shortcuts
Cnotifi leverages the Android ShortcutManager API for deep integration:
- Constructing Pinned Shortcuts corresponding to saved parameters.
- Foreground intents fired sequentially for seamless background notification deployment without requiring full activity initialization.
- Dynamic Bitmap icons synthesized through the Android Graphics framework to serialize configurations onto the native Android launcher surface.

## Build Requirements

- Minimum Software Development Kit (SDK) Target: API Level 24 minimum.
- Target SDK: Aligned with the latest stable Android iterations to ensure compliance with modern runtime permission mandates regarding notification channels.

## Compilation Instructions

The project strictly uses Gradle build systems structured under Kotlin Domain Specific Language (KTS). Transitive dependency resolutions are synchronized logically through the version catalog.
To compile the system:
1. Ensure the presence of the required Android SDK environments.
2. Synchronize gradle configurations.
3. Establish a connection to an active emulator or physical ADB target.
4. Execute the gradle assemble wrapper task.

## Author Information

Author: xounzii
