package com.clarify.ai

import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketException
import java.util.concurrent.TimeUnit

class AIClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // increased the default values to support slower connections
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private suspend fun sendPostRequest(urlStr: String, body: String): Response = withContext(Dispatchers.IO) {
        val mediaType = "application/json".toMediaType()
        val requestBody = body.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(urlStr)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            println("sending post request...")
            val startTime = System.currentTimeMillis()

            val response = client.newCall(request).execute()

            val endTime = System.currentTimeMillis()
            println("got response!")
            val duration = endTime - startTime
            println("Request took ${duration}ms")

            response
        } catch (e: IOException) {
            println("IOException occurred: ${e.message}")

            // Check for network connectivity issues
            if (e.message?.contains("No address associated with hostname") == true ||
                e.message?.contains("Network is unreachable") == true ||
                e.message?.contains("Unable to resolve host") == true) {
                throw Exception("Are you connected to the internet?")
            }

            // Check for timeout
            if (e.message?.contains("timeout") == true) {
                println("Request timed out")
                throw Exception("Request timed out")
            }

            // Re-throw other IOExceptions
            throw e
        } catch (e: Exception) {
            println("Other exception occurred: ${e.message}")
            throw e
        }
    }

    suspend fun callModel(message: String, detailedResponse: Boolean): String {
        val url = if (detailedResponse) {
            "https://avia2292.pythonanywhere.com/get-detailed-response"
        } else {
            "https://avia2292.pythonanywhere.com/get-response"
        }

        // Clean the message - remove quotes, newlines, etc.
        val cleanMessage = message
            .replace("\"", "")
            .replace("'", "")
            .replace("\n", " ")
            .replace("\\n", " ")

        if (cleanMessage.isBlank()) {
            throw Exception("Please enter a valid message.")
        }

        if (cleanMessage.length > 70)
        {
            throw Exception("Message is too long, please try again with a shorter message.")
        }

        val body = """{"term": "$cleanMessage"}"""

        val response = sendPostRequest(url, body)

        return if (response.isSuccessful) {
            println("Request successful!")
            val responseBody = response.body?.string() ?: ""
            println(responseBody)

            val jsonResponse = JSONObject(responseBody)
            println(jsonResponse.getBoolean("is_error"))

            if (jsonResponse.getBoolean("is_error")) {
                val errorMessage = jsonResponse.optString("error", "Unknown error")
                throw Exception(errorMessage)
            }

            jsonResponse.getString("message")
        } else {
            "Request failed with status code ${response.code}"
        }
    }
}