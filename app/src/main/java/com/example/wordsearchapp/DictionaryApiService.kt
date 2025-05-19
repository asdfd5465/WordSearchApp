package com.example.wordsearchapp

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class DictionaryApiService {

    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun getMeaning(word: String): Result<List<ApiEntry>> {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://api.dictionaryapi.dev/api/v2/entries/en/$word")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("API Error: ${response.code}"))
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    return@withContext Result.failure(IOException("Empty response body"))
                }

                // The API returns a JSON array of entries
                val type = object : TypeToken<List<ApiEntry>>() {}.type
                val entries: List<ApiEntry> = gson.fromJson(responseBody, type)
                Result.success(entries)

            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) { // Catch other parsing exceptions
                Result.failure(e)
            }
        }
    }
}
