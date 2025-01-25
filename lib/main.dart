import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import 'ddg_request.dart';
import 'dart:io';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
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
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
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
  static const platform = MethodChannel('com.example.untitled/floating');
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
        // message = "Explain this term or text VERY shortly: '$message'. "
        //     "If the term is in [Hebrew], answer in the same language, "
        //     "otherwise in English";
        var detailedResponse = false;
        return await _sendMessageFromPlatform(message, detailedResponse);
      } else if (call.method == "handleMoreDetails") {
        print("handleMoreDetails received: ${call.arguments}");
        String message = call.arguments;
        // message = "Explain this term or text in a few sentences: '$message'."
        //     "If the term is in [Hebrew], answer in the same language, "
        //     "otherwise in English";
        var detailedResponse = true;
        return await _sendMessageFromPlatform(message, detailedResponse);
      }
      print("Unhandled method: ${call.method}");
      return null;
    });

    _checkPermissions();

    // Add observer for app lifecycle changes
    WidgetsBinding.instance.addObserver(this);
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
          _buildPermissionButtonAndroid(
            'Disable Battery Optimization',
            _hasBatteryOptimization,
            _requestBatteryOptimization,
          ),
        ],
      ),
    );
  }


  Future<String?> _sendMessage() async {
    print("HELLOOOOOO");
    //print("api token:" + _apiToken!);
    if (_controller.text.isNotEmpty) {
      String message = _controller.text;

      String response = await client.callModel(message, false); //"Feature removed for now"; //await chat.callModel(message);
      _controller.clear(); // Clear the input field after sending
      _update_response(response);
      return response;
    }
    return "";
  }

  @override
  Widget build(BuildContext context) {
    // This method is rerun every time setState is called, for instance as done
    // by the _incrementCounter method above.
    //
    // The Flutter framework has been optimized to make rerunning build methods
    // fast, so that you can just rebuild anything that needs updating rather
    // than having to individually change instances of widgets.
    return Scaffold(
      appBar: AppBar(
        // TRY THIS: Try changing the color here to a specific color (to
        // Colors.amber, perhaps?) and trigger a hot reload to see the AppBar
        // change color while the other colors stay the same.
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        // Here we take the value from the MyHomePage object that was created by
        // the App.build method, and use it to set our appbar title.
        title: Text(widget.title),
      ),
      body: SingleChildScrollView(
        child: Center(
          // Center is a layout widget. It takes a single child and positions it
          // in the middle of the parent.
          child: Column(
            // Column is also a layout widget. It takes a list of children and
            // arranges them vertically. By default, it sizes itself to fit its
            // children horizontally, and tries to be as tall as its parent.
            //
            // Column has various properties to control how it sizes itself and
            // how it positions its children. Here we use mainAxisAlignment to
            // center the children vertically; the main axis here is the vertical
            // axis because Columns are vertical (the cross axis would be
            // horizontal).
            //
            // TRY THIS: Invoke "debug painting" (choose the "Toggle Debug Paint"
            // action in the IDE, or press "p" in the console), to see the
            // wireframe for each widget.
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              const Text(
                'Chat with LLaMA:',
              ),
              Padding(
                padding: const EdgeInsets.all(16.0),
                child: TextField(
                  controller: _controller,
                  decoration: InputDecoration(
                    border: OutlineInputBorder(),
                    labelText: 'Type your message',
                  ),
                ),
              ),
              ElevatedButton(
                onPressed: _sendMessage,
                child: const Text('Send'),
              ),
              const SizedBox(height: 20),
              Text(
                'Response:',
                style: Theme.of(context).textTheme.headlineMedium,
              ),
              Padding(
                padding: const EdgeInsets.all(16.0),
                child: Text(
                  _response,
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
              ),
              buildPermissionsButton(),
            ],
          ),
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _startFloatingService,
        tooltip: 'Increment',
        child: const Icon(Icons.add),
      ),
      // This trailing comma makes auto-formatting nicer for build methods.
    );
  }

  Future<void> _startFloatingService() async {
    try {
      await platform.invokeMethod('startFloatingService');
      print("Floating service started successfully.");
    } on PlatformException catch (e) {
      print("Error starting service: ${e.message}");
    }
  }

  Future<String?> _sendMessageFromPlatform(String message, bool detailedResponse) async {
    print("I WAS CALLED!");
    String response = await client.callModel(message, detailedResponse);
    _update_response(response);
    print(response + "is my response");
    return response; // Return response to the platform
  }
}
