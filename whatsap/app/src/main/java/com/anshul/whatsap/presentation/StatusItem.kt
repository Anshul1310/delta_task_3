package com.anshul.whatsap.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anshul.whatsap.R
import com.anshul.whatsap.ui.theme.AppGray
import com.anshul.whatsap.ui.theme.AppGreen
import com.anshul.whatsap.ui.theme.AppWhite

@Composable
fun MyStatus() {
    Row {
        Box(modifier = Modifier.size(80.dp)) {
            Image(
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
                painter = painterResource(R.drawable.boy),
                contentDescription = ""
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(AppGreen, CircleShape)
                    .clip(CircleShape)
            ) {
                Image(
                    imageVector = Icons.Rounded.Add,
                    colorFilter = ColorFilter.tint(Color.Black),
                    contentDescription = "",
                    modifier = Modifier.padding(3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.align(Alignment.CenterVertically)) {
            Text(text = "My Status", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = AppWhite)
            Text(text = "Tap to add Status Update", color = AppGray)
        }
    }
}

@Composable
fun MyStatusItem(statusData: StatusData) {
    Row {
        Box(modifier = Modifier.size(60.dp)) {
            Image(
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
                painter = painterResource(R.drawable.boy),
                contentDescription = ""
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.align(Alignment.CenterVertically)) {
            Text(text = statusData.name, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = AppWhite)
            Text(text = statusData.time, color = AppGray, fontSize = 13.sp)
        }
    }
}

data class StatusData(val image: Int, val name: String, val time: String)