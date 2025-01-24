import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:http/http.dart' as http;

// Enum for available models
enum DuckDuckGoModel {
  claude3Haiku('claude-3-haiku-20240307'),
  metaLlama('meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo'),
  mistralai('mistralai/Mixtral-8x7B-Instruct-v0.1'),
  gpt4oMini('gpt-4o-mini');

  final String value;
  const DuckDuckGoModel(this.value);
}
/*
TODO:
enhance main screen look (?) - add note to enable the settings
cleanup a lot of comments both here and on server
decide on a name + logo, then add to floating window
find beta testers and mini-publish
follow up on ddg fallback
open a git for backend?
rename MyApp from dropdown!
 */

class DuckDuckGoChat {
  // Constructor
  DuckDuckGoChat() {
    //_fetchApiToken(); // Fetch the API token when the instance is created
  }

  // should do post if needed... currently this is always the first request
  Future<http.Response> _sendPostRequest(String url_str, String body) async {
    Uri url = Uri.parse(url_str);
    final headers = {
      'Content-Type': 'application/json',
    };

    try
    {
      print("sending post request...");
      final startTime = DateTime.now();
      final response = await http.post(
        url,
        headers: headers,
        body: body,
      );
      final endTime = DateTime.now();
      print("got response!");
      final duration = endTime.difference(startTime);
      print("Request took ${duration.inMilliseconds}ms");
      return response;
    }
    on SocketException catch (e)
    {
      if (e.osError?.errorCode == 7)
        {
          throw "Are you connected to the internet?";
        }
      else
        {
          print(e.osError?.errorCode);
          rethrow;
        }
    }
  }

  Future<String> callModel(String message, bool detailedResponse) async {
    // Send the message to the specified model
    String url;

    if (detailedResponse) {
      url = 'https://avia2292.pythonanywhere.com/get-detailed-response';
    } else {
      url = 'https://avia2292.pythonanywhere.com/get-response';
    }
    message = message.replaceAll('"', '').replaceAll("'", ""); // important to not break things on server, temporary solution
    final body = '{"term": "$message"}';

    final response = await _sendPostRequest(url, body);
    //print(response.body);
    //print(headers);
    //print(body);
    if (response.statusCode == 200) {
      print('Request successful!');
      return jsonDecode(response.body)["message"];
    } else {
      return 'Request failed with status code ${response.statusCode}';
    }
  }
}