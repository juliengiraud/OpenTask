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

    companion object {
        private const val EXTRA_TASK_ID = "TASK_ID"
        private const val EXTRA_IS_EDIT_MODE = "IS_EDIT_MODE"
        private const val EXTRA_EXIT_ON_BACK = "EXIT_ON_BACK"
        private const val EXTRA_CREATE_NEW = "CREATE_NEW"

        fun createIntent(
            context: Context,
            taskId: String? = null,
            isEditMode: Boolean = false,
            exitOnBack: Boolean = false,
            createNew: Boolean = false
        ): Intent {
            return Intent(context, MainActivity::class.java).apply {
                if (taskId != null) putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_IS_EDIT_MODE, isEditMode)
                putExtra(EXTRA_EXIT_ON_BACK, exitOnBack)
                putExtra(EXTRA_CREATE_NEW, createNew)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }

        fun openTask(
            context: Context,
            task: Task,
            isEditMode: Boolean = false,
            exitOnBack: Boolean = false
        ) {
            if (context is MainActivity) {
                context.selectedTask = task
                context.isEditMode = isEditMode
                context.exitOnBack = exitOnBack
            } else {
                context.startActivity(createIntent(context, task.id, isEditMode, exitOnBack))
            }
        }

        fun createNewTask(context: Context, exitOnBack: Boolean = false) {
            if (context is MainActivity) {
                val newTask = TaskRepository.createEmptyTask()
                openTask(context, newTask, isEditMode = true, exitOnBack = exitOnBack)
            } else {
                context.startActivity(createIntent(context, isEditMode = true, exitOnBack = exitOnBack, createNew = true))
            }
        }
    }

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
        val taskId = intent?.getStringExtra(EXTRA_TASK_ID)
        val createNew = intent?.getBooleanExtra(EXTRA_CREATE_NEW, false) ?: false
        exitOnBack = intent?.getBooleanExtra(EXTRA_EXIT_ON_BACK, false) ?: false
        isEditMode = intent?.getBooleanExtra(EXTRA_IS_EDIT_MODE, false) ?: false
        
        if (createNew) {
            selectedTask = TaskRepository.createEmptyTask()
        } else if (taskId != null) {
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
    val actualPageCount = AppTabs.entries.size
    val virtualPageCount = 1000 * actualPageCount
    val initialPage = virtualPageCount / 2
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { virtualPageCount }
    )
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
                val currentTab = AppTabs.entries[pagerState.currentPage % actualPageCount]
                TopPanel(title = currentTab.label)
            },
            bottomBar = {
                MainBottomBar(
                    selectedTabIndex = pagerState.currentPage % actualPageCount,
                    onTabClick = { index ->
                        scope.launch {
                            val currentVirtualPage = pagerState.currentPage
                            val currentActualIndex = currentVirtualPage % actualPageCount
                            val diff = index - currentActualIndex
                            pagerState.scrollToPage(currentVirtualPage + diff)
                        }
                    }
                )
            },
            floatingActionButton = {
                AddNoteButton(
                    onClick = {
                        MainActivity.createNewTask(activity)
                    }
                )
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { pageIndex ->
                val tab = AppTabs.entries[pageIndex % actualPageCount]
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
