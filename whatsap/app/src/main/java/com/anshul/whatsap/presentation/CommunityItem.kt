package com.anshul.whatsap.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anshul.whatsap.R
import com.anshul.whatsap.ui.theme.AppBlack
import com.anshul.whatsap.ui.theme.AppGray
import com.anshul.whatsap.ui.theme.AppGreen
import com.anshul.whatsap.ui.theme.AppWhite

@Composable
fun CommunityItem(communityItemModel: CommunityItemModel) {
    val strFollowing = if (communityItemModel.isFollowed) "Followed" else "Follow"
    Box {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(60.dp)) {
                Image(
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                    painter = painterResource(R.drawable.boy),
                    contentDescription = ""
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                Text(text = communityItemModel.Name, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = AppWhite)
                Text(text = "${communityItemModel.member} members", color = AppGray, fontSize = 13.sp)
            }
        }

        Button(
            onClick = {},
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (communityItemModel.isFollowed) AppGray else AppGreen,
                contentColor = AppBlack
            )
        ) {
            Text(text = strFollowing)
        }
    }
}

data class CommunityItemModel(val Name: String, val member: Int, val isFollowed: Boolean)
