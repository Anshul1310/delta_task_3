package com.anshul.whatsap.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

@Preview(showSystemUi = true)
@Composable

fun ComingSoon(){
    Column(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
        Text(text = "Coming soon", fontSize = 25.sp)
    }
}
