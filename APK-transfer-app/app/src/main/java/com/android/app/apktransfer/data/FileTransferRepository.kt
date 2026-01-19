package com.android.app.apktransfer.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class FileTransferRepository(private val context: Context) {
    @Volatile
    private var currentCall: Call? = null

    fun cancelUpload() {
        currentCall?.cancel()
        currentCall = null
    }

    fun copyUriToCache(uri: Uri): String {
        val fileName = context.contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) it.getString(idx) else null
            } else null
        } ?: uri.path?.substringAfterLast('/') ?: "temp_file"

        val file = File(context.cacheDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file.absolutePath
    }

    fun copyApkToCache(apkPath: String, packageName: String): File? {
        return try {
            val sourceFile = File(apkPath)
            if (!sourceFile.exists() || !sourceFile.canRead()) return null

            val timestamp = System.currentTimeMillis()
            val fileName = "${packageName}_${sourceFile.nameWithoutExtension}_${timestamp}.apk"
            val destFile = File(context.cacheDir, fileName)

            sourceFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun listCacheFiles(): List<File> {
        return context.cacheDir.listFiles()?.filter { it.isFile } ?: emptyList()
    }

    fun clearCache() {
        context.cacheDir.deleteRecursively()
    }

    suspend fun uploadFileToServer(
        filePath: String,
        serverUrl: String,
        onStart: () -> Unit,
        onProgress: (Int) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            withContext(Dispatchers.Main) { onStart() }

            val file = File(filePath)
            if (!file.exists()) {
                withContext(Dispatchers.Main) { onError("File not found") }
                return@withContext
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .callTimeout(600, TimeUnit.SECONDS)
                .build()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("application/octet-stream".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("$serverUrl/upload")
                .post(requestBody)
                .build()

            val call = client.newCall(request)
            currentCall = call
            call.execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "No response"
                    withContext(Dispatchers.Main) { onSuccess(responseBody) }
                } else {
                    withContext(Dispatchers.Main) { onError("Server error: ${response.code}") }
                }
            }

            withContext(Dispatchers.Main) { onProgress(100) }
        } catch (e: IOException) {
            if (currentCall?.isCanceled() == true) return@withContext
            withContext(Dispatchers.Main) { onError("Network error: ${e.message}") }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onError("Error: ${e.message}") }
        } finally {
            currentCall = null
        }
    }
}
