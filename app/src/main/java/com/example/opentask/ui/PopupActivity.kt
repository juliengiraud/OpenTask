package com.example.opentask.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.opentask.model.TaskRepository
import com.example.opentask.service.MainService
import com.example.opentask.ui.theme.OpenTaskTheme
import com.example.opentask.util.DateUtils
import java.time.LocalDate

class PopupActivity : ComponentActivity() {
    private var currentDate by mutableStateOf(LocalDate.now())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            OpenTaskTheme {
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                val date = currentDate
                val formattedDate = DateUtils.formatDate(date)
                
                // Use derivedStateOf to react to TaskRepository index changes automatically
                val todaysTasks by remember(date) {
                    derivedStateOf {
                        TaskRepository.getTasksForDate(date)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { finish() }, // Close when clicking outside
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .heightIn(max = screenHeight * 0.8f) // Max 80% of screen height
                            .background(AppConfig.DefaultBackgroundColor, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = false) { }, // Prevent closing when clicking inside
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppConfig.SubPanelBackgroundColor)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        NotesList(
                            tasks = todaysTasks,
                            modifier = Modifier
                                .weight(1f, fill = false) // Adaptive height
                                .padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Footer Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppConfig.SubPanelBackgroundColor)
                                .clickable {
                                    MainActivity.createNewTask(this@PopupActivity, exitOnBack = true, dueDate = date.atStartOfDay())
                                }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ajouter",
                                color = AppConfig.AddNoteButtonBackgroundColor,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val dateExtra = intent.getStringExtra(EXTRA_DATE)
        currentDate = if (dateExtra != null) LocalDate.parse(dateExtra) else LocalDate.now()
    }

    companion object {
        const val EXTRA_DATE = "extra_date"
    }
}
