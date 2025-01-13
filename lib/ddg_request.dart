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
cleanup this file functions
enhance main screen look (?) - add note to enable the settings
cleanup a lot of comments both here and on server
decide on a name + logo, then add to floating window
find beta testers and mini-publish
follow up on ddg fallback
open a git for backend?
 */

class DuckDuckGoChat {
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

  static final CORS_SERVER_INIT = "https://avia2292.pythonanywhere.com/cors?url="; //"https://cors-anywhere.herokuapp.com/";
  static final STATUS_URL = "https://duckduckgo.com/duckchat/v1/status";
  static final STATUS_URL_WITH_CORS = CORS_SERVER_INIT + STATUS_URL;
  static final CHAT_URL = "https://duckduckgo.com/duckchat/v1/chat";
  static final CHAT_URL_WITH_CORS = "https://avia2292.pythonanywhere.com/ai-chat";

  List<Map<String, String>> messages = [];
  String? _apiToken; // Private variable to hold the API token
  DuckDuckGoModel _model;

  // Constructor
  DuckDuckGoChat({DuckDuckGoModel? model})
      : _model = model ?? DuckDuckGoModel.gpt4oMini {
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

  Future<void> _fetchApiToken() async {
    if (_apiToken != null) return; // Return if the token is already fetched

    print("Fetching API token...");
    var headers = {
      'x-vqd-accept': '1',
      ...headersBase,
    };

    var response = await _makeGetRequest(Uri.parse(STATUS_URL_WITH_CORS), headers);
    // await http.get(
    //   Uri.parse(STATUS_URL_WITH_CORS),
    //   headers: headers,
    // );
    print("after");
    //print(response.statusCode);
    //print(response.headers);
    //print(response.body);;
    final res_body = (jsonDecode(jsonDecode(response.body)["body"]));
    final res_headers = (jsonDecode(response.body)["headers"]);
    //print(req_headers);;
    // TODO: fix next requests!
    if (response.statusCode == 200) {
      if (res_body['status'] == "0") {
        _apiToken = res_headers['x-vqd-4'];
        print("API token fetched: $_apiToken");
      } else {
        print('Error in response body: ${res_body}');
      }
    } else {
      print('Failed to fetch API token. Status code: ${response.statusCode}, '
          'Content: ${response.body}');
    }
  }

  Future<String> sendMessage(String message) async {
    await _fetchApiToken(); // Ensure the API token is fetched before sending a message
    final headers = {
      'x-vqd-4': _apiToken!,
      ...headersBase,
    };
    messages.add({'content': message, 'role': 'user'});
    final payload = {
      'messages': messages,
      'model': _model.value,
    };
    print(messages);
    final response = await http.post(
      Uri.parse(CHAT_URL_WITH_CORS),
      headers: headers,
      body: jsonEncode(payload),
    );
    print(response.body);
    print(response.headers);
    //print(response.bodyBytes);
    if (response.statusCode == 200) {
      final words = <String>[];
      final res_body = (jsonDecode(response.body)["body"]);
      final res_headers = (jsonDecode(utf8.decode(response.bodyBytes))["headers"]);
      //print(res_body);
      for (var line in res_body.split('\n')) {
        if (line.isNotEmpty) {
          try {
            if (jsonDecode(line.substring(6)).containsKey("message")) {
              String message = jsonDecode(line.substring(6))["message"];
              words.add(utf8.decode(message.codeUnits)); //decode using utf8
            }
          } catch (e) {
            if (line != "data: [DONE]") {
              print('Error parsing JSON: $e, tried parsing: "$line"');
            }
          }
        }
      }
      final assistantMessage = words.join('');
      print(assistantMessage);

      // Update the API token with the new value from the response headers
      _apiToken = res_headers['x-vqd-4'];
      messages = [];
      //messages.add({'content': assistantMessage, 'role': 'assistant'});
      //print(messages);
      return assistantMessage; // Return the assistant's message instead of the API token
    } else {
      // bad response from my server! shouldn't get here normally
      var error_string = 'Error - bad status code in response from original server: ${response
          .statusCode}, ${response.body}';
      print(error_string);
      return error_string;
    }
  }

  Future<bool> checkModel() async {
    await _fetchApiToken(); // Ensure the API token is fetched before checking the model

    final headers = {
      'x-vqd-4': _apiToken!,
      ...headersBase,
    };

    final payload = {
      'messages': [{'content': 'Hello', 'role': 'user'}],
      'model': _model.value,
    };

    final response = await http.post(
      Uri.parse(CHAT_URL_WITH_CORS),
      headers: headers,
      body: jsonEncode(payload),
    );

    if (response.statusCode == 200) {
      print('Model ${_model.value} is supported now');
      return true;
    } else {
      print('Status Code: ${response.statusCode}');
      print('Reason: ${response.reasonPhrase}');
      print('Headers: ${response.headers}');
      print('Content: ${response.body}');
      return false;
    }
  }

  Future<String> callModel(String message, bool detailedResponse) async {
    // Send the message to the specified model
    Uri url;
    if (detailedResponse) {
      url = Uri.parse('https://avia2292.pythonanywhere.com/get-detailed-response');
    } else {
      url = Uri.parse('https://avia2292.pythonanywhere.com/get-response');
    }
    final headers = {
      'Content-Type': 'application/json',
    };
    final body = '{"term": "$message"}';

    final response = await http.post(
      url,
      headers: headers,
      body: body,
    );
    print(headers);
    print(body);
    if (response.statusCode == 200) {
      print('Request successful!');
      return jsonDecode(response.body)["message"];
    } else {
      return 'Request failed with status code ${response.statusCode}';
      return await sendMessage(message);
    }
  }

  void main() async {
    final models = [
      //'claude-3-haiku-20240307', // works
      //'meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo',  // works!!!!!
      //'mistralai/Mixtral-8x7B-Instruct-v0.1', // works!!!!
      'gpt-4o-mini' // works!!!
    ];
    DuckDuckGoChat chat = DuckDuckGoChat(model: DuckDuckGoModel.metaLlama);

    // Check if a specific model is supported
    bool isModelSupported = await chat.checkModel();
    if (isModelSupported) {
      print("The model is supported. You can start chatting!");

      while (true) {
        stdout.write('You: ');
        final message = stdin.readLineSync();
        if (message != null && message.isNotEmpty) {
          // Send the message to the selected model
          String? response = await chat.sendMessage(message);
          if (response != null) {
            print('Assistant: $response');
          } else {
            print('Failed to get a response from the assistant.');
          }
        } else {
          print('Message cannot be empty. Please try again.');
        }
      }
    } else {
      print("The model is not supported.");
    }
  }
}