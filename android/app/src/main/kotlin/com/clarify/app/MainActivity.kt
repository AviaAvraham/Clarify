package com.clarify.app // Ensure this matches your app's package name

import android.content.Intent
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import android.util.Log
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import android.provider.Settings
import android.os.PowerManager
import android.net.Uri
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.engine.dart.DartExecutor.DartEntrypoint

class MainActivity: FlutterActivity() {
    // Define the channel name
    private val CHANNEL = "com.clarify.app/floating" // Use your actual package name

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
                    "checkOverlayPermission" -> {
                        result.success(Settings.canDrawOverlays(this))
                    }
                    "checkBatteryOptimization" -> {
                        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
                        result.success(powerManager.isIgnoringBatteryOptimizations(packageName))
                    }
                    "requestOverlayPermission" -> {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName"))
                        startActivity(intent)
                        result.success(null)
                    }
                    "requestBatteryOptimization" -> {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:$packageName"))
                        startActivity(intent)
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

        // Create and cache a second, headless engine for YourFloatingService:
        val bgEngine = FlutterEngine(this).apply {
              // Start the Dart entrypoint immediately
           dartExecutor.executeDartEntrypoint(
                 DartExecutor.DartEntrypoint.createDefault()
                )
            }
        FlutterEngineCache.getInstance()
          .put("bg_engine_id", bgEngine)
    }
}
