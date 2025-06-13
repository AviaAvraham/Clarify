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
import android.widget.ImageButton
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import com.clarify.app.AIClient
import android.app.Activity
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class YourFloatingService : Service() {

    private var floatingView: View? = null
    private var backgroundView: View? = null
    private lateinit var windowManager: WindowManager
    private val LOADING_EMOJI = "\uD83E\uDD14";
    private val FAIL_EMOJI = "\uD83E\uDEE0";
    private val SUCCESS_EMOJI = "\uD83E\uDDD0";
    private val aiClient = AIClient()

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

    private fun callDartMethod(message: String, showMoreButton : Boolean = false)
    {
        // time measurement
         val startTime = System.currentTimeMillis()
        Log.d("YourFloatingService", "Attempting to call AI with message: $message")

        // Hide the 'more' button - only noticeable when we click it
        val moreButton = floatingView!!.findViewById<Button>(R.id.floating_more)
        moreButton.visibility = View.GONE
        val reloadButton = floatingView!!.findViewById<ImageButton>(R.id.retry_button)
        // reloadButton.visibility = View.GONE
        reloadButton.apply {
            clearAnimation()
            animate().cancel()

            visibility = View.GONE
            alpha = 0f
            isClickable = false // Disable click until the animation ends
            // isEnabled = false // Disable the button to prevent all touch events
        }
        updateFloatingView("", LOADING_EMOJI)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                println("Calling AI model...")
                // if showMoreButton is true we want a non-detailed response
                // also, if the server fails, callModel will throw
                val response = aiClient.callModel(message, !showMoreButton)
                println("AI Response: $response")

                val endTime = System.currentTimeMillis()
                val elapsedTime = endTime - startTime

                updateFloatingView(response /* + "(in " + elapsedTime + "ms)"*/, SUCCESS_EMOJI)

                if (showMoreButton)
                {
                    moreButton.visibility = View.VISIBLE
                }
                // Show the reload button after 5s delay, to avoid being rate limited
                reloadButton.visibility = View.VISIBLE
                reloadButton.animate().alpha(1f).setDuration(5000).withEndAction {
                    reloadButton.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .setDuration(150)
                        .withEndAction {
                            reloadButton.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(150)
                                .withEndAction {
                                    reloadButton.isClickable = true
                                }
                                .start()
                        }
                        .start()
                }.start()
                Log.d("YourFloatingService", "AI call execution time: $elapsedTime ms")
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                println("Error calling AI: $errorMessage")
                updateFloatingView("Got an error:\n$errorMessage", FAIL_EMOJI)
                Handler(Looper.getMainLooper()).postDelayed({
                    reloadButton.visibility = View.VISIBLE
                }, 3000) // Show the reload button after 3 seconds
            }
        }
    }

    private fun callDartMethodForMessage(message: String) {
        callDartMethod(message, showMoreButton = true)
    }

    private fun callDartMethodForMore(message: String) {
        callDartMethod(message)
    }

    private fun createFloatingView(text: String) {

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

        val reloadButton = floatingView!!.findViewById<ImageButton>(R.id.retry_button)
        reloadButton.setOnClickListener {
            if (moreButton.visibility == View.VISIBLE) {
                callDartMethodForMessage(text)
            }
            else
            {
                callDartMethodForMore(text)
            }
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
