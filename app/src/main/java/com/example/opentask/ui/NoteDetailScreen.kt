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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
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
    var isParsedMode by remember { mutableStateOf(false) }
    var currentTaskState by remember(task.id) { mutableStateOf(task) }

    var textFieldValue by remember(task.id) { 
        mutableStateOf(TextFieldValue(task.toRaw())) 
    }
    val focusRequester = remember { FocusRequester() }

    val handleSave = { content: String ->
        val toSave = if (isParsedMode) {
            currentTaskState.copy(textContent = content).toRaw()
        } else {
            content
        }
        onSave(toSave)
    }

    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Panel (Reverted to Column as per initial state)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .shadow(elevation = 4.dp)
                .zIndex(1f)
                .background(AppConfig.TopPanelBackgroundColor)
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
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
                                handleSave(textFieldValue.text)
                            } else {
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

                Switch(
                    checked = isParsedMode,
                    onCheckedChange = { checked ->
                        if (checked) {
                            // Raw -> Parsed
                            val newTask = Task.fromRaw(task.filename, textFieldValue.text)
                            currentTaskState = newTask
                            textFieldValue = TextFieldValue(newTask.textContent)
                        } else {
                            // Parsed -> Raw
                            val raw = currentTaskState.copy(textContent = textFieldValue.text).toRaw()
                            textFieldValue = TextFieldValue(raw)
                        }
                        isParsedMode = checked
                    }
                )
                
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh-mm-ss")
                Text(
                    text = task.lastUpdate.format(formatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }
        }

        // Isolated Editor Panel
        NoteEditorPanel(
            textFieldValue = textFieldValue,
            onValueChange = { textFieldValue = it },
            isEditMode = isEditMode,
            onEditModeChange = onEditModeChange,
            focusRequester = focusRequester,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NoteEditorPanel(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    isEditMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black)

    // Auto-scroll logic isolated to the editor panel
    LaunchedEffect(textFieldValue.selection, textLayoutResult, isEditMode) {
        if (!isEditMode) return@LaunchedEffect
        textLayoutResult?.let { layout ->
            val cursorOffset = textFieldValue.selection.max
            if (cursorOffset <= layout.layoutInput.text.length) {
                val lineIndex = layout.getLineForOffset(cursorOffset)
                val lineBottom = layout.getLineBottom(lineIndex)
                val paddingTopPx = with(density) { 16.dp.toPx() }
                val targetScroll = lineBottom + paddingTopPx
                val buffer = with(density) { 48.dp.toPx() }
                
                if (scrollState.viewportSize > 0) {
                    val isCursorBelowViewport = targetScroll + buffer > scrollState.value + scrollState.viewportSize
                    if (isCursorBelowViewport) {
                        scrollState.animateScrollTo((targetScroll + buffer - scrollState.viewportSize).toInt())
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = viewportHeight)
                    .drawBehind {
                        textLayoutResult?.let { layout ->
                            val strokeWidth = 0.5.dp.toPx()
                            val lineCount = layout.lineCount
                            val paddingTopPx = 16.dp.toPx()
                            
                            if (lineCount > 0) {
                                // 1. Draw lines for all existing text lines
                                for (i in 0 until lineCount) {
                                    val y = paddingTopPx + layout.getLineBottom(i) - strokeWidth / 2
                                    drawLine(
                                        color = Color(0xFFF2F2F2),
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = strokeWidth
                                    )
                                }

                                // 2. Calculate average line height for the rest
                                val lineHeight = if (lineCount > 1) {
                                    layout.getLineBottom(1) - layout.getLineBottom(0)
                                } else {
                                    layout.getLineBottom(0)
                                }

                                // 3. Continue drawing lines until the end of the container
                                var currentY = paddingTopPx + layout.getLineBottom(lineCount - 1)
                                while (currentY + lineHeight <= size.height + strokeWidth) {
                                    currentY += lineHeight
                                    val drawY = currentY - strokeWidth / 2
                                    drawLine(
                                        color = Color(0xFFF2F2F2),
                                        start = Offset(0f, drawY),
                                        end = Offset(size.width, drawY),
                                        strokeWidth = strokeWidth
                                    )
                                }
                            }
                        }
                    }
                    .padding(horizontal = 12.dp)
                    .padding(top = 16.dp, bottom = 48.dp)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = onValueChange,
                    readOnly = !isEditMode,
                    textStyle = textStyle,
                    onTextLayout = { textLayoutResult = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        innerTextField()
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
                                            onValueChange(textFieldValue.copy(selection = TextRange(index)))
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
