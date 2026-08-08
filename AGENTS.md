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
- **Centralized Configuration:** Always use `AppConfig` for UI constants. Avoid hardcoded hex values or padding in UI components.
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
