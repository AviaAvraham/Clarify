package com.clarify.ai

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Button
import android.widget.TextView
import android.widget.ImageButton
import android.os.Handler
import android.os.Looper
import android.graphics.Rect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class FloatingService : Service() {
    /*
    If I add a settings menu one day, here are some options:
    * hide bottom buttons (copy, reload, more)
    * detailed response by default (also hides more button)
    * toggle tap outside to close
    * dark mode (could be fun to put different emojis for this)

    * add credits for myself and links (source code, donations, linkedIn, etc.)
     */

    private var floatingView: View? = null
    private lateinit var windowManager: WindowManager
    private val LOADING_EMOJI = "\uD83E\uDD14";
    private val FAIL_EMOJI = "\uD83E\uDEE0";
    private val SUCCESS_EMOJI = "\uD83E\uDDD0";
    private val aiClient = AIClient()
    private var hideBottomButtons = false
    private var detailedResponse: Boolean = false
    private var term = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        term = intent?.getStringExtra("extra_text") ?: "No text" // this will update if the service is already running

        if (floatingView != null) {
            Log.d("FloatingService", "Floating view already exists, updating content.")
            callAI(term) // Ensure this line executes
            return START_STICKY
        }

        Log.d("FloatingService", "Creating floating view with text: $term")
        createFloatingView()
        return START_STICKY
    }

    private fun updateFloatingView(message: String, emoji: String) {
        val statusView = floatingView?.findViewById<TextView>(R.id.floating_status)
        if (statusView != null) {
            statusView.text = emoji
        } else {
            Log.e("FloatingService", "Status TextView is null. Skipping emoji update.")
        }

        if (message != "")
        {
            val responseView = floatingView?.findViewById<TextView>(R.id.floating_text)
            if (responseView != null) {
                responseView.text = "\u200E$message"
            } else {
                Log.e("FloatingService", "Response TextView is null. Skipping text update.")
            }
        }
    }

    private fun callAI(message: String)
    {
        // time measurement
         val startTime = System.currentTimeMillis()
        Log.d("FloatingService", "Attempting to call AI with message: $message")

        // Hide the 'more' button
        val moreButton = floatingView!!.findViewById<Button>(R.id.floating_more)
        moreButton.visibility = View.GONE
        val reloadButton = floatingView!!.findViewById<ImageButton>(R.id.retry_button)
        reloadButton.apply {
            clearAnimation()
            animate().cancel()

            visibility = View.GONE
            alpha = 0f
            isClickable = false // Disable click until the animation ends
            // isEnabled = false // Disable the button to prevent all touch events
        }

        val copyButton = floatingView!!.findViewById<ImageButton>(R.id.copy_button)
        copyButton.visibility = View.GONE
        updateFloatingView("", LOADING_EMOJI)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                println("Calling AI model...")

                // if the server fails, callModel will throw
                val response = aiClient.callModel(message, detailedResponse)
                println("AI Response: $response")

                val endTime = System.currentTimeMillis()
                val elapsedTime = endTime - startTime

                updateFloatingView(response /* + "(in " + elapsedTime + "ms)"*/, SUCCESS_EMOJI)

                if (!detailedResponse && !hideBottomButtons)
                {
                    moreButton.visibility = View.VISIBLE
                }

                // Show the reload button after 5s delay, to avoid being rate limited
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!hideBottomButtons) {
                        reloadButton.visibility = View.VISIBLE
                    }
                    reloadButton.alpha = 0f // Ensure it's transparent before fading in

                    reloadButton.animate()
                        .alpha(1f)
                        .setDuration(300) // Short fade in (300ms)
                        .withEndAction {
                            reloadButton.animate()
                                .scaleX(1f)
                                .scaleY(1f)
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
                        }
                        .start()
                }, 3000) // Delay for 3 seconds

                if (!hideBottomButtons)
                    copyButton.visibility = View.VISIBLE
                Log.d("FloatingService", "AI call execution time: $elapsedTime ms")
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                println("Error calling AI: $errorMessage")
                updateFloatingView("Got an error:\n$errorMessage", FAIL_EMOJI)
                Handler(Looper.getMainLooper()).postDelayed({
                    moreButton.visibility = View.INVISIBLE // if made visible (why?), use hideBottomButtons
                    copyButton.visibility = View.VISIBLE
                    reloadButton.alpha = 1f
                    reloadButton.visibility = View.VISIBLE
                    reloadButton.isClickable = true
                }, 3000) // Show the reload button after 3 seconds
            }
        }
    }

    private fun createFloatingView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Inflate the floating view layout
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.floating_view, null)

        setupButtons()

        // Configure layout params for the floating view
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        layoutParams.x = 0
        layoutParams.y = 100

        setupScaleAndDrag(layoutParams)

        // Add the floating view to the window
        windowManager.addView(floatingView, layoutParams)
        callAI(term) // Ensure this line executes
        Log.d("FloatingService", "AI called")
    }

    private fun setupButtons() {
        val textView = floatingView!!.findViewById<TextView>(R.id.floating_text)
        textView.text = "Looking up '$term'..."

        val closeButton = floatingView!!.findViewById<Button>(R.id.floating_close)
        closeButton.setOnClickListener { stopSelf() }

        val moreButton = floatingView!!.findViewById<Button>(R.id.floating_more)
        moreButton.setOnClickListener {
            detailedResponse = true
            callAI(term)
        }

        val reloadButton = floatingView!!.findViewById<ImageButton>(R.id.retry_button)
        reloadButton.setOnClickListener {
            callAI(term)
        }

        val copyButton = floatingView!!.findViewById<ImageButton>(R.id.copy_button)
        copyButton.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText(null, textView.text.toString())
            clipboard.setPrimaryClip(clip)

            // Copy button animation
            copyButton.animate()
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(150)
                .withEndAction {
                    copyButton.setImageResource(R.drawable.ic_check)
                    copyButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .start()
                }
                .start()

            Handler(Looper.getMainLooper()).postDelayed({
                copyButton.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(150)
                    .withEndAction {
                        copyButton.setImageResource(R.drawable.ic_copy)
                        copyButton.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .start()
                    }
                    .start()
            }, 1500)
        }
    }

    private fun setupScaleAndDrag(layoutParams: WindowManager.LayoutParams) {
        floatingView!!.post {
            val minScale = 0.5f
            val maxScale = 1.0f // no scaling above initial size
            var currentScale = 1f
            val originalWidth = floatingView!!.width
            // val originalHeight = floatingView!!.height

            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f

            val scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    currentScale *= detector.scaleFactor
                    currentScale = currentScale.coerceIn(minScale, maxScale)
                    println("Current scale: $currentScale")

                    floatingView!!.scaleX = currentScale
                    floatingView!!.scaleY = currentScale

                    /*
                    Things here work weird.
                    Because we scale the view, we need to allow the user touch outside the view.
                    For the width - it works fine, but the height is a bit weird and becomes too small

                    Instead, and perhaps this is better, we hide the buttons, which allow for
                    more space. Weird, but whatever works
                     */
                    val scaledWidth = (originalWidth * currentScale).toInt()
                    layoutParams.width = scaledWidth
                    windowManager.updateViewLayout(floatingView, layoutParams)

                    if (currentScale < 1.0f) {
                        floatingView!!.findViewById<Button>(R.id.floating_more).visibility = View.GONE
                        floatingView!!.findViewById<ImageButton>(R.id.copy_button).visibility = View.GONE
                        floatingView!!.findViewById<ImageButton>(R.id.retry_button).visibility = View.GONE
                        hideBottomButtons = true
                    } else {
                        if (!detailedResponse)
                            floatingView!!.findViewById<Button>(R.id.floating_more).visibility = View.VISIBLE
                        floatingView!!.findViewById<ImageButton>(R.id.copy_button).visibility = View.VISIBLE
                        floatingView!!.findViewById<ImageButton>(R.id.retry_button).visibility = View.VISIBLE
                        hideBottomButtons = false
                    }

                    return true
                }
            })

            floatingView!!.setOnTouchListener { _, event ->
                scaleGestureDetector.onTouchEvent(event)

                if (scaleGestureDetector.isInProgress) {
                    // If scaling is in progress, ignore other touch events
                    return@setOnTouchListener true
                }

                // Movement code with proper bounds checking
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        val touchX = event.x
                        val touchY = event.y

                        /*
                        Since the view is scaled, we need to convert touch coordinates to where they
                        would be in an unscaled view - the location of the button doesn't change
                        when we scale like we do. We just visually scale the view.
                        GPT can't do math, so I figured it out myself. this is just a difference
                        from the center, and scaled accordingly, try to imagine with scaling of
                        0.5x so it makes more sense - if I clicked some place on the right, and
                        the view is scaled to 0.5x, the real location of the button is twice as far
                         */

                        // Get the center of the view (pivot point)
                        val centerX = floatingView!!.width / 2f
                        val centerY = floatingView!!.height / 2f

                        // Convert touch coords to button location (the button is unaffected by scaling)
                        val x = (touchX - centerX) * (1/ currentScale) + centerX
                        val y = (touchY - centerY) * (1/ currentScale) + centerY

                        // Find what got clicked
                        val button = floatingView!!.findViewById<Button>(R.id.floating_close)
                        val buttonBounds = Rect()
                        button.getHitRect(buttonBounds)

                        if (buttonBounds.contains(x.toInt(), y.toInt())) {
                            button.performClick()
                            return@setOnTouchListener true
                        }
                        // End of weird

                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!scaleGestureDetector.isInProgress) {
                            val newX = initialX + (event.rawX - initialTouchX).toInt()
                            val newY = initialY + (event.rawY - initialTouchY).toInt()

                            // Get screen dimensions
                            val screenWidth = resources.displayMetrics.widthPixels
                            val screenHeight = resources.displayMetrics.heightPixels

                            // Use actual view dimensions instead of layout params
                            val actualWidth = floatingView!!.width
                            val actualHeight = floatingView!!.height

                            // Calculate visual dimensions after scaling
                            val visualWidth = (actualWidth * currentScale).toInt()
                            val visualHeight = (actualHeight * currentScale).toInt()
                            val horizontalOffset = (actualWidth - visualWidth) / 2// + 60
                            val verticalOffset = (actualHeight - visualHeight) / 2

                            var leftBound = -horizontalOffset
                            var rightBound = screenWidth - horizontalOffset - visualWidth

                            val weirdOffset = (leftBound + rightBound) / 2 // leftBound is usually 0, but whatever
                            leftBound -= weirdOffset
                            rightBound -= weirdOffset

                            val topBound = -verticalOffset
                            val bottomBound = screenHeight - actualHeight + verticalOffset

                            // Apply bounds
                            layoutParams.x = newX.coerceIn(leftBound, rightBound)
                            layoutParams.y = newY.coerceIn(topBound, bottomBound)

//                            println("Screen: ${screenWidth}x${screenHeight}")
//                            println("Actual view: ${actualWidth}x${actualHeight}")
//                            println("Visual: ${visualWidth}x${visualHeight}")
//                            println("Calculated bounds - Left: $leftBound, Right: $rightBound")
//                            println("Current position: x=${layoutParams.x}, y=${layoutParams.y}")
//                            println("Raw touch: ${event.rawX}, Calculated newX: $newX")

                            windowManager.updateViewLayout(floatingView, layoutParams)
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the floating view
        if (floatingView != null) {
            windowManager.removeView(floatingView)
            floatingView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
