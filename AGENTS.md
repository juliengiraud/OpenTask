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
  - **Minimalist Implementation:** Always strive for the smallest, most efficient code possible. Avoid maintaining redundant state (like local copies of lists) or creating complex workarounds when a simple direct check on existing data (like a `due_date` field) is sufficient. Minimize interactions between components; do not call an update or notification method if the underlying data relevant to that component has not changed.
- **Code Hygiene & Best Practices:**
  - **Structured Concurrency (NO GlobalScope):** Do not use `GlobalScope` for launching coroutines or as a default constructor fallback value. Always require an explicit, properly lifecycle-managed `CoroutineScope` to prevent resource and memory leaks.
  - **Explicit Imports (NO Star/Wildcard Imports):** Never use wildcard or star imports (`*`). Always list each import explicitly to maintain visibility, clean namespaces, and avoid declaration collisions.
- **Interaction & Feedback:**
  - When the user provides instructions on how to interact (meta-instructions), always add them to `AGENTS.md` to ensure they persist across sessions.
  - **Proactive Memory Management:** Automatically update `AGENTS.md` with meta-learnings, architectural rules, or interaction preferences at the end of a task. Do not wait for a reminder.
  - **Strict Scope Control:** Do not modify `TODO.md`. This file is reserved for the user's manual tracking.
  - Address user questions directly and avoid treating questions about past actions as new tasks without answering them first.
  - **Strict Scope Control:** When given specific feedback on a single aspect of a UI component (e.g., alignment), do not modify other aspects (e.g., size) unless explicitly requested. Stay strictly within the scope of the instruction.
  - **Strict File Scope:** Only modify files that are explicitly named or clearly targeted in the user's request. If the user asks to add a test case, **only** update the test file. Do not proactively modify the implementation to make the test pass or "improve" related code unless explicitly instructed to "fix" or "implement" the change.
  - **Data Integrity (STRICT):** When the user provides a code snippet or specific variables to include (e.g., in a test), include them **exactly** as requested, even if they appear unused or redundant to the IDE. Do not omit or modify any part of the requested data.
  - **No Unsolicited Renaming:** Do not rename variables, constants, or functions unless explicitly requested to improve naming or as part of a formal refactoring task. Maintain existing naming even if it feels suboptimal or inconsistent with your personal preference.
  - **Comment Preservation (STRICT):** Never remove, modify, or "clean up" existing comments in the code (especially those describing future scenarios, TODOs, or architectural notes) unless explicitly instructed to do so. These comments are for the user's tracking and must be preserved exactly as found when modifying surrounding code.
  - **Question vs Task Distinction (STRICT):** Never treat a question about how a feature works as a request to implement or change it. Always answer the question first. 
  - **MANDATORY TEXT-ONLY RESPONSE:** If the user's message is an inquiry (ends in a question mark or asks "how", "why", "where"), you are **PROHIBITED** from using `write_file`, `replace_file_content`, or `multi_replace_file_content` in that same turn. You must provide a text-only explanation first. Only proceed with implementation in a *subsequent* turn if the user explicitly instructs it (e.g., "Implement this", "Fix this").
  - **Component Isolation:** If an issue is specific to a particular screen or sub-panel (e.g., keyboard overlap in an editor), **isolate the fix within that component** rather than modifying global shared components (like `TopPanel`). Never change global components to fix local issues.
  - **Global Component Stability:** Shared components (like `TopPanel`) should not be modified unless a global change is explicitly requested. Do not attempt to "centralize" logic into global components if it causes side effects on other screens.
- **Window Insets & Stability:** 
  - To prevent "jumping" headers or black status bars when the keyboard appears, ensure headers use `statusBarsPadding()` and the scrollable content uses `imePadding()` and `navigationBarsPadding()` within its own isolated container. This prevents the system from attempting to "pan" the entire root layout.
- **Smart Interaction Preferences:**
  - **Focus Management:** When opening an editor, if the title is empty (e.g., new note), focus the title field. Otherwise, focus the body content.
  - **Empty Space Interaction:** In the editor, a tap on any empty space below the text should request focus and move the cursor to the end of the content.
  - **Visual Feedback:** Use `AppConfig.EditorFocusBorderColor` (typically orange) for active focus borders in editors.
- **Centralized Configuration:** Always use `AppConfig` for UI constants. Avoid hardcoded hex values, padding, or dimensions in UI components. If a new adjustment is needed, add it to `AppConfig` first.
- **Filesystem File Watcher API**: Rely exclusively on Java's native coroutine-powered `WatchService` (`KWatchChannel`) for structural updates rather than scanning folders on a periodic loop.
  - **Strict Filename Verification**: Only monitor, log, or scan files strictly matching the format `"yyyy-MM-dd_HH-mm-ss.md"`. Ignore all temporary or unrelated file paths at ingestion.
  - **Asynchronous Event Debouncing**: Accumulate filesystem event triggers in a per-file stack queue and apply a 100ms debounce delay. New intermediate events for the same file replace previous states and reset the timer, logging and calling actions only for the final resolved state after settlement.
  - **Full Scan Monitoring**: Perform complete directory query operations only during the initial state exploration or fallback scenarios, logging full performance runtime metrics unconditionally.
- **Model-Driven Parsing:** Move all data-specific parsing and reconstruction logic (like Obsidian file handling) into the relevant model classes (e.g., `Task`). The UI should remain agnostic to the storage format and only handle presentation states (like toggling between parsed/raw views).
  - **Note Editing Logic:** 
    - Parsed mode is the default.
    - Editing is only permitted in **parsed mode**. The edit button is disabled while in raw mode.
    - The mode toggle (Switch) is disabled while editing.
    - Auto-save on back only occurs in parsed mode if changes are detected.
  - **Deterministic Identifiers:** For file-synced models, use the unique filename as the `id`. This prevents background scans or saves from breaking UI state by generating new random UUIDs for the same file.
  - **Conflict Resolution Policy:**
    - If a conflict occurs during editing (file changed on disk while user has unsaved changes), the **YAML frontmatter from the filesystem always wins** (overwrites local changes).
    - Body content and Titles are merged using a smart strategy:
      - If both sides changed the same part, conflict markers are used (`<<<<<<< APP ...`).
      - If only one side changed, the changes are auto-merged without markers.
      - **Titles use word-level diffing** (`MergeUtils.generateConflict` with `" "` separator) to minimize conflict markers and preserve unchanged words.
  - **Obsidian File Structure:**
    - Raw format: YAML frontmatter -> 1 empty line -> `# Title` -> 1 empty line -> Inner Content.
    - `Task.fromRaw` extracts the creation date strictly from the filename (`yyyy-MM-dd_HH-mm-ss.md`).
    - `Task.toRaw` always enforces standard spacing: exactly 1 empty line after YAML and exactly 1 empty line after the `# Title` line.
    - **Whitespace Preservation:** Titles and Inner Content preserves whitespace found. If exactly one blank line exists immediately after the title, it is skipped (treated as a separator); otherwise, content begins immediately on the next line. `toRaw` will always re-normalize this to exactly one blank line.
  - **YAML Preservation:** When parsing YAML, only the properties managed by the app (done, due_date, last_update, creation_date) are extracted into `Task` fields. All other lines (comments, extra properties) are stored in `extraYaml` and preserved exactly in `toRaw`. This ensures a "perfect merge" where unmanaged data is left unchanged.
  - **Inferred Titles:** If a note title is empty, the repository infers it from the first non-empty line of the body. The UI state must explicitly synchronize with this inferred title after a save to avoid state desync (showing an empty title when one exists in the data source).
- **Compose UI Update Stability:**
  - **Reference Refresh:** Re-assigning the same object instance to a `MutableState` (or replacing it with an identical instance in a list) might not trigger recomposition. Ensure a fresh instance or a change in a key property (like `lastUpdate`) occurs for updates to be detected reliably.
  - **Dirty Checking:** In editors, only trigger expensive save operations or network/disk syncs if the content is "dirty" (actually changed from its initial state).
- **Filesystem File Watcher API**: Rely exclusively on Java's native coroutine-powered `WatchService` (`KWatchChannel`) for structural updates rather than scanning folders on a periodic loop.
  - **Strict Filename Verification**: Only monitor, log, or scan files strictly matching the format `"yyyy-MM-dd_HH-mm-ss.md"`. Ignore all temporary or unrelated file paths at ingestion.
  - **Asynchronous Event Debouncing**: Accumulate filesystem event triggers in a per-file stack queue and apply a debounce delay (`FS_EVENT_DEBOUNCE_MS`, typically 100ms). New intermediate events for the same file replace previous states and reset the timer, logging and calling actions only for the final resolved state after settlement.
  - **Echo Suppression**: To prevent the watcher from triggering on the app's own writes, use `pauseWatching`/`resumeWatching` with a settlement delay (matching the debounce time) during I/O operations. This ensures the asynchronous OS "echo" is reliably caught while the file is still ignored.
  - **Background Initialization**: Perform initial directory scans and watcher setup in a background `CoroutineScope` (e.g., `Dispatchers.IO`) to prevent ANRs, especially when dealing with hundreds of files.
- **Anti-Patterns & Lessons Learned:**
  - **Avoid Over-Engineering Repository Updates**: When replacing an item in a list to trigger a UI update, do not attempt to "optimize" by checking for specific field changes (like dates) unless flickering is a proven issue. A simple, predictable replacement is often safer and maintains the correct "last update" state.
  - **Main Thread I/O is Critical**: Never perform bulk filesystem operations (like reading 300+ files) or service setup synchronously on the Main Thread. Always wrap these in a dedicated `CoroutineScope` with `Dispatchers.IO`.
  - **Asynchronous FS Echos**: Standard `WatchService` implementations are asynchronous. A `finally` block that resumes watching immediately after a write will almost always catch the "echo" of that write. A settlement delay (matching the debounce window) is mandatory when resuming.
  - **Local State Synchronization**: When a component holds local `mutableStateOf` (like a title in an editor), always ensure there is a mechanism (like a `LaunchedEffect`) to sync that state back from the source-of-truth prop when the component is not in active use (not editing), otherwise inferred changes from the data layer will be missed.
- **DRY Principle:**
  - Centralize navigation and task creation logic in `MainActivity.companion`.
  - Use `MainActivity.openTask()` and `MainActivity.createNewTask()` for consistent behavior across the app and popups.
  - **State Refreshing:** After performing a save or update on a task, always refresh any local references to that task (e.g., `selectedTask`) from the repository. This ensures the UI is working with the latest data and prevents state desync between the view and the data source.
  - In list components (like `NotesList`), use a `getDefaultNoteClickHandler` pattern to automatically determine if `exitOnBack` should be true based on the context.
- **Task Creation Flow:**
  - Defer task creation until the target activity handles it (using `EXTRA_CREATE_NEW`) to avoid visual glitches in the calling UI (e.g., empty items appearing in a list before the editor opens).
- **Navigation Flow:**
  - When opening a task for editing from an external source (like a popup), use `exitOnBack = true` to ensure the user returns to the previous context when they finish or go back.
  - **Unique Page Stack:** For tab or pager-based navigation, the back stack should only contain unique pages. When pushing a page to the stack, if it already exists, remove the oldest occurrence to prevent an infinite stack and ensure the back button cycles through distinct recent locations.
  - **Tab State Reset:** For specific screens like `CalendarScreen`, the local view state (e.g., selected month) should reset to the default ("Today") when the user navigates away from the tab.
    - For **swiping**, use `pagerState.settledPage` to ensure the reset only occurs once the transition is completed and the user has released the screen (avoiding jumps during "peeking").
    - For **button navigation**, use a manual trigger in `onTabClick` to ensure the reset happens immediately even if the user navigates back quickly before the animation settles.
- **Activity Lifecycle & Navigation:** `MainActivity` uses `standard` launch mode (not `singleTop`) to allow transient editor instances to exist on top of `PopupActivity` without finishing the main application's instance. Use `exitOnBack = true` for these transient instances. `PopupActivity` uses `android:excludeFromRecents="true"` to prevent transient overlay popups from appearing in the user's system recent apps history.
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
  - **Date Change Handling:** The `MainService` must listen for `Intent.ACTION_DATE_CHANGED` and `Intent.ACTION_TIMEZONE_CHANGED` to ensure the permanent notification is refreshed when a new day starts, even if no tasks are modified.
  - **Content Filtering:** The permanent notification and its associated `PopupActivity` should only display tasks for the current day that are not yet marked as done. A task belongs to "today" if its `due_date` is today.
  - **Encapsulated Startup:** `AppNotificationManager` encapsulates the `startForeground` logic. Use `notificationManager.start(service, taskNames)` to initialize the foreground state.
  - **State-Aware Updates:** `AppNotificationManager` maintains an internal `taskNames` state. `updateForegroundNotification` only triggers a system update if the new task list differs from the cached one, preventing redundant refreshes.
- **Performance Optimization:** Use the `_tasksByDate` map in `TaskRepository` for fast lookup of notes by date. Any operation that modifies the main task list must call `rebuildIndex()` to keep the map in sync.

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
