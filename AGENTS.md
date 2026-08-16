# OpenTask Project Memory

## Project Overview
- **Name:** OpenTask
- **Goal:** A modern, Jetpack Compose-based replacement for the **ColorNote** app, with potential **Obsidian** synchronization.
- **Structure:** Single module Android application (`:app`).
- **Tech Stack:** Kotlin, Jetpack Compose, Material3.
- **Key Files:**
  - `AppConfig.kt`: Centralized UI configuration (colors, etc.).
  - `MainActivity.kt`: Main entry point and navigation controller. Handles task selection and creation flow.
  - `PopupActivity.kt`: Adaptive popup for quick task viewing and creation (Modal overview).
  - `TaskRepository.kt`: Data management for tasks.
  - `AppTabs.kt`: Enum defining the main navigation sections.
  - `MainBottomBar.kt`: Component for bottom navigation.
  - `AddNoteButton.kt`: The primary Floating Action Button for note creation.
  - `DateUtils.kt`: Localized date formatting helpers (e.g., "Wed. 17 July").
  - `SubTopPanel.kt`: Shared component for secondary headers with consistent shadow and z-index.

## General Preferences & Rules
- **Naming Conventions:**
  - Use **AppTabs** instead of "Destinations" or "Sections" for navigation items.
  - Avoid technical/boilerplate names like "Scaffold", "Shell", "Wrapper", or "Container" for structural UI components unless they are standard library names.
- **Simplicity & Standard UI:** Keep the app as simple as possible. Prefer standard Android/Material 3 system behaviors and animations (like default ripples for clicks) over custom complex animations or custom selection indicators unless explicitly requested.
- **Interaction & Feedback:**
  - When the user provides instructions on how to interact (meta-instructions), always add them to `AGENTS.md` to ensure they persist across sessions.
  - **Proactive Memory Management:** Automatically update `AGENTS.md` with meta-learnings, architectural rules, or interaction preferences at the end of a task. Do not wait for a reminder.
  - Address user questions directly and avoid treating questions about past actions as new tasks without answering them first.
  - **Strict Scope Control:** When given specific feedback on a single aspect of a UI component (e.g., alignment), do not modify other aspects (e.g., size) unless explicitly requested. Stay strictly within the scope of the instruction.
  - **Component Isolation:** If an issue is specific to a particular screen or sub-panel (e.g., keyboard overlap in an editor), **isolate the fix within that component** rather than modifying global shared components (like `TopPanel`). Never change global components to fix local issues.
  - **Global Component Stability:** Shared components (like `TopPanel`) should not be modified unless a global change is explicitly requested. Do not attempt to "centralize" logic into global components if it causes side effects on other screens.
- **Window Insets & Stability:** 
  - To prevent "jumping" headers or black status bars when the keyboard appears, ensure headers use `statusBarsPadding()` and the scrollable content uses `imePadding()` and `navigationBarsPadding()` within its own isolated container. This prevents the system from attempting to "pan" the entire root layout.
- **Centralized Configuration:** Always use `AppConfig` for UI constants. Avoid hardcoded hex values, padding, or dimensions in UI components. If a new adjustment is needed, add it to `AppConfig` first.
- **Model-Driven Parsing:** Move all data-specific parsing and reconstruction logic (like Obsidian file handling) into the relevant model classes (e.g., `Task`). The UI should remain agnostic to the storage format and only handle presentation states (like toggling between parsed/raw views).
  - **Obsidian File Structure:**
    - Raw format: YAML frontmatter -> 1 empty line -> `# Title` -> 1 empty line -> Inner Content.
    - `Task.fromRaw` extracts the creation date strictly from the filename (`yyyy-MM-dd_HH-mm-ss.md`).
    - `Task.toRaw` always enforces the standard spacing (1 empty line after YAML, 1 empty line after Title).
- **DRY Principle:**
  - Centralize navigation and task creation logic in `MainActivity.companion`.
  - Use `MainActivity.openTask()` and `MainActivity.createNewTask()` for consistent behavior across the app and popups.
  - In list components (like `NotesList`), use a `getDefaultNoteClickHandler` pattern to automatically determine if `exitOnBack` should be true based on the context.
- **Task Creation Flow:**
  - Defer task creation until the target activity handles it (using `EXTRA_CREATE_NEW`) to avoid visual glitches in the calling UI (e.g., empty items appearing in a list before the editor opens).
- **Navigation Flow:**
  - When opening a task for editing from an external source (like a popup), use `exitOnBack = true` to ensure the user returns to the previous context when they finish or go back.
- **Layout Structure:**
  - `MainActivity` uses a standard `Scaffold`. The `MainBottomBar` goes into the `bottomBar` slot, and `AddNoteButton` goes into the `floatingActionButton` slot. This ensures the list content is correctly padded.

## UI Styling Preferences
- **Shadow & Depth:** Use `shadow(elevation = 2.dp)` and `zIndex(0.5f)` on top/sub-top panels to create a clear visual hierarchy over scrollable content.
- **Clipping:** Use `.clip(RoundedCornerShape(dp))` on parent containers that have both a background and rounded corners. This prevents child backgrounds (like headers or footers) from "bleeding" over the rounded corners.
- **Adaptive UI:** For components like popups, use `heightIn(max = screenHeight * 0.8f)` on the main container and `weight(1f, fill = false)` on scrollable content (like `NotesList`) to make the UI adapt to content size instead of taking up fixed space.
- **Text Styling:** Primary action buttons in headers/footers (like the "Ajouter" button in the popup) should use `MaterialTheme.typography.titleLarge` to match header weights.
- **Color Mapping in AppConfig:**
  - `DefaultBackgroundColor`: `0xFFEEEEEE` (Main app background).
  - `TopPanelBackgroundColor`: `0xFFD6D6D6` (Main headers).
  - `SubPanelBackgroundColor`: `0xFFE0E0E0` (Sub-headers, popup header/footer backgrounds).
  - `AddNoteButtonBackgroundColor`: Standard action green (`0xFF4CAF50`).
  - `AddNoteButtonIconColor`: Usually `Color.White`.

## Services & Notifications
- **Permanent Notifications (Foreground Service):**
  - To prevent a persistent notification from vibrating or making sound during updates (especially when the app restarts and calls `startForeground` again), use `.setSilent(true)` and `.setOnlyAlertOnce(true)` in the `NotificationCompat.Builder`.
  - Even if `IMPORTANCE_DEFAULT` is used for the channel, vibration can be disabled via `enableVibration(false)` and `setSound(null, null)` on the `NotificationChannel` object.
  - **Channel Rotation:** Android caches notification channel settings. If you change importance, sound, or vibration settings in code, you MUST increment the `channelId` (e.g., from `v5` to `v6`) for the changes to take effect on existing installs.
  - **PendingIntent Stability:** Use a stable request code (e.g., `0`) in `PendingIntent.getActivity` for the notification's content intent. Using dynamic codes (like timestamps) can cause the system to treat every update as a new notification, leading to visual glitches or unwanted alerts.

## Target project structure

Not ready yet, the goal as the app grow will be to organize the project into feature packages like this

```md
:root:
- core
  - data
    - db
  - domain
  - presentation
    - util
- feat1
  - data
  - di
    feat1Module => ex. fun provideXRepository
  - domain
    - user
      GetUserUseCase
      User
      UserValidation
  - presentation
    - components
    - usecase1
      - components
      usecase1Screen.tk
      usecase1State
    - usecase2
      - components
      usecase2Screen.tk
      usecase2State
- feat2
  - data
  - domain
  - presentation
- ui
MainActivity.kt
```
