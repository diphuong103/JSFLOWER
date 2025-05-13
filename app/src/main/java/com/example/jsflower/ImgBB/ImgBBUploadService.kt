package com.example.jsflower.ImgBB

import android.content.Context
import android.net.Uri
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImgBBUploadService {
    private val client = OkHttpClient()
    private val API_KEY = "d5e2f70fce99fcedeb7507ebc4eb1aed"

    interface UploadCallback {
        fun onSuccess(imageUrl: String)
        fun onFailure(errorMessage: String)
    }

    fun uploadImage(context: Context, imageUri: Uri, callback: UploadCallback) {
        // Convert uri to file that can be uploaded
        val file = convertUriToFile(context, imageUri) ?: run {
            callback.onFailure("Failed to process image file")
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull())
            )
            .addFormDataPart("key", API_KEY)
            .build()

        val request = Request.Builder()
            .url("https://api.imgbb.com/1/upload")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback.onFailure("Server error: ${response.code}")
                    return
                }

                try {
                    val responseBody = response.body?.string() ?: ""
                    val jsonObject = JSONObject(responseBody)

                    if (jsonObject.getBoolean("success")) {
                        val data = jsonObject.getJSONObject("data")
                        val imageUrl = data.getString("url")
                        callback.onSuccess(imageUrl)
                    } else {
                        callback.onFailure("Upload failed")
                    }
                } catch (e: Exception) {
                    callback.onFailure("Error parsing response: ${e.message}")
                } finally {
                    // Clean up temporary file
                    file.delete()
                }
            }
        })
    }

    private fun convertUriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)

            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            tempFile
        } catch (e: Exception) {
            Log.e("ImgBBUploadService", "Error converting uri to file: ${e.message}")
            null
        }
    }
}