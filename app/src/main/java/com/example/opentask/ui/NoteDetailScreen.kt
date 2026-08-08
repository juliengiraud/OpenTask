package com.example.opentask.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.example.opentask.model.Task
import java.time.format.DateTimeFormatter

@Composable
fun NoteDetailScreen(
    task: Task,
    isEditMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    onSave: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var textFieldValue by remember(task.id) { 
        mutableStateOf(TextFieldValue(task.textContent)) 
    }
    val focusRequester = remember { FocusRequester() }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            focusRequester.requestFocus()
        }
    }

    Column(modifier = modifier.fillMaxSize().background(Color.White)) {
        // Top Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .shadow(elevation = 4.dp)
                .zIndex(1f)
                .background(AppConfig.TopPanelBackgroundColor)
                .padding(start = 16.dp, end = 16.dp, bottom = 4.dp, top = 16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "←",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .clickable { onBack() }
                            .padding(end = 12.dp)
                    )
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1
                    )
                }

                Text(
                    text = if (isEditMode) "✓" else "✎",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .clickable { 
                            if (isEditMode) {
                                onSave(textFieldValue.text)
                            } else {
                                // When entering via icon, place cursor at the end
                                textFieldValue = textFieldValue.copy(
                                    selection = TextRange(textFieldValue.text.length)
                                )
                            }
                            onEditModeChange(!isEditMode) 
                        }
                        .padding(start = 12.dp)
                )
            }
        }

        // Sub Top Panel
        SubTopPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val shortFilename = if (task.filename.length > 10) {
                    task.filename.take(10)
                } else {
                    task.filename
                }
                Text(
                    text = shortFilename,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh-mm-ss")
                Text(
                    text = task.lastUpdate.format(formatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }
        }

        // Content
        val scrollState = rememberScrollState()
        val textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Box {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    readOnly = !isEditMode,
                    textStyle = textStyle,
                    onTextLayout = { textLayoutResult = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.drawBehind {
                                textLayoutResult?.let { layout ->
                                    val strokeWidth = 0.5.dp.toPx()
                                    for (i in 0 until layout.lineCount) {
                                        val y = layout.getLineBottom(i) - strokeWidth / 2
                                        drawLine(
                                            color = Color(0xFFF2F2F2),
                                            start = Offset(0f, y),
                                            end = Offset(size.width, y),
                                            strokeWidth = strokeWidth
                                        )
                                    }
                                }
                            }
                        ) {
                            innerTextField()
                        }
                    }
                )

                if (!isEditMode) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { offset ->
                                        textLayoutResult?.let { layout ->
                                            val index = layout.getOffsetForPosition(offset)
                                            textFieldValue = textFieldValue.copy(
                                                selection = TextRange(index)
                                            )
                                        }
                                        onEditModeChange(true)
                                    }
                                )
                            }
                    )
                }
            }
        }
    }
}
