package com.example.opentask.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.opentask.model.Task
import com.example.opentask.model.TaskRepository
import com.example.opentask.service.MainService
import com.example.opentask.ui.theme.OpenTaskTheme
import kotlinx.coroutines.launch
import androidx.core.content.edit

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> }

    private var watchedFolder by mutableStateOf<String?>(null)
    private val debugLogs = mutableStateListOf<String>()
    var selectedTask by mutableStateOf<Task?>(null)
    var isEditMode by mutableStateOf(false)
    var exitOnBack by mutableStateOf(false)

    private val debugReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MainService.ACTION_DEBUG_LOG) {
                val message = intent.getStringExtra(MainService.EXTRA_LOG_MESSAGE)
                message?.let { addDebugLog(it) }
            }
        }
    }

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            watchedFolder = it.toString()
            saveWatchedFolder(it.toString())

            // Notify service about folder change
            val intent = Intent(this, MainService::class.java).apply {
                action = MainService.ACTION_UPDATE_WATCHED_FOLDER
                putExtra(MainService.EXTRA_FOLDER_URI, it.toString())
            }
            startService(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        watchedFolder = getSavedWatchedFolder()
        handleIntent(intent)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val filter = IntentFilter(MainService.ACTION_DEBUG_LOG)
        registerReceiver(debugReceiver, filter, RECEIVER_NOT_EXPORTED)

        val intent = Intent(this, MainService::class.java)
        Log.d("MainActivity", "Starting MainService")
        startForegroundService(intent)

        setContent {
            OpenTaskTheme {
                OpenTaskApp(
                    activity = this,
                    onSelectFolder = { folderPickerLauncher.launch(null) },
                    onResetWatcher = {
                        watchedFolder = null
                        saveWatchedFolder(null)
                        val resetIntent = Intent(this, MainService::class.java).apply {
                            action = MainService.ACTION_RESET_WATCHER
                        }
                        startService(resetIntent)
                    },
                    watchedFolder = watchedFolder,
                    debugLogs = debugLogs
                )
            }
        }
    }

    private fun saveWatchedFolder(uri: String?) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        prefs.edit { putString("watched_folder", uri) }
    }

    private fun getSavedWatchedFolder(): String? {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        return prefs.getString("watched_folder", null)
    }

    fun addDebugLog(message: String) {
        debugLogs.add(message)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val taskId = intent?.getStringExtra("TASK_ID")
        exitOnBack = intent?.getBooleanExtra("EXIT_ON_BACK", false) ?: false
        if (taskId != null) {
            selectedTask = TaskRepository.tasks.find { it.id == taskId }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(debugReceiver)
    }
}

@Composable
fun OpenTaskApp(
    activity: MainActivity,
    onSelectFolder: () -> Unit,
    onResetWatcher: () -> Unit,
    watchedFolder: String?,
    debugLogs: List<String>
) {
    val pagerState = rememberPagerState(pageCount = { AppTabs.entries.size })
    val scope = rememberCoroutineScope()

    if (activity.selectedTask != null) {
        val handleBack = {
            if (activity.isEditMode) {
                activity.isEditMode = false
            } else {
                if (activity.exitOnBack) {
                    activity.exitOnBack = false
                    activity.finish()
                } else {
                    activity.selectedTask = null
                }
            }
        }
        BackHandler(onBack = handleBack)
        NoteDetailScreen(
            task = activity.selectedTask!!,
            isEditMode = activity.isEditMode,
            onEditModeChange = { activity.isEditMode = it },
            onSave = { newContent ->
                TaskRepository.updateTask(activity.selectedTask!!.id, newContent)
            },
            onBack = handleBack
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                val currentTab = AppTabs.entries[pagerState.currentPage]
                TopPanel(title = currentTab.label)
            },
            bottomBar = {
                MainBottomBar(
                    selectedTabIndex = pagerState.currentPage,
                    onTabClick = { index ->
                        scope.launch {
                            pagerState.scrollToPage(index)
                        }
                    },
                    onAddNoteClick = { /* TODO */ }
                )
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) { pageIndex ->
                val tab = AppTabs.entries[pageIndex]
                when (tab) {
                    AppTabs.HOME -> NotesScreen(
                        modifier = Modifier.fillMaxSize()
                    )
                    AppTabs.FAVORITES -> Greeting("Favorites", Modifier.fillMaxSize())
                    AppTabs.SETTINGS -> SettingsScreen(
                        onSelectFolder = onSelectFolder,
                        onResetWatcher = onResetWatcher,
                        watchedFolder = watchedFolder,
                        debugLogs = debugLogs,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    OpenTaskTheme {
        Greeting("Android")
    }
}
