package com.anshul.whatsap.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anshul.whatsap.R
import com.anshul.whatsap.helper.ApiClient
import com.anshul.whatsap.ui.theme.AppBlack
import com.anshul.whatsap.ui.theme.AppDarkGray
import com.anshul.whatsap.ui.theme.AppGray
import com.anshul.whatsap.ui.theme.AppGreen
import com.anshul.whatsap.ui.theme.AppWhite
import kotlinx.coroutines.launch

data class SelectableUser(
    val id: String,
    val name: String,
    val isSelected: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(onBack: () -> Unit, onGroupCreated: () -> Unit) {
    var groupName by remember { mutableStateOf("") }
    val users = remember { mutableStateListOf<SelectableUser>() }
    var isLoading by remember { mutableStateOf(true) }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val result = ApiClient.getUsers()
        isLoading = false
        if (result.isSuccess) {
            val userList = result.getOrDefault(emptyList())
            for (userJson in userList) {
                users.add(
                    SelectableUser(
                        id = userJson.getString("id"),
                        name = userJson.getString("name")
                    )
                )
            }
        }
    }

    Scaffold(
        containerColor = AppBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Create Group", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppWhite)
                        val selectedCount = users.count { it.isSelected }
                        if (selectedCount > 0) {
                            Text(
                                text = "$selectedCount members selected",
                                fontSize = 12.sp,
                                color = AppGreen
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBlack
                )
            )
        },
        floatingActionButton = {
            val selectedUsers = users.filter { it.isSelected }
            if (selectedUsers.isNotEmpty() && groupName.isNotBlank()) {
                FloatingActionButton(
                    onClick = {
                        if (isCreating) return@FloatingActionButton
                        isCreating = true
                        errorMessage = ""
                        val memberIds = selectedUsers.map { it.id }
                        coroutineScope.launch {
                            val result = ApiClient.createRoom(groupName, memberIds)
                            isCreating = false
                            if (result.isSuccess) {
                                onGroupCreated()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Failed to create group"
                            }
                        }
                    },
                    containerColor = AppGreen,
                    contentColor = AppBlack
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            color = AppBlack,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Create Group"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = groupName,
                onValueChange = { groupName = it },
                placeholder = { Text("Group Name", color = AppGray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppDarkGray,
                    unfocusedContainerColor = AppDarkGray,
                    cursorColor = AppGreen,
                    focusedIndicatorColor = AppGreen,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = AppWhite,
                    unfocusedTextColor = AppWhite
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage.isNotEmpty()) {
                Text(text = errorMessage, color = Color.Red, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "Select Members",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppGray
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppGreen)
                }
            } else if (users.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No users available", color = AppGray, fontSize = 14.sp)
                }
            } else {
                LazyColumn {
                    items(users.size) { index ->
                        val user = users[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    users[index] = user.copy(isSelected = !user.isSelected)
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.boy),
                                contentDescription = user.name,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = user.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                color = AppWhite
                            )
                            Checkbox(
                                checked = user.isSelected,
                                onCheckedChange = { isChecked ->
                                    users[index] = user.copy(isSelected = isChecked)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AppGreen,
                                    checkmarkColor = AppBlack,
                                    uncheckedColor = AppGray
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
