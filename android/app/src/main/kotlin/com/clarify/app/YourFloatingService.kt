package com.clarify.app

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.embedding.engine.dart.DartExecutor


class YourFloatingService : Service() {

    private var floatingView: View? = null
    private var backgroundView: View? = null
    private lateinit var windowManager: WindowManager
    //private var flutterEngine: FlutterEngine? = null
    private val LOADING_EMOJI = "\uD83E\uDD14";
    private val FAIL_EMOJI = "\uD83E\uDEE0";
    private val SUCCESS_EMOJI = "\uD83E\uDDD0";

    companion object {
        // Create a static Flutter engine that persists across service calls
        private var flutterEngine: FlutterEngine? = null
    }

    private fun ensureFlutterEngine() {
        if (flutterEngine == null) {
            Log.d("YourFloatingService", "Creating persistent Flutter engine")
            flutterEngine = FlutterEngine(this).apply {
                dartExecutor.executeDartEntrypoint(
                    DartExecutor.DartEntrypoint.createDefault()
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra("extra_text") ?: "No text"

        if (floatingView != null) {
            Log.d("YourFloatingService", "Floating view already exists, updating content.")
            callDartMethodForMessage(text) // Ensure this line executes
            return START_STICKY
        }

        Log.d("YourFloatingService", "Creating floating view with text: $text")
        createFloatingView(text)
        return START_STICKY
    }

    private fun updateFloatingView(message: String, emoji: String) {
        val statusView = floatingView?.findViewById<TextView>(R.id.floating_status)
        if (statusView != null) {
            statusView.text = emoji
        } else {
            Log.e("YourFloatingService", "Status TextView is null. Skipping emoji update.")
        }

        if (message != "")
        {
            val responseView = floatingView?.findViewById<TextView>(R.id.floating_text)
            if (responseView != null) {
                responseView.text = message
            } else {
                Log.e("YourFloatingService", "Response TextView is null. Skipping text update.")
            }
        }
    }

    private fun callDartMethod(methodName: String, message: String, showMoreButton : Boolean = false)
    {
        Log.d("YourFloatingService", "Attempting to call Dart method '$methodName' with message: $message")

        // Hide the 'more' button - only noticeable when we click it
        val moreButton = floatingView!!.findViewById<Button>(R.id.floating_more)
        moreButton.visibility = View.GONE
        updateFloatingView("", LOADING_EMOJI)

        flutterEngine?.let { engine ->
            val methodChannel = MethodChannel(engine.dartExecutor.binaryMessenger, "com.clarify.app/floating")
            methodChannel.invokeMethod(methodName, message, object : MethodChannel.Result {
                override fun success(result: Any?) {
                    Log.d("YourFloatingService", "Dart method '$methodName' succeeded with result: $result")
                    updateFloatingView(result.toString(), SUCCESS_EMOJI)

                    if (showMoreButton)
                    {
                        val moreButton = floatingView!!.findViewById<Button>(R.id.floating_more)
                        moreButton.visibility = View.VISIBLE
                    }
                }
                override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
                    Log.d("YourFloatingService", "Dart method '$methodName' error: $errorCode - $errorMessage")
                    updateFloatingView("Got an error:\n$errorMessage", FAIL_EMOJI)
                }
                override fun notImplemented() {
                    Log.d("YourFloatingService", "Dart method '$methodName' not implemented")
                    updateFloatingView("Method not implemented", FAIL_EMOJI)
                }
            })
        }
    }

    private fun callDartMethodForMessage(message: String) {
        callDartMethod("handleMessage", message, showMoreButton = true)
    }

    private fun callDartMethodForMore(message: String) {
        callDartMethod("handleMoreDetails",message)
    }

    private fun createFloatingView(text: String) {

        ensureFlutterEngine()
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
        textView.text = "Looking up '$text'..."

        // Add a close button
        val closeButton = floatingView!!.findViewById<Button>(R.id.floating_close)
        closeButton.setOnClickListener {
            stopSelf() // Stop the service and remove the floating view
        }

        val moreButton = floatingView!!.findViewById<Button>(R.id.floating_more)
        moreButton.setOnClickListener {
            callDartMethodForMore(text)
        }

        // Configure layout params for the floating view
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        layoutParams.x = 0
        layoutParams.y = 100

        // Add the floating view to the window
        windowManager.addView(floatingView, layoutParams)
        callDartMethodForMessage(text) // Ensure this line executes
        Log.d("YourFloatingService", "dart method called")
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
