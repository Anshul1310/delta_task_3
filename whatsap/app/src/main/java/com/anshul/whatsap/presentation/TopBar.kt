package com.anshul.whatsap.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anshul.whatsap.R
import com.anshul.whatsap.ui.theme.AppGreen
import com.anshul.whatsap.ui.theme.AppWhite

@Composable
fun TopBar(OnSearchClick: () -> Unit, OndotClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "DChat",
            modifier = Modifier.align(Alignment.CenterVertically),
            fontSize = 28.sp,
            color = AppGreen,
            fontWeight = FontWeight.ExtraBold
        )
        Row {
            IconButton(onClick = { OnSearchClick() }) {
                Image(
                    modifier = Modifier.size(22.dp),
                    painter = painterResource(R.drawable.search),
                    contentDescription = "Search",
                    colorFilter = ColorFilter.tint(AppWhite)
                )
            }
            IconButton(onClick = { OndotClick() }) {
                Image(
                    modifier = Modifier.size(22.dp),
                    painter = painterResource(R.drawable.more),
                    contentDescription = "More",
                    colorFilter = ColorFilter.tint(AppWhite)
                )
            }
        }
    }
}