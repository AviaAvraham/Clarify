package com.example.untitled

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.plugin.common.MethodChannel
import android.util.Log


class YourFloatingService : Service() {

    private var floatingView: View? = null
    private var backgroundView: View? = null
    private lateinit var windowManager: WindowManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra("extra_text") ?: "No text"

        if (floatingView != null) {
            Log.d("YourFloatingService", "Floating view already exists, updating content.")
            callDartMethod(text) // Ensure this line executes
            return START_NOT_STICKY
        }

        Log.d("YourFloatingService", "Creating floating view with text: $text")
        createFloatingView(text)
        return START_NOT_STICKY
    }

    private fun callDartMethod(message: String) {
        Log.d("YourFloatingService","Attempting to call Dart method with message: $message")
        val flutterEngine = FlutterEngineCache.getInstance()["flutter_engine_id"]
        if (flutterEngine != null) {
            Log.d("YourFloatingService", "FlutterEngine found, calling method")
            val methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "com.example.untitled/floating")
            methodChannel.invokeMethod("handleMessage", message, object : MethodChannel.Result {
                override fun success(result: Any?) {
                    val textView = floatingView!!.findViewById<TextView>(R.id.floating_text)
                    textView.text = "something went wrong"
                    Log.d("YourFloatingService","Dart method succeeded with result: $result")
                    textView.text = result as String
                }
                override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
                    Log.d("YourFloatingService","Dart method error: $errorCode - $errorMessage")

                    val textView = floatingView!!.findViewById<TextView>(R.id.floating_text)
                    textView.text = "Dart method error: $errorCode - $errorMessage"
                }
                override fun notImplemented() {
                    Log.d("YourFloatingService","Dart method not implemented")
                    val textView = floatingView!!.findViewById<TextView>(R.id.floating_text)
                    textView.text = "Dart method not implemented"
                }
            })
        } else {
            Log.d("YourFloatingService","FlutterEngine is not initialized or cached")
        }
    }

    private fun createFloatingView(text: String) {

        callDartMethod(text) // Ensure this line executes
        Log.d("YourFloatingService", "dart method called")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Add a background view to detect outside clicks
        backgroundView = View(this).apply {
            setBackgroundColor(0x00000000) // Fully transparent
            setOnClickListener {
                stopSelf() // Close the floating window when background is clicked
            }
        }

        val backgroundParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        backgroundParams.gravity = Gravity.TOP or Gravity.START
        windowManager.addView(backgroundView, backgroundParams)

        // Inflate the floating view layout
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.floating_view, null)

        // Set the text
        val textView = floatingView!!.findViewById<TextView>(R.id.floating_text)
        textView.text = text

        // Add a close button
        val closeButton = floatingView!!.findViewById<Button>(R.id.floating_close)
        closeButton.setOnClickListener {
            stopSelf() // Stop the service and remove the floating view
        }

        // Configure layout params for the floating view
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 100
        layoutParams.y = 100

        // Add the floating view to the window
        windowManager.addView(floatingView, layoutParams)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the floating view and background view if they exist
        if (floatingView != null) {
            windowManager.removeView(floatingView)
            floatingView = null
        }
        if (backgroundView != null) {
            windowManager.removeView(backgroundView)
            backgroundView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
