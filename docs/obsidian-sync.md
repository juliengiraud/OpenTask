# Obsidian Sync & Note Model

## Model-Driven Parsing
All data-specific parsing/reconstruction logic (Obsidian file handling) lives in the relevant model classes (e.g. `Task`). The UI stays agnostic to storage format and only handles presentation state (parsed vs. raw view).

## Obsidian File Structure
Raw format:
```
YAML frontmatter
(1 empty line)
# Title
(1 empty line)
Inner Content
```
- `Task.fromRaw` extracts the creation date strictly from the filename (`yyyy-MM-dd_HH-mm-ss.md`).
- `Task.toRaw` always enforces standard spacing: exactly 1 empty line after YAML, exactly 1 empty line after `# Title`.
- **Whitespace preservation:** titles and inner content preserve found whitespace. If exactly one blank line exists immediately after the title, it's treated as a separator and skipped; otherwise content begins on the next line. `toRaw` always re-normalizes to exactly one blank line.

## YAML Preservation
Only app-managed properties (`done`, `due_date`, `last_update`, `creation_date`) are extracted into `Task` fields. All other lines (comments, extra properties) are stored in `extraYaml` and preserved exactly in `toRaw` — giving a "perfect merge" where unmanaged data is left unchanged.

## Inferred Titles
If a note title is empty, the repository infers it from the first non-empty line of the body. UI state must explicitly re-sync with this inferred title after a save, to avoid showing an empty title when the data source actually has one.

## Note Editing Logic
- Parsed mode is the default.
- Editing is only permitted in **parsed mode**; the edit button is disabled in raw mode.
- The mode toggle (Switch) is disabled while editing.
- Auto-save on back only occurs in parsed mode, and only if changes are detected.

## Deterministic Identifiers
For file-synced models, the unique filename is used as the `id`. This prevents background scans/saves from breaking UI state by generating new random UUIDs for the same file.

## Conflict Resolution Policy
- If the file changed on disk while the user has unsaved changes:
  - **YAML frontmatter from the filesystem always wins** (overwrites local changes).
  - **Body content and Titles** are merged with a smart strategy:
    - Same part changed on both sides → conflict markers (`<<<<<<< APP ...`).
    - Only one side changed → auto-merge, no markers.
    - **Titles use word-level diffing** (`MergeUtils.generateConflict` with `" "` separator) to minimize conflict markers and preserve unchanged words.

## Filesystem File Watcher
- Uses Java's native coroutine-powered `WatchService` (`KWatchChannel`) exclusively — no periodic folder scanning for structural updates.
- **Strict filename verification:** only monitors/logs/scans files strictly matching `yyyy-MM-dd_HH-mm-ss.md`; all other paths are ignored at ingestion.
- **Async event debouncing:** filesystem events accumulate in a per-file stack queue with a debounce delay (`FS_EVENT_DEBOUNCE_MS`, typically 100ms). New intermediate events for the same file replace previous state and reset the timer; only the final resolved state after settlement triggers logging/actions.
- **Echo suppression:** to avoid the watcher reacting to the app's own writes, use `pauseWatching`/`resumeWatching` around I/O with a settlement delay matching the debounce time (the OS write "echo" is asynchronous, so resuming immediately would still catch it).
- **Background initialization:** initial directory scans and watcher setup run in a background `CoroutineScope` (`Dispatchers.IO`) to avoid ANRs, especially with hundreds of files.
- **Full scan monitoring:** complete directory query operations only run on initial state exploration or fallback scenarios, with full performance/runtime metrics logged unconditionally.
- **Service lifecycle optimization:** `MainService` skips folder scanning/watcher setup if already initialized (e.g. Activity reconnecting to an existing Foreground Service), determined via a null check on `FolderWatcherManager`. Avoids redundant I/O and `ClosedWatchServiceException` logs.
