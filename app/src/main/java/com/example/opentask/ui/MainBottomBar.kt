package com.example.opentask.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

@Composable
fun MainBottomBar(
    selectedTabIndex: Int,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth()
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
