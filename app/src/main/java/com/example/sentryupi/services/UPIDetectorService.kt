package com.example.sentryupi.services

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class UPIDetectorService : AccessibilityService() {
    companion object{
        private const val TAG = "SentryUPI_Service"
        //List of target UPI apps package names
        private val TargetUPIApps = setOf(
            "com.phonepe.app", //this is phonepe app
            "net.one97.paytm", //paytm app
            "com.google.android.apps.nbu.paisa.user", //Google pay
            "in.org.npci.upiapp"  //BHIM
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?){
        if(event == null) return
        // detect when an app window opens or changes state
        if(event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){
            val packageName = event.packageName?.toString()?: return
            if(TargetUPIApps.contains(packageName)){
                Log.d(TAG,"UPI App Launched: $packageName")
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "SentryUpi Accessibility Service Connected")
    }
}