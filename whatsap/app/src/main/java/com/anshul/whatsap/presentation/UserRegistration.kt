package com.anshul.whatsap.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.anshul.whatsap.ui.theme.AppBlack
import com.anshul.whatsap.ui.theme.AppDarkGray
import com.anshul.whatsap.ui.theme.AppGray
import com.anshul.whatsap.ui.theme.AppGreen
import com.anshul.whatsap.ui.theme.AppWhite

@Composable
fun UserRegistration() {
    var selectedCountry by remember { mutableStateOf("India") }
    var expanded by remember { mutableStateOf(false) }
    var countryCode by remember { mutableStateOf("+91") }
    var phoneNumber by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBlack)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Enter your Phone Number",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppGreen
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "WhatsApp need to verify your phone number", color = AppWhite)
            Text(text = "  what's my", color = AppGreen, fontWeight = FontWeight.SemiBold)
        }
        Text(text = "number", color = AppGreen, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(onClick = { expanded = true }) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 66.dp)) {
                Text(text = selectedCountry, modifier = Modifier.align(Alignment.Center), color = AppWhite)
                Image(
                    contentDescription = "",
                    modifier = Modifier.align(Alignment.CenterEnd),
                    painter = painterResource(android.R.drawable.arrow_down_float)
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 66.dp), color = AppGreen)
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("India", "China", "Canada").forEach { country ->
                DropdownMenuItem(
                    text = { Text(text = country, color = AppWhite) },
                    onClick = {
                        selectedCountry = country
                        expanded = false
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = countryCode,
                onValueChange = { countryCode = it },
                modifier = Modifier.weight(0.3f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = AppGreen,
                    focusedTextColor = AppWhite,
                    unfocusedTextColor = AppWhite
                )
            )
            Spacer(modifier = Modifier.width(10.dp))
            TextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                placeholder = { Text(text = "Phone Number", fontSize = 14.sp, color = AppGray) },
                modifier = Modifier.weight(0.7f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = AppGreen,
                    focusedTextColor = AppWhite,
                    unfocusedTextColor = AppWhite
                )
            )
        }
        Spacer(modifier = Modifier.height(15.dp))
        Text(text = "Carrier Charges may apply", color = AppGray)
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
            shape = RoundedCornerShape(5.dp)
        ) {
            Text(text = "Continue", color = AppBlack, fontWeight = FontWeight.Bold)
        }
    }
}
