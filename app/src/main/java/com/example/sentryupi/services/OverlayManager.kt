package com.example.sentryupi.services

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

class OverlayManager(private val context: Context) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    companion object {
        private const val TAG = "SentryUPI_Overlay"
        private const val BORDER_THICKNESS_PX = 20 // Border thickness in pixels
    }

    fun showOverlay(colorHex: String = "#FF0000") {
        if (overlayView != null) return // Already active

        if (!Settings.canDrawOverlays(context)) {
            Log.e(TAG, "❌ Overlay permission missing")
            return
        }

        // Renamed to windowLayoutParams to avoid collision with view.layoutParams
        val windowLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            windowLayoutParams.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        windowLayoutParams.gravity = Gravity.CENTER

        val parsedColor = Color.parseColor(colorHex)
        val rootLayout = FrameLayout(context)

        // 1. Top Edge
        val topEdge = View(context).apply { setBackgroundColor(parsedColor) }
        val topParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            BORDER_THICKNESS_PX,
            Gravity.TOP
        )

        // 2. Bottom Edge
        val bottomEdge = View(context).apply { setBackgroundColor(parsedColor) }
        val bottomParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            BORDER_THICKNESS_PX,
            Gravity.BOTTOM
        )

        // 3. Left Edge
        val leftEdge = View(context).apply { setBackgroundColor(parsedColor) }
        val leftParams = FrameLayout.LayoutParams(
            BORDER_THICKNESS_PX,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.START
        )

        // 4. Right Edge
        val rightEdge = View(context).apply { setBackgroundColor(parsedColor) }
        val rightParams = FrameLayout.LayoutParams(
            BORDER_THICKNESS_PX,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.END
        )

        // Add views with their respective layout params
        rootLayout.addView(topEdge, topParams)
        rootLayout.addView(bottomEdge, bottomParams)
        rootLayout.addView(leftEdge, leftParams)
        rootLayout.addView(rightEdge, rightParams)

        try {
            windowManager.addView(rootLayout, windowLayoutParams)
            overlayView = rootLayout
            Log.d(TAG, "✅ Edge-to-edge border added successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to attach overlay: ${e.message}")
            overlayView = null
        }
    }

    fun hideOverlay() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
                Log.d(TAG, "Red border removed.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay view: ${e.message}")
            } finally {
                overlayView = null
            }
        }
    }
}