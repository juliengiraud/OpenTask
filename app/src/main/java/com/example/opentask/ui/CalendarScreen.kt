package com.example.opentask.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.opentask.R
import com.example.opentask.util.DateUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    val dragThreshold = 50f // Minimum distance to trigger a swipe

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppConfig.DefaultBackgroundColor)
            .padding(8.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { totalDragY = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        totalDragY += dragAmount
                    },
                    onDragEnd = {
                        if (totalDragY < -dragThreshold) {
                            // Swipe Up -> Next Month
                            selectedDate = selectedDate.plusMonths(1)
                        } else if (totalDragY > dragThreshold) {
                            // Swipe Down -> Previous Month
                            selectedDate = selectedDate.minusMonths(1)
                        }
                    }
                )
            }
    ) {
        SubTopPanel(contentPadding = PaddingValues(0.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalendarHeaderSection(
                    weight = 2f,
                    onClick = { selectedDate = selectedDate.minusMonths(1) }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_triangle_up),
                        contentDescription = "Previous",
                        tint = Color.Black,
                        modifier = Modifier.offset(y = 2.dp)
                    )
                }
                CalendarHeaderSection(
                    weight = 3f,
                    onClick = { /* Middle action */ }
                ) {
                    Text(
                        text = DateUtils.formatMonthYear(selectedDate),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black
                    )
                }
                CalendarHeaderSection(
                    weight = 2f,
                    onClick = { selectedDate = selectedDate.plusMonths(1) }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_triangle_down),
                        contentDescription = "Next",
                        tint = Color.Black,
                        modifier = Modifier.offset(y = (-2).dp)
                    )
                }
            }
        }

        // Days of week header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            val days = DayOfWeek.entries
            val locale = Locale.getDefault()
            days.forEachIndexed { index, day ->
                val color = when (index) {
                    5 -> AppConfig.CalendarSaturdayColor
                    6 -> AppConfig.CalendarSundayColor
                    else -> AppConfig.CalendarWeekdayColor
                }
                val dayLabel = day.getDisplayName(TextStyle.FULL, locale)
                    .take(3)
                    .uppercase() + "."

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 2.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = dayLabel,
                        color = color,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Calendar Grid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            val firstOfMonth = selectedDate.withDayOfMonth(1)
            val startOffset = firstOfMonth.dayOfWeek.value - 1

            // 1. Backgrounds and Day Numbers
            Column(modifier = Modifier.fillMaxSize()) {
                repeat(6) { row ->
                    Row(modifier = Modifier.weight(1f)) {
                        repeat(7) { col ->
                            val index = row * 7 + col
                            val targetDate = firstOfMonth.plusDays(index.toLong() - startOffset)
                            val isInMonth = targetDate.month == selectedDate.month
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (isInMonth) Color.White else AppConfig.CalendarOutOfMonthColor)
                                    .padding(start = 4.dp, top = 0.dp),
                                contentAlignment = Alignment.TopStart
                            ) {
                                val textColor = if (isInMonth) {
                                    when (col) {
                                        5 -> AppConfig.CalendarSaturdayColor
                                        6 -> AppConfig.CalendarSundayColor
                                        else -> AppConfig.CalendarWeekdayColor
                                    }
                                } else {
                                    Color.Gray
                                }
                                Text(
                                    text = targetDate.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            // 2. Grid lines and Today Highlight
            Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
                val strokeWidth = 0.5.dp.toPx()
                val columns = 7
                val rows = 6
                val cellWidth = size.width / columns
                val cellHeight = size.height / rows

                // Draw gray vertical lines
                for (i in 0..columns) {
                    val x = i * cellWidth
                    drawLine(
                        color = AppConfig.CalendarGridLineColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = strokeWidth
                    )
                }

                // Draw gray horizontal lines
                for (i in 0..rows) {
                    val y = i * cellHeight
                    drawLine(
                        color = AppConfig.CalendarGridLineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                }

                // Draw Today's Blue Highlight (on top of gray lines)
                val today = LocalDate.now()
                val todayIndex = startOffset + today.dayOfMonth - 1
                if (today.month == selectedDate.month && todayIndex in 0 until 42) {
                    val r = todayIndex / 7
                    val c = todayIndex % 7
                    val x = c * cellWidth
                    val y = r * cellHeight
                    val blue = AppConfig.CalendarCurrentDayBorderColor
                    val thickStroke = AppConfig.CalendarCurrentDayBorderWidth.toPx()

                    // Draw inner border
                    drawRect(
                        color = blue,
                        topLeft = Offset(x + thickStroke / 2, y + thickStroke / 2),
                        size = Size(cellWidth - thickStroke, cellHeight - thickStroke),
                        style = Stroke(width = thickStroke)
                    )

                    // Also draw the grid lines in blue for this cell to ensure full coverage
                    val stroke = 0.5.dp.toPx()
                    drawLine(blue, Offset(x, y), Offset(x + cellWidth, y), stroke)
                    drawLine(blue, Offset(x, y + cellHeight), Offset(x + cellWidth, y + cellHeight), stroke)
                    drawLine(blue, Offset(x, y), Offset(x, y + cellHeight), stroke)
                    drawLine(blue, Offset(x + cellWidth, y), Offset(x + cellWidth, y + cellHeight), stroke)
                }
            }
        }
    }
}

@Composable
private fun RowScope.CalendarHeaderSection(
    weight: Float,
    onClick: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
