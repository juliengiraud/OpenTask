# Services & Notifications

## Permanent Notification (Foreground Service)
- To prevent the persistent notification from vibrating/sounding on update (e.g. app restart calling `startForeground` again), use `.setSilent(true)` and `.setOnlyAlertOnce(true)` on `NotificationCompat.Builder`.
- Even with `IMPORTANCE_DEFAULT`, vibration can be disabled via `enableVibration(false)` and `setSound(null, null)` on the `NotificationChannel`.
- **Channel rotation:** Android caches notification channel settings — any change to importance/sound/vibration requires incrementing the `channelId` (e.g. `v5` → `v6`) to take effect on existing installs.
- **PendingIntent stability:** use a stable request code (e.g. `0`) in `PendingIntent.getActivity` for the notification's content intent. Dynamic codes (e.g. timestamps) make the system treat every update as a new notification → visual glitches / unwanted alerts.
- **Date change handling:** `MainService` listens for `Intent.ACTION_DATE_CHANGED` and `Intent.ACTION_TIMEZONE_CHANGED` to refresh the permanent notification when a new day starts, even without task modifications.
- **Content filtering:** the permanent notification and its `PopupActivity` only show tasks for the current day that aren't yet done. A task belongs to "today" if its `due_date` is today.
- **Encapsulated startup:** `AppNotificationManager` encapsulates `startForeground` logic — use `notificationManager.start(service, taskNames)` to initialize foreground state.
- **State-aware updates:** `AppNotificationManager` caches `taskNames` internally; `updateForegroundNotification` only triggers a system update if the new task list differs from the cached one (avoids redundant refreshes).

## Repository Performance
- `TaskRepository` uses a `_tasksByDate` map for fast lookup of notes by date.
- Any operation that modifies the main task list must call `rebuildIndex()` to keep the map in sync.
