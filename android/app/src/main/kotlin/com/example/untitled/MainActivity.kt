package com.example.untitled // Ensure this matches your app's package name

import android.content.Intent
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import android.util.Log
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache

class MainActivity: FlutterActivity() {
    // Define the channel name
    private val CHANNEL = "com.example.untitled/floating" // Use your actual package name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the MethodChannel safely
        flutterEngine?.let { engine ->
            MethodChannel(engine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
                when (call.method) {
                    "startFloatingService" -> {
                        Log.d("MainActivity", "Starting floating service...")
                        startService(Intent(this, YourFloatingService::class.java))
                        result.success(null)
                    }
                    else -> {
                        Log.e("MainActivity", "Method not implemented: ${call.method}")
                        result.notImplemented()
                    }
                }
            }
        }
        if (flutterEngine == null) {
            Log.e("MainActivity", "flutterEngine is null!")
        } else {
            Log.d("MainActivity", "flutterEngine initialized successfully.")
        }

    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // Cache the engine for reuse in your service
        FlutterEngineCache.getInstance().put("flutter_engine_id", flutterEngine)
    }
}
