package com.example.sentryupi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class PauseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetPackage = intent.getStringExtra("TARGET_PACKAGE") ?: "com.phonepe.app"

        setContent {
            var secondsLeft by remember { mutableStateOf(5) }

            LaunchedEffect(Unit) {
                while (secondsLeft > 0) {
                    delay(1000)
                    secondsLeft--
                }

                // Launch the target UPI app directly after the 5-second countdown completes
                val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(launchIntent)
                }
                finish() // Closes pause screen
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF8B0000))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🚨 RED ZONE ALERT",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Your account balance is in the RED zone.\nTake 5 seconds to rethink this payment.",
                        fontSize = 18.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "$secondsLeft",
                        fontSize = 80.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Yellow
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Opening payment app in $secondsLeft seconds...",
                        fontSize = 14.sp,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}