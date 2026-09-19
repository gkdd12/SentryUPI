package com.example.sentryupi.services

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

class OverlayManager(private val context: Context) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    fun showOverlay(colorHex: String = "#FF0000") {
        if (overlayView != null) return // Already showing, prevent blinking

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, // Allows drawing past status bar edges
            PixelFormat.TRANSLUCENT
        )

        // Force overlay to draw inside the camera cutout/notch area
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutParams.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        layoutParams.gravity = Gravity.CENTER

        // Create an 8dp (16px) glowing stroke border frame
        val borderDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT) // Middle remains clear for interaction
            setStroke(16, Color.parseColor(colorHex)) // Red glowing border
        }

        val frameLayout = FrameLayout(context).apply {
            background = borderDrawable
        }

        overlayView = frameLayout

        try {
            windowManager.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideOverlay() {
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                overlayView = null
            }
        }
    }
}