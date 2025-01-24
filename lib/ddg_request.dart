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
  // still needed?
  static const Map<String, String> headersBase = {
    'User-Agent': 'Foo',
    'Accept': 'text/event-stream',
    'Accept-Language': 'en-GB,en;q=0.5',
    'Accept-Encoding': 'gzip, deflate, br, zstd',
    'Referer': 'https://duckduckgo.com/',
    'Content-Type': 'application/json',
    'Origin': 'https://duckduckgo.com',
    'Connection': 'keep-alive',
    'Cookie': 'dcm=5',
    'Sec-Fetch-Dest': 'empty',
    'Sec-Fetch-Mode': 'cors',
    'Sec-Fetch-Site': 'same-origin',
    'TE': 'trailers',
  };

  // Constructor
  DuckDuckGoChat() {
    //_fetchApiToken(); // Fetch the API token when the instance is created
  }

  // should do post if needed... currently this is always the first request
  Future<http.Response> _makeGetRequest(Uri url, Map<String, String> headers) async {
    try
    {
      return await http.get(url, headers: headers);
    }
    on SocketException catch (e)
    {
      if (e.osError?.errorCode == 7)
        {
          throw "Got an error. Are you connected to the internet?";
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
    String url_str;

    if (detailedResponse) {
      url_str = ('https://avia2292.pythonanywhere.com/get-detailed-response');
    } else {
      url_str = ('https://avia2292.pythonanywhere.com/get-response');
    }
    Uri url = Uri.parse(url_str);
    final headers = {
      'Content-Type': 'application/json',
    };
    message = message.replaceAll('"', '').replaceAll("'", "");
    final body = '{"term": "$message"}';
    print("sending post...");
    final startTime = DateTime.now();
    //final client = http.Client();
    final response = await http.post(
      url,
      headers: headers,
      body: body,
    );
    final endTime = DateTime.now();
    print("got response!");
    final duration = endTime.difference(startTime);
    print("Request took ${duration.inMilliseconds}ms");
    print(response.body);
    print(headers);
    print(body);
    if (response.statusCode == 200) {
      print('Request successful!');
      return jsonDecode(response.body)["message"];
    } else {
      return 'Request failed with status code ${response.statusCode}';
    }
  }
}