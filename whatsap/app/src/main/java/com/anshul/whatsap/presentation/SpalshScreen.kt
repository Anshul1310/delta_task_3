package com.anshul.whatsap.presentation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anshul.whatsap.R
import com.anshul.whatsap.ui.theme.AppBlack
import com.anshul.whatsap.ui.theme.AppGray
import com.anshul.whatsap.ui.theme.AppGreen

@Composable
fun SplashScreen() {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }





    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBlack)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.whatsapp_icon),
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(100.dp),
                colorFilter = ColorFilter.tint(AppGreen)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "DChat",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AppGreen
            )
        }

        Text(
            text = "Made with ❤ by ANSHUL",
            fontSize = 14.sp,
            color = AppGray,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(Modifier.height(100.dp))
        )
    }
}