import 'package:flutter/material.dart';
// import 'package:url_launcher/url_launcher.dart';

class SettingsPage extends StatefulWidget {
  const SettingsPage({Key? key}) : super(key: key);

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  bool _enableAds = true;

  /// Handles changes to the "Enable Ads" setting
  void _onEnableAdsChanged(bool? value) {
    if (value == null) return;
    setState(() {
      _enableAds = value;
      // TODO: Persist this preference, e.g., SharedPreferences or backend

    });
  }

  /// Launches an email intent to contact the developer
  Future<void> _launchEmail() async {

    // TODO: doesn't really work
    // if (await canLaunchUrl(emailUri)) {
    //   await launchUrl(emailUri);
    // } else {
    //   throw 'Could not launch $emailUri';
    // }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
        backgroundColor: Theme.of(context).colorScheme.primary,
        elevation: 0,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16.0),
        children: [
          // Clarify-specific options
          const Text(
            'Clarify Options',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'No additional options available.',
            style: TextStyle(color: Colors.grey),
          ),
          const Divider(height: 32),

          // Enable Ads checkbox
          CheckboxListTile(
            title: const Text('Enable Ads'),
            value: _enableAds,
            onChanged: _onEnableAdsChanged,
            controlAffinity: ListTileControlAffinity.leading,
          ),
          const Divider(height: 32),

          // Developer info
          const Text(
            'About',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 8),
          ListTile(
            leading: const Icon(Icons.email),
            title: const Text('aa.devsgroup@gmail.com'),
            onTap: _launchEmail,
          ),
        ],
      ),
    );
  }
}
