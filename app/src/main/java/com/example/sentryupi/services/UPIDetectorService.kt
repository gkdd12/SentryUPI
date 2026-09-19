package com.example.sentryupi.services

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class UPIDetectorService : AccessibilityService() {

    private lateinit var overlayManager: OverlayManager
    private var currentPackageName: String = ""
    companion object{
        private const val TAG = "SentryUPI_Service"
        //List of target UPI apps package names
        private val TargetUPIApps = setOf(
            "com.phonepe.app", //this is phonepe app
            "net.one97.paytm", //paytm app
            "com.google.android.apps.nbu.paisa.user", //Google pay
            "in.org.npci.upiapp"  //BHIM
        )

        // System packages that pop up on top of apps (Keyboard, Fingerprint, Biometrics)
        private val SystemIgnoredPackages = setOf(
            "com.android.systemui",
            "com.google.android.inputmethod.latin",
            "com.samsung.android.honeyboard",
            "com.miui.securitycenter"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

            val newPackage = event.packageName?.toString() ?: return

            // Ignore transient system popups like keyboard or fingerprint dialogs
            if (SystemIgnoredPackages.contains(newPackage)) return

            if (newPackage != currentPackageName) {
                currentPackageName = newPackage

                if (TargetUPIApps.contains(currentPackageName)) {
                    Log.d(TAG, "Target UPI App Active: $currentPackageName")
                    overlayManager.showOverlay("#FF0000") // Show glowing border
                } else {
                    Log.d(TAG, "Non-UPI App Active: $currentPackageName")
                    overlayManager.hideOverlay() // Hide when leaving UPI app
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
        overlayManager.hideOverlay()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = OverlayManager(this)
        Log.d(TAG, "SentryUpi Accessibility Service Connected")
    }
}