import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import 'ai_client.dart';
import 'dart:io';
// import 'settings_page.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Clarify',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // TRY THIS: Try running your application with "flutter run". You'll see
        // the application has a purple toolbar. Then, without quitting the app,
        // try changing the seedColor in the colorScheme below to Colors.green
        // and then invoke "hot reload" (save your changes or press the "hot
        // reload" button in a Flutter-supported IDE, or press "r" if you used
        // the command line to start the app).
        //
        // Notice that the counter didn't reset back to zero; the application
        // state is not lost during the reload. To reset the state, use hot
        // restart instead.
        //
        // This works for code too, not just values: Most code changes can be
        // tested with just a hot reload.
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.lightBlue),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Clarify'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  // This widget is the home page of your application. It is stateful, meaning
  // that it has a State object (defined below) that contains fields that affect
  // how it looks.

  // This class is the configuration for the state. It holds the values (in this
  // case the title) provided by the parent (in this case the App widget) and
  // used by the build method of the State. Fields in a Widget subclass are
  // always marked "final".

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> with WidgetsBindingObserver {
  int _counter = 0;
  String _response = '';
  final TextEditingController _controller = TextEditingController();
  AIClient client = AIClient();
  static const platform = MethodChannel('com.clarify.app/floating');
  bool _hasOverlayPermission = false;
  bool _hasBatteryOptimization = false;

  void _incrementCounter() {
    setState(() {
      // This call to setState tells the Flutter framework that something has
      // changed in this State, which causes it to rerun the build method below
      // so that the display can reflect the updated values. If we changed
      // _counter without calling setState(), then the build method would not be
      // called again, and so nothing would appear to happen.
      _counter++;
    });
  }

  void _update_response(String response) {
    setState(() {
      _response = response;
    });
  }

  @override
  void initState() {
    super.initState();

    // Set up method call handler
    platform.setMethodCallHandler((call) async {
      if (call.method == "handleMessage") {
        print("handleMessage received: ${call.arguments}");
        String message = call.arguments;
        var detailedResponse = false;
        return await _sendMessageFromPlatform(message, detailedResponse);
      } else if (call.method == "handleMoreDetails") {
        print("handleMoreDetails received: ${call.arguments}");
        String message = call.arguments;
        var detailedResponse = true;
        return await _sendMessageFromPlatform(message, detailedResponse);
      }
      print("Unhandled method: ${call.method}");
      return null;
    });

    _checkPermissions();

    // Add observer for app lifecycle changes
    WidgetsBinding.instance.addObserver(this);

    SystemChrome.setSystemUIOverlayStyle(
      const SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        // optionally choose light/dark icons to contrast your bar:
        statusBarIconBrightness: Brightness.light,
      ),
    );
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);
    if (state == AppLifecycleState.resumed) {
      // Trigger the permission check when the app resumes
      print("App resumed. Rechecking permissions...");
      Future.delayed(Duration(milliseconds: 1000), () async {
        await _checkPermissions();
      });
    }
  }


  Future<void> _checkPermissions() async {
    if (defaultTargetPlatform != TargetPlatform.android) {
      return;
    }
    try {
      final bool overlayPermission =
          await platform.invokeMethod('checkOverlayPermission');
      final bool batteryOptimization =
          await platform.invokeMethod('checkBatteryOptimization');

      print("Overlay permission: $overlayPermission");
      print("Battery optimization: $batteryOptimization");

      setState(() {
        _hasOverlayPermission = overlayPermission;
        _hasBatteryOptimization = batteryOptimization;
      });
    } on PlatformException catch (e) {
      print("Failed to get permissions: '${e.message}'.");
    } on MissingPluginException catch (e) {
      print("Got: ${e.message}, probably running service and not main activity");
    }
  }

  Future<void> _requestOverlayPermission() async {
    await platform.invokeMethod('requestOverlayPermission');
    // Check again after a short delay to allow for permission changes
    // Recheck permissions after a short delay to allow for permission changes
    Future.delayed(Duration(seconds: 1), () async {
      await _checkPermissions();
    });
  }

  Future<void> _requestBatteryOptimization() async {
    await platform.invokeMethod('requestBatteryOptimization');
    // Try checking twice with a small delay in between
    Future.delayed(Duration(seconds: 1), () async {
      await _checkPermissions();
    });
  }


  Widget _buildPermissionButtonAndroid(
      String title, bool hasPermission, VoidCallback onRequest) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: ElevatedButton(
        style: ElevatedButton.styleFrom(
          backgroundColor: Colors.white,
          padding: EdgeInsets.all(16),
        ),
        onPressed: hasPermission ? null : onRequest,
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              hasPermission ? Icons.check_circle : Icons.cancel,
              color: hasPermission ? Colors.green : Colors.red,
            ),
            SizedBox(width: 8),
            Text(
              title,
              style: TextStyle(
                color: Colors.black87,
                fontSize: 16,
              ),
            ),
          ],
        ),
      ),
    );
  }
  
  Widget buildPermissionsButton()
  {
    if (defaultTargetPlatform != TargetPlatform.android) {
      return Container();
    }
    if (_hasOverlayPermission)
    {
      return Container();
    }
    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          _buildPermissionButtonAndroid(
            'Display Over Other Apps',
            _hasOverlayPermission,
            _requestOverlayPermission,
          ),
          // _buildPermissionButtonAndroid(
          //   'Disable Battery Optimization',
          //   _hasBatteryOptimization,
          //   _requestBatteryOptimization,
          // ),
        ],
      ),
    );
  }


  Future<String?> _sendMessage() async {
    FocusScope.of(context).unfocus();
    String response;
    if (_controller.text.isNotEmpty) {
      String message = _controller.text;

      response = await client.callModel(message, false);
      //_controller.clear(); // Clear the input field after sending
    }
    else
      {
        response = "";
      }
      response = response.trim();
    _update_response(response);
    return response;
  }

  @override
  Widget build(BuildContext context) {
    final double statusBarHeight = MediaQuery.of(context).padding.top;
    return GestureDetector(
      // Close keyboard when tapping outside textbox
      onTap: () => FocusScope.of(context).unfocus(),
      child: Scaffold(
        backgroundColor: Colors.grey[100],
        extendBodyBehindAppBar: true,
        appBar: PreferredSize(
          preferredSize: Size.fromHeight(statusBarHeight + 60.0),
          child: Container(
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.primary,
              borderRadius: const BorderRadius.vertical(
                bottom: Radius.circular(24),
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.05),
                  offset: const Offset(0, 2),
                  blurRadius: 6,
                ),
              ],
            ),
            child: SafeArea(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16.0), // some horizontal padding
                child: Row(
                  children: [
                    // Left: Your App Icon
                    Padding(
                      padding: const EdgeInsets.only(right: 12.0),
                      child: Image.asset(
                        'Icon144.png', // or use Icon(Icons.your_icon)
                        width: 32,
                        height: 32,
                      ),
                    ),

                    // Middle: Title expands to fill available space
                    Expanded(
                      child: Text(
                        widget.title,
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          fontWeight: FontWeight.w600,
                          letterSpacing: 0.5,
                          fontSize: 18,
                          color: Colors.white,
                        ),
                      ),
                    ),

                    // Right: Settings cog
                    Padding(
                      padding: const EdgeInsets.only(left: 12.0),
                      child: IconButton(
                        icon: const Icon(Icons.settings),
                        color: Colors.white,
                        onPressed: () {
                          // Navigator.push(
                          //   context,
                          //   MaterialPageRoute(builder: (_) => const SettingsPage()),
                          // );
                        },
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),

        body: SafeArea(
          child: SingleChildScrollView(
            physics: const BouncingScrollPhysics(),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16.0),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.start,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: <Widget>[
                  const SizedBox(height: 16),
                  // Input section with subtle gradient background
                  Container(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        colors: [
                          Theme.of(context).colorScheme.primary.withOpacity(0.05),
                          Theme.of(context).colorScheme.secondary.withOpacity(0.05),
                        ],
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                      ),
                      borderRadius: BorderRadius.circular(24),
                    ),
                    padding: const EdgeInsets.all(16.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          "What term would you like to clarify?",
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w500,
                            color: Theme.of(context).colorScheme.primary,
                          ),
                        ),
                        const SizedBox(height: 12),
                        Card(
                          elevation: 0,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(18),
                            side: BorderSide(
                              color: Theme.of(context).colorScheme.primary.withOpacity(0.2),
                              width: 1.0,
                            ),
                          ),
                          child: Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 4.0),
                            child: TextField(
                              controller: _controller,
                              maxLines: 2, // Reduced from 3 to 2 to make it less high
                              // Prevent multiline/enter key behavior
                              textInputAction: TextInputAction.done,
                              onSubmitted: (_) => _sendMessage(),
                              decoration: InputDecoration(
                                filled: true,
                                fillColor: Colors.white,
                                hintText: 'Type here...',
                                hintStyle: TextStyle(color: Colors.grey[400]),
                                contentPadding: const EdgeInsets.all(16),
                                border: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(16),
                                  borderSide: BorderSide.none,
                                ),
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(height: 16),
                        SizedBox(
                          width: double.infinity,
                          child: ElevatedButton(
                            onPressed: _sendMessage,
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Theme.of(context).colorScheme.primary,
                              foregroundColor: Colors.white,
                              padding: const EdgeInsets.symmetric(vertical: 14),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(16),
                              ),
                              elevation: 0,
                            ),
                            child: const Text(
                              'Send',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.w600,
                                letterSpacing: 0.5,
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),

                  AnimatedSwitcher(
                    duration: const Duration(milliseconds: 300), // Smooth transition
                    child: _response.isEmpty
                        ? const SizedBox(height: 24) // Minimal space when no response
                        : Padding(
                      padding: const EdgeInsets.only(top: 24),
                      child: Container(
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(24),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withOpacity(0.05),
                              blurRadius: 10,
                              offset: const Offset(0, 4),
                            ),
                          ],
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Container(
                              padding: const EdgeInsets.all(16),
                              decoration: BoxDecoration(
                                color: Theme.of(context).colorScheme.secondary.withOpacity(0.1),
                                borderRadius: const BorderRadius.vertical(
                                  top: Radius.circular(24),
                                ),
                              ),
                              child: Row(
                                children: [
                                  // "Lit" lightbulb with color and gradient effect
                                  Container(
                                    padding: const EdgeInsets.all(6),
                                    decoration: BoxDecoration(
                                      color: Colors.amber.withOpacity(0.2),
                                      borderRadius: BorderRadius.circular(12),
                                    ),
                                    child: ShaderMask(
                                      shaderCallback: (Rect bounds) {
                                        return LinearGradient(
                                          colors: [
                                            Colors.amber[300]!,
                                            Colors.amber[600]!,
                                          ],
                                          begin: Alignment.topCenter,
                                          end: Alignment.bottomCenter,
                                        ).createShader(bounds);
                                      },
                                      child: const Icon(
                                        Icons.lightbulb,
                                        color: Colors.white,
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  Text(
                                    'Response',
                                    style: TextStyle(
                                      fontSize: 16,
                                      fontWeight: FontWeight.w600,
                                      color: Theme.of(context).colorScheme.secondary,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            Padding(
                              padding: const EdgeInsets.all(20),
                              child: SelectableText(  // Make text selectable
                                _response,
                                style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                                  height: 1.5,
                                  letterSpacing: 0.3,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),

                  const SizedBox(height: 24),

                  // Permissions button with subtle styling
                  Padding(
                    padding: const EdgeInsets.only(bottom: 16),
                    child: buildPermissionsButton(),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }


  Future<String?> _sendMessageFromPlatform(String message, bool detailedResponse) async {
    print("I WAS CALLED!");
    String response = await client.callModel(message, detailedResponse);
    _update_response(response);
    print(response + "is my response");
    return response; // Return response to the platform
  }
}
