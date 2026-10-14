package com.example.sentryupi.services

import android.accessibilityservice.AccessibilityService
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

class UPIDetectorService : AccessibilityService() {

    private var lastActivePackage: String = ""
    private lateinit var repository: BudgetRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO)

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

        // Ignore SentryUPI and PauseActivity so they don't trigger recursive loops
        if (activePackage == packageName || activePackage.contains("sentryupi")) return

        if (activePackage != lastActivePackage) {
            lastActivePackage = activePackage

            if (TargetUPIApps.contains(activePackage)) {
                Log.d(TAG, "🟢 Target UPI App Launched: $activePackage")
                evaluateZoneAndRespond(activePackage)
            }
        }
    }

    private fun evaluateZoneAndRespond(activePackage: String) {
        serviceScope.launch {
            val config = repository.budgetConfigFlow.first()
            val currentZone = config.getZone()

            Log.d(TAG, "Budget Config Loaded. Current Zone: ${currentZone.name}")

            when (currentZone) {
                BudgetZone.GREEN -> {
                    Log.d(TAG, "Green Zone: Safe spending. No alerts.")
                }
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
}