package com.example.sentryupi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                OverlayPermissionScreen(
                    hasPermission = Settings.canDrawOverlays(this),
                    onRequestPermission = { requestOverlayPermission() },
                    onOpenAppDetails = { openAppDetailsPage() }
                )
            }
        }
    }

    private fun requestOverlayPermission() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } catch (e: Exception) {
            openAppDetailsPage()
        }
    }

    private fun openAppDetailsPage() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }
}

@Composable
fun OverlayPermissionScreen(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppDetails: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "SentryUPI Setup",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (hasPermission) "✅ Overlay Permission Granted" else "❌ Overlay Permission Required",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRequestPermission) {
            Text(text = if (hasPermission) "Permission Active" else "Grant Overlay Permission")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(onClick = onOpenAppDetails) {
            Text(text = "Open App Settings (Fallback)")
        }
    }
}