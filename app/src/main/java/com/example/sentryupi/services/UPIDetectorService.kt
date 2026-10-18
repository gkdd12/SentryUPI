package com.example.sentryupi.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.example.sentryupi.PauseActivity
import com.example.sentryupi.data.BudgetConfig
import com.example.sentryupi.data.BudgetRepository
import com.example.sentryupi.data.BudgetZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class UPIDetectorService : AccessibilityService() {

    private var lastActivePackage: String = ""
    private lateinit var repository: BudgetRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var lastProcessedTransactionTime: Long = 0L
    private val successKeywords = listOf("Paid successfully", "Payment Successful", "Transfer Successful", "Paid ₹")
    private val amountRegex = Pattern.compile("(?:₹|Rs\\.?)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
    private val dateTimeRegex = Pattern.compile("(\\d{1,2}\\s+[A-Za-z]+\\s+\\d{4}(?:\\s+at|\\s*,)?\\s+\\d{1,2}:\\d{2}\\s*(?:AM|PM)?)", Pattern.CASE_INSENSITIVE)
    private var lastProcessedTransactionSignature: String = ""

    companion object {
        private const val TAG = "SentryUPI_Service"
        private const val CHANNEL_ID = "sentry_upi_yellow_channel"

        private val TargetUPIApps = setOf(
            "com.phonepe.app",
            "net.one97.paytm",
            "com.google.android.apps.nbu.paisa.user",
            "in.org.npci.upiapp"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = BudgetRepository(this)
        createNotificationChannel()
        Log.d(TAG, "SentryUPI Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val activePackage = rootInActiveWindow?.packageName?.toString()
            ?: event.packageName?.toString()
            ?: return

        // 1. Ignore SentryUPI
        if (activePackage == packageName || activePackage.contains("sentryupi")) return

        // 2. IGNORE System UI (Notification Shade) without resetting lastActivePackage
        if (activePackage == "com.android.systemui") return

        if (TargetUPIApps.contains(activePackage)) {
            if (activePackage != lastActivePackage) {
                lastActivePackage = activePackage
                Log.d(TAG, "🟢 Target UPI App Launched: $activePackage")
                evaluateZoneAndRespond(activePackage)
            }

            val rootNode = rootInActiveWindow ?: return
            scanForPaymentSuccess(rootNode)
        } else {
            lastActivePackage = activePackage
        }
    }

    private fun evaluateZoneAndRespond(activePackage: String) {
        serviceScope.launch {
            val config = repository.budgetConfigFlow.first()

            // 1. Check for 7-Day Re-Verification Prompt
            if (config.isVerificationDue()) {
                sendVerificationReminderNotification()
            }

            // 2. Evaluate Zone Status
            val currentZone = config.getZone()
            Log.d(TAG, "Budget Config Loaded. Current Zone: ${currentZone.name}")

            when (currentZone) {
                BudgetZone.GREEN -> Log.d(TAG, "Green Zone: Safe spending.")
                BudgetZone.YELLOW -> {
                    triggerHapticPulse()
                    sendYellowNotification(config)
                }
                BudgetZone.RED -> {
                    triggerHapticPulse()
                    launchPauseActivity(activePackage)
                }
            }
        }
    }

    private fun launchPauseActivity(packageName: String) {
        val intent = Intent(this, PauseActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("TARGET_PACKAGE", packageName)
        }
        startActivity(intent)
    }

    private fun sendYellowNotification(config: BudgetConfig) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ SentryUPI Yellow Alert")
            .setContentText("Current Balance: ₹${config.currentBalance}. You are approaching your limit!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setTimeoutAfter(4000)
            .build()

        notificationManager.notify(6001, notification)
    }

    private fun triggerHapticPulse() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                val timings = longArrayOf(0, 150, 100, 150)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                val timings = longArrayOf(0, 150, 100, 150)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(300)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SentryUPI Yellow Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }

    private fun scanForPaymentSuccess(node: AccessibilityNodeInfo) {
        val allTexts = mutableListOf<String>()
        collectNodeText(node, allTexts)

        val fullScreenText = allTexts.joinToString(" ")

        val isLiveSuccessScreen = successKeywords.any { keyword -> fullScreenText.contains(keyword, ignoreCase = true) }
                && (fullScreenText.contains("Done", ignoreCase = true) || fullScreenText.contains("Share", ignoreCase = true))

        if (isLiveSuccessScreen) {
            val amountMatcher = amountRegex.matcher(fullScreenText)
            if (amountMatcher.find()) {
                val rawAmountStr = amountMatcher.group(1)?.replace(",", "") ?: return
                val parsedAmount = rawAmountStr.toDoubleOrNull() ?: return

                // Extract date/time from screen (e.g. "21 September 2026 at 05:19 PM")
                val dateTimeMatcher = dateTimeRegex.matcher(fullScreenText)
                val dateTimeStr = if (dateTimeMatcher.find()) dateTimeMatcher.group(1) else ""

                // Construct unique signature for this receipt
                val currentSignature = "$parsedAmount-$dateTimeStr"

                // Skip if this exact transaction signature was already processed
                if (currentSignature == lastProcessedTransactionSignature) {
                    return
                }

                if (parsedAmount > 0) {
                    lastProcessedTransactionSignature = currentSignature
                    lastProcessedTransactionTime = System.currentTimeMillis()
                    Log.d(TAG, "💸 Unique Payment Detected! Signature: $currentSignature")

                    serviceScope.launch {
                        val updatedBalance = repository.deductBalance(parsedAmount)
                        sendDeductionNotification(parsedAmount, updatedBalance)
                    }
                }
            }
        }
    }

    private fun sendDeductionNotification(deducted: Double, remaining: Double) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("💸 Balance Auto-Updated")
            .setContentText("Deducted ₹$deducted. New Balance: ₹$remaining")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(7001, notification)
    }

    private fun sendVerificationReminderNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📅 7-Day Balance Check")
            .setContentText("It's been 7 days! Please open SentryUPI to verify your account balance.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(7002, notification)
    }

    private fun collectNodeText(node: AccessibilityNodeInfo?, textList: MutableList<String>) {
        if (node == null) return
        if (!node.text.isNullOrEmpty()) {
            textList.add(node.text.toString())
        }
        for (i in 0 until node.childCount) {
            collectNodeText(node.getChild(i), textList)
        }
    }
}