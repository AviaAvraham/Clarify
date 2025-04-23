package com.clarify.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log

class ProcessTextActivity : Activity() {

    private val REQUEST_OVERLAY_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ProcessTextActivity", "Something happened")

        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT) ?: ""

        if (!Settings.canDrawOverlays(this)) {
            // Request overlay permission
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
        } else {
            // Launch the floating view with the text
            launchFloatingService(text.toString())
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                // Retry launching the floating view after permission is granted
                val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT) ?: ""
                launchFloatingService(text.toString())
            } else {
                // Permission not granted, handle gracefully
                Log.e("ProcessTextActivity", "Overlay permission denied")
                finish()
            }
        }
    }

    private fun launchFloatingService(text: String) {
        val serviceIntent = Intent(this, YourFloatingService::class.java)
        serviceIntent.putExtra("extra_text", text)
        startService(serviceIntent)
    }
}
