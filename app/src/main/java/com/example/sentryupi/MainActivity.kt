package com.example.sentryupi

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentryupi.data.BudgetConfig
import com.example.sentryupi.data.BudgetRepository
import com.example.sentryupi.data.BudgetZone
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: BudgetRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = BudgetRepository(this)

        setContent {
            var showSettingsScreen by remember { mutableStateOf(false) }

            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                if (showSettingsScreen) {
                    PermissionsScreen(
                        onOpenAccessibilitySettings = { openAccessibilitySettings() },
                        onOpenNotificationSettings = { openNotificationSettings() },
                        onBack = { showSettingsScreen = false }
                    )
                } else {
                    DashboardScreen(
                        repository = repository,
                        onOpenSettings = { showSettingsScreen = true }
                    )
                }
            }
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun openNotificationSettings() {
        val intent = Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            } else {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.parse("package:$packageName")
            }
        }
        startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(repository: BudgetRepository, onOpenSettings: () -> Unit) {
    var validationError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val configState = repository.budgetConfigFlow.collectAsState(initial = BudgetConfig())
    val config = configState.value

    // Toggle state for editing
    var isEditing by remember { mutableStateOf(false) }

    var accountNo by remember(config) { mutableStateOf(config.lastFourDigits) }
    var balance by remember(config) { mutableStateOf(config.currentBalance.toString()) }
    var a1Threshold by remember(config) { mutableStateOf(config.greenYellowThreshold.toString()) }
    var a2Threshold by remember(config) { mutableStateOf(config.yellowRedThreshold.toString()) }
    var minBal by remember(config) { mutableStateOf(config.minBalanceToMaintain.toString()) }

    val currentZone = config.getZone()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SentryUPI Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Permissions Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Zone Indicator Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (currentZone) {
                        BudgetZone.GREEN -> Color(0xFF2E7D32)
                        BudgetZone.YELLOW -> Color(0xFFF57F17)
                        BudgetZone.RED -> Color(0xFFC62828)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "CURRENT STATUS", fontSize = 12.sp, color = Color.White)
                    Text(
                        text = "${currentZone.name} ZONE",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Account & Balance Fields (enabled controlled by isEditing)
            OutlinedTextField(
                value = accountNo,
                onValueChange = { if (it.length <= 4) accountNo = it },
                label = { Text("Bank Account (Last 4 Digits)") },
                enabled = isEditing,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = balance,
                onValueChange = { balance = it },
                label = { Text("Current Balance (₹)") },
                enabled = isEditing,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
            // Optional update if your IDE highlights Divider()
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))

            Text(text = "Zone Thresholds", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = a1Threshold,
                onValueChange = { a1Threshold = it },
                label = { Text("Green / Yellow Boundary (a1)") },
                enabled = isEditing,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = a2Threshold,
                onValueChange = { a2Threshold = it },
                label = { Text("Yellow / Red Boundary (a2)") },
                enabled = isEditing,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = minBal,
                onValueChange = { minBal = it },
                label = { Text("Minimum Balance to Maintain (₹)") },
                enabled = isEditing,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            // Show Validation Error if present
            validationError?.let { errorMsg ->
                Text(
                    text = errorMsg,
                    color = Color.Red,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (!isEditing) {
                Button(
                    onClick = {
                        validationError = null
                        isEditing = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✏️ Edit Details")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            accountNo = config.lastFourDigits
                            balance = config.currentBalance.toString()
                            a1Threshold = config.greenYellowThreshold.toString()
                            a2Threshold = config.yellowRedThreshold.toString()
                            minBal = config.minBalanceToMaintain.toString()
                            validationError = null
                            isEditing = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val parsedA1 = a1Threshold.toDoubleOrNull() ?: 0.0
                            val parsedA2 = a2Threshold.toDoubleOrNull() ?: 0.0
                            val parsedMin = minBal.toDoubleOrNull() ?: 0.0

                            when {
                                parsedA1 <= parsedA2 -> {
                                    validationError = "⚠️ Green/Yellow boundary (a1) must be greater than Yellow/Red boundary (a2)!"
                                }
                                parsedA2 <= parsedMin -> {
                                    validationError = "⚠️ Yellow/Red boundary (a2) must be greater than Minimum Balance!"
                                }
                                else -> {
                                    validationError = null
                                    scope.launch {
                                        repository.saveConfig(
                                            BudgetConfig(
                                                lastFourDigits = accountNo,
                                                currentBalance = balance.toDoubleOrNull() ?: 0.0,
                                                greenYellowThreshold = parsedA1,
                                                yellowRedThreshold = parsedA2,
                                                minBalanceToMaintain = parsedMin
                                            )
                                        )
                                        isEditing = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onOpenAccessibilitySettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Permissions") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            PermissionCard(
                title = "1. Accessibility Service",
                description = "Required to detect when payment apps like PhonePe open.",
                buttonText = "Enable Accessibility",
                onClick = onOpenAccessibilitySettings
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionCard(
                title = "2. Notification Permission",
                description = "Required to show Yellow Zone caution alerts.",
                buttonText = "Enable Notifications",
                onClick = onOpenNotificationSettings
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onClick) {
                Text(text = buttonText)
            }
        }
    }
}