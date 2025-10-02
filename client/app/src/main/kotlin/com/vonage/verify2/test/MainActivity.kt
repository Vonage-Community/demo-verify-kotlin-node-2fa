package com.vonage.verify2.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.IOException

var currentRequestId: String = ""
const val BACKEND_URL = ""

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VerifyApp() }
    }
}

@Composable
fun VerifyApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            VerificationScreen()
        }
    }
}

@Composable
fun VerificationScreen() {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var checkUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var fallbackToSms by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        if (fallbackToSms) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("SMS Code") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            if (!fallbackToSms) {
                Button(onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        message = ""
                        fallbackToSms = false

                        try {
                            val (requestId, authUrl) = startVerification(phone)
                            currentRequestId = requestId
                            checkUrl = authUrl
                        } catch (e: Exception) {
                            message = "Unable to verify. Please try again."
                            isLoading = false
                            return@launch
                        }

                        try {
                            val codeFromSa = checkSilentAuth(checkUrl)
                            val verified = submitCode(currentRequestId, codeFromSa)
                            message =
                                if (verified) "✅ Verified via Silent Auth" else "Fallback to SMS"
                            if (!verified) fallbackToSms = true
                        } catch (e: Exception) {
                            message = "Silent Auth failed, please enter SMS code"
                            fallbackToSms = true
                        } finally {
                            isLoading = false
                        }
                    }
                }) {
                    Text("Login")
                }
            }

            if (fallbackToSms) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        try {
                            val verified = submitCode(currentRequestId, code)
                            message = if (verified) "✅ Verified via SMS" else "❌ Invalid SMS code"
                        } catch (e: Exception) {
                            message = "Error: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                }) {
                    Text("Submit Code")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (message.isNotEmpty()) {
            Text(message)
        }
    }
}

suspend fun startVerification(phone: String): Pair<String, String> = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val json = Gson().toJson(mapOf("phone" to phone))
    val requestBody = json.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url("$BACKEND_URL/verification")
        .post(requestBody)
        .build()
    val response = client.newCall(request).execute()
    if (!response.isSuccessful) throw IOException("Verification failed: ${response.code}")

    val jsonBody = Gson().fromJson(response.body?.string(), JsonObject::class.java)

    val checkURL = jsonBody.get("check_url")?.asString ?: throw IOException("Missing check_url")
    val requestId = jsonBody.get("request_id")?.asString ?: throw IOException("Missing request_id")

    Pair(requestId, checkURL)
}

suspend fun checkSilentAuth(url: String): String = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val request = Request.Builder().url(url).get().build()
    val response = client.newCall(request).execute()

    println("Response code: ${response.code}")

    if (!response.isSuccessful) throw IOException("Silent Auth failed: ${response.code}")
    val jsonBody = Gson().fromJson(response.body?.string(), JsonObject::class.java)

    jsonBody.get("code")?.asString ?: throw IOException("Missing code from Silent Auth response")
}

suspend fun submitCode(requestId: String, code: String): Boolean = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val json = Gson().toJson(mapOf("request_id" to requestId, "code" to code))
    val requestBody = json.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url("$BACKEND_URL/check-code")
        .post(requestBody)
        .build()
    val response = client.newCall(request).execute()
    if (!response.isSuccessful) throw IOException("Code verification failed: ${response.code}")
    val jsonBody = Gson().fromJson(response.body?.string(), JsonObject::class.java)
    jsonBody.get("verified")?.asBoolean ?: false
}
