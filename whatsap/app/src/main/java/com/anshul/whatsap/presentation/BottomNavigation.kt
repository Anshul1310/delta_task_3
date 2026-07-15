package com.anshul.whatsap.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anshul.whatsap.R
import com.anshul.whatsap.ui.theme.AppDarkSurface
import com.anshul.whatsap.ui.theme.AppGray
import com.anshul.whatsap.ui.theme.AppGreen

data class BottomNavItem(val label: String, val icon: Int)

@Composable
fun BottomNavigation(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val items = listOf(
        BottomNavItem("Chats", R.drawable.message_4475881),
        BottomNavItem("Updates", R.drawable.update_icon),
        BottomNavItem("Community", R.drawable.communities_icon),
        BottomNavItem("Calls", R.drawable.telephone)
    )

    NavigationBar(containerColor = AppDarkSurface) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppGreen,
                    selectedTextColor = AppGreen,
                    indicatorColor = AppGreen.copy(alpha = 0.12f),
                    unselectedIconColor = AppGray,
                    unselectedTextColor = AppGray
                )
            )
        }
    }
}