# OpenTask

A note & task management app integrated with Obsidian

## Overview

Inspired by ColorNote and Obsidian, the goal of this app is to automate life and personal knowledge management

Development still in progress for MVP, here is the overview of the minimal features needed:

- [/] UI
  - [/] All notes screen
    - [ ] sorting options
    - [x] ability to create note
  - [x] Single note screen
    - [x] ability to edit & save note
  - [/] Calendar screen
    - [ ] view daily notes on calendar
    - [x] ability to add note on specific day
  - [x] Settings screen
    - [x] select Obsidian folder
- [/] Obsidian / filesystem compatibility
  - [x] read notes from Obsidian files
  - [x] save notes as Obsidian files
  - [x] watch for changes in Obsidian files
  - [/] handle conflicts

## Data flow

WIP: right now the data flow is quite messy, here is what it should be

```mermaid
sequenceDiagram
    actor U as User
    participant App as Application
    participant Repo as NotesRepository
    participant Watcher as FolderWatcher
    participant FS as Obsidian Folder

    Note over U,FS: 1. Select folder, initialize repository & setup watcher

    U->>App: Select Obsidian folder
    App->>FS: List folder files
    FS-->>App: Folder files
    App->>App: Filter note only files
    loop For each note file
        App->>FS: Read file content
        FS-->>App: Content (markdown)
        App->>App: Parse into Note
        App->>Repo: Add to repository
    end
    App->>Watcher: Starts watching folder

    Note over U,FS: 2. External changes detected

    FS-->>Watcher: Event (created/updated/deleted file)
    Watcher->>App: Notify change
    App->>FS: Re-read affected file(s)
    FS-->>App: Updated content
    App->>App: Parse content
    App->>Repo: Update corresponding note
    Repo-->>U: UI updated (observed via Flow/LiveData)

    Note over U,FS: 3. Create/modify from the app

    U->>App: Create or modify a note
    App->>Repo: Update repository
    App->>Watcher: Pause watching on corresponding file
    App->>FS: Write corresponding file
    App->>Watcher: Re-enable watching on corresponding file
    Note right of Watcher: Prevents an infinite loop<br/>triggered by our own write
```
