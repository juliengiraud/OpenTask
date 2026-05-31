package com.example.androidtaskapp.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.androidtaskapp.R
import com.example.androidtaskapp.model.Task
import com.example.androidtaskapp.model.TaskRepository
import com.example.androidtaskapp.service.MainService
import com.example.androidtaskapp.ui.theme.AndroidTaskAppTheme
import kotlinx.coroutines.launch
import androidx.core.content.edit

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private var watchedFolder by mutableStateOf<String?>(null)
    private val debugLogs = mutableStateListOf<String>()

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
            AndroidTaskAppTheme {
                AndroidTaskAppApp(
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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(debugReceiver)
    }
}

@Composable
fun AndroidTaskAppApp(
    onSelectFolder: () -> Unit,
    onResetWatcher: () -> Unit,
    watchedFolder: String?,
    debugLogs: List<String>
) {
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { AppDestinations.entries.size })
    val scope = rememberCoroutineScope()

    if (selectedTask != null) {
        BackHandler {
            if (isEditMode) {
                isEditMode = false
            } else {
                selectedTask = null
            }
        }
        NoteDetailScreen(
            task = selectedTask!!,
            isEditMode = isEditMode,
            onEditModeChange = { isEditMode = it },
            onSave = { newContent ->
                TaskRepository.updateTask(selectedTask!!.id, newContent)
            },
            onBack = {
                if (isEditMode) {
                    isEditMode = false
                } else {
                    selectedTask = null
                }
            }
        )
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEachIndexed { index, destination ->
                    item(
                        icon = {
                            Icon(
                                painterResource(destination.icon),
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.scrollToPage(index)
                            }
                        }
                    )
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    val currentDestination = AppDestinations.entries[pagerState.currentPage]
                    TopPanel(title = currentDestination.label)
                }
            ) { innerPadding ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) { pageIndex ->
                    val destination = AppDestinations.entries[pageIndex]
                    when (destination) {
                        AppDestinations.HOME -> NotesScreen(
                            onTaskClick = { selectedTask = it },
                            modifier = Modifier.fillMaxSize()
                        )
                        AppDestinations.FAVORITES -> Greeting("Favorites", Modifier.fillMaxSize())
                        AppDestinations.SETTINGS -> SettingsScreen(
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
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Notes", R.drawable.ic_event),
    FAVORITES("Favorites", R.drawable.ic_favorite),
    SETTINGS("Settings", R.drawable.ic_settings),
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
    AndroidTaskAppTheme {
        Greeting("Android")
    }
}
