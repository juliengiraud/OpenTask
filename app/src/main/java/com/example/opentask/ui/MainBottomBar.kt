package com.example.opentask.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

@Composable
fun MainBottomBar(
    selectedTabIndex: Int,
    onTabClick: (Int) -> Unit,
    onAddNoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Column container is transparent by default, allowing content to be seen 
    // behind the AddNoteButton area.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.End
    ) {
        // Add button floating above the navigation bar
        AddNoteButton(onClick = onAddNoteClick)
        
        // NavigationBar with its default solid background
        NavigationBar(
            modifier = Modifier.fillMaxWidth()
        ) {
            AppTabs.entries.forEachIndexed { index, tab ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            painterResource(tab.icon),
                            contentDescription = tab.label
                        )
                    },
                    label = { Text(tab.label) },
                    selected = selectedTabIndex == index,
                    onClick = { onTabClick(index) }
                )
            }
        }
    }
}
