package com.example.androidtaskapp.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.androidtaskapp.R
import com.example.androidtaskapp.service.TaskService
import com.example.androidtaskapp.ui.theme.AndroidTaskAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private var watchedFolder by mutableStateOf<String?>(null)
    private val debugLogs = mutableStateListOf<String>()

    private val debugReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TaskService.ACTION_DEBUG_LOG) {
                val message = intent.getStringExtra(TaskService.EXTRA_LOG_MESSAGE)
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
            val intent = Intent(this, TaskService::class.java).apply {
                action = TaskService.ACTION_UPDATE_WATCHED_FOLDER
                putExtra(TaskService.EXTRA_FOLDER_URI, it.toString())
            }
            startService(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        watchedFolder = getSavedWatchedFolder()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val filter = IntentFilter(TaskService.ACTION_DEBUG_LOG)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(debugReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            // No flag needed for older versions, or can use 0
            registerReceiver(debugReceiver, filter)
        }

        val intent = Intent(this, TaskService::class.java)
        Log.d("MainActivity", "Starting TaskService")
        startForegroundService(intent)

        setContent {
            AndroidTaskAppTheme {
                AndroidTaskAppApp(
                    onSelectFolder = { folderPickerLauncher.launch(null) },
                    onResetWatcher = {
                        watchedFolder = null
                        saveWatchedFolder(null)
                        val resetIntent = Intent(this, TaskService::class.java).apply {
                            action = TaskService.ACTION_RESET_WATCHER
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
        prefs.edit().putString("watched_folder", uri).apply()
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
    val pagerState = rememberPagerState(pageCount = { AppDestinations.entries.size })
    val scope = rememberCoroutineScope()

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
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { pageIndex ->
                val destination = AppDestinations.entries[pageIndex]
                when (destination) {
                    AppDestinations.HOME -> Greeting("Home", Modifier.fillMaxSize())
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

@Composable
fun SettingsScreen(
    onSelectFolder: () -> Unit,
    onResetWatcher: () -> Unit,
    watchedFolder: String?,
    debugLogs: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = "Watched Folder:", style = MaterialTheme.typography.titleMedium)
        Text(text = watchedFolder ?: "Not selected", style = MaterialTheme.typography.bodyMedium)
        
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = onSelectFolder, modifier = Modifier.fillMaxWidth()) {
                Text("Select Folder")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onResetWatcher, modifier = Modifier.fillMaxWidth()) {
                Text("Reset Watcher")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = "Debug Information", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 8.dp)
        ) {
            items(debugLogs) { log ->
                Text(text = log, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.ic_home),
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
