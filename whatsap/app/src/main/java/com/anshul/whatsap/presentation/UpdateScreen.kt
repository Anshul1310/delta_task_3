package com.anshul.whatsap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anshul.whatsap.R
import com.anshul.whatsap.ui.theme.AppBlack
import com.anshul.whatsap.ui.theme.AppDarkGray
import com.anshul.whatsap.ui.theme.AppGray
import com.anshul.whatsap.ui.theme.AppGreen
import com.anshul.whatsap.ui.theme.AppWhite

@Composable
fun UpdateScreen(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val scrollState = rememberScrollState()
    var isdotOpen by remember { mutableStateOf(false) }
    var searchedText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val communitiesModel = listOf(
        CommunityItemModel("Anshul", 2738, true),
        CommunityItemModel("Anshul", 2738, false),
        CommunityItemModel("Anshul", 2738, false),
        CommunityItemModel("Anshul", 2738, false),
        CommunityItemModel("Anshul", 2738, false),
        CommunityItemModel("Anshul", 2738, false),
        CommunityItemModel("Anshul", 2738, true)
    )
    val sampleStatusdata = listOf(
        StatusData(R.drawable.boy, "Anshul Negi", "10:00 pm"),
        StatusData(R.drawable.boy, "Anshul Negi", "10:00 pm"),
        StatusData(R.drawable.boy, "Anshul Negi", "10:00 pm"),
        StatusData(R.drawable.boy, "Anshul Negi", "10:00 pm")
    )
    Scaffold(
        containerColor = AppBlack,
        bottomBar = { BottomNavigation(selectedTab = selectedTab, onTabSelected = onTabSelected) },
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                containerColor = AppGreen,
                onClick = {},
                contentColor = AppBlack
            ) {
                Icon(contentDescription = "", painter = painterResource(R.drawable.baseline_photo_camera_24))
            }
        }
    ) {
        Column(modifier = Modifier.padding(it).background(AppBlack)) {
            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    TextField(
                        value = searchedText,
                        modifier = Modifier.fillMaxWidth(),
                        onValueChange = { searchedText = it },
                        placeholder = { Text(text = "Search", fontSize = 14.sp, color = AppGray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = AppGreen,
                            focusedTextColor = AppWhite,
                            unfocusedTextColor = AppWhite
                        )
                    )
                    IconButton(
                        onClick = { isSearching = false },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "",
                            tint = AppWhite
                        )
                    }
                }
            } else {
                TopBar({ isSearching = true }, { isdotOpen = !isdotOpen })
            }

            Row(modifier = Modifier.fillMaxWidth().align(Alignment.End)) {
                DropdownMenu(expanded = isdotOpen, onDismissRequest = { isdotOpen = false }) {
                    DropdownMenuItem(text = { Text("Anshul", color = AppWhite) }, onClick = { isdotOpen = false })
                    DropdownMenuItem(text = { Text("Anuj", color = AppWhite) }, onClick = { isdotOpen = false })
                    DropdownMenuItem(text = { Text("Yashveer", color = AppWhite) }, onClick = { isdotOpen = false })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(10.dp)
            ) {
                Text(text = "Status", fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = AppWhite)
                MyStatus()
                sampleStatusdata.forEach { item ->
                    Spacer(modifier = Modifier.height(16.dp))
                    MyStatusItem(item)
                }
                Spacer(modifier = Modifier.fillMaxWidth().height(16.dp))
                HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = AppDarkGray)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Community", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = AppWhite)
                Text(text = "By joining the communities you get the info regarding communities in your area", color = AppGray)
                Text(text = "Find Channels to follow", color = AppGreen)
                communitiesModel.forEach { item ->
                    Spacer(modifier = Modifier.height(16.dp))
                    CommunityItem(item)
                }
            }
        }
    }
}
