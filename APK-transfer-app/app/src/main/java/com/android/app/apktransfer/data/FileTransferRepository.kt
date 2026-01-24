// 데이터 계층 패키지 선언.
package com.android.app.apktransfer.data

// 파일 캐시/업로드에 필요한 컨텍스트.
import android.content.Context
// 파일 선택 결과를 표현하는 Uri 타입.
import android.net.Uri
// ContentResolver에서 파일명을 얻기 위한 컬럼 키.
import android.provider.OpenableColumns
// 코루틴 디스패처 사용.
import kotlinx.coroutines.Dispatchers
// 스레드 전환 헬퍼.
import kotlinx.coroutines.withContext
// OkHttp에서 MIME 타입 변환.
import okhttp3.MediaType.Companion.toMediaType
// 멀티파트 바디 구성.
import okhttp3.MultipartBody
// 취소 가능한 호출 핸들.
import okhttp3.Call
// HTTP 클라이언트.
import okhttp3.OkHttpClient
// 요청 빌더.
import okhttp3.Request
// 파일 바디 생성.
import okhttp3.RequestBody.Companion.asRequestBody
// 로컬 파일 접근.
import java.io.File
// 네트워크 IO 예외 처리.
import java.io.IOException
// 타임아웃 설정용.
import java.util.concurrent.TimeUnit

// 파일 캐시 복사와 업로드를 담당하는 리포지토리.
class FileTransferRepository(private val context: Context) {
    // 업로드 취소를 위해 현재 호출을 스레드 간 공유한다.
    @Volatile
    private var currentCall: Call? = null

    // 진행 중인 업로드를 취소한다.
    fun cancelUpload() {
        currentCall?.cancel()
        currentCall = null
    }

    // 선택된 Uri를 앱 캐시 디렉토리로 복사한다.
    fun copyUriToCache(uri: Uri): String {
        // ContentResolver에서 표시 이름을 우선 가져온다.
        val fileName = context.contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) it.getString(idx) else null
            } else null
        } ?: uri.path?.substringAfterLast('/') ?: "temp_file"

        // 캐시 디렉토리에 새 파일을 만든다.
        val file = File(context.cacheDir, fileName)
        // 입력 스트림을 열어 캐시로 복사한다.
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        // 업로드용으로 절대 경로를 반환한다.
        return file.absolutePath
    }

    // APK 파일을 캐시에 복사하고 새 파일을 반환한다.
    fun copyApkToCache(apkPath: String, packageName: String): File? {
        return try {
            // 원본 APK 파일 참조.
            val sourceFile = File(apkPath)
            // 존재/권한 체크 실패 시 중단.
            if (!sourceFile.exists() || !sourceFile.canRead()) return null

            // 충돌을 피하기 위해 타임스탬프를 포함한 파일명 생성.
            val timestamp = System.currentTimeMillis()
            val fileName = "${packageName}_${sourceFile.nameWithoutExtension}_${timestamp}.apk"
            val destFile = File(context.cacheDir, fileName)

            // 원본을 캐시로 복사한다.
            sourceFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            // 복사된 파일을 반환한다.
            destFile
        } catch (e: Exception) {
            // IO/권한 문제 등을 로깅하고 null 처리.
            e.printStackTrace()
            null
        }
    }

    // 캐시 디렉토리의 파일 목록을 가져온다.
    fun listCacheFiles(): List<File> {
        return context.cacheDir.listFiles()?.filter { it.isFile } ?: emptyList()
    }

    // 캐시 디렉토리를 비워 선택 파일을 초기화한다.
    fun clearCache() {
        context.cacheDir.deleteRecursively()
    }

    // 멀티파트로 파일을 서버에 업로드한다.
    suspend fun uploadFileToServer(
        filePath: String,
        serverUrl: String,
        onStart: () -> Unit,
        onProgress: (Int) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            // UI 콜백은 메인 스레드에서 실행한다.
            withContext(Dispatchers.Main) { onStart() }

            // 업로드할 파일 참조.
            val file = File(filePath)
            if (!file.exists()) {
                // 파일이 없으면 즉시 오류 콜백.
                withContext(Dispatchers.Main) { onError("File not found") }
                return@withContext
            }

            // 대용량 업로드를 고려해 타임아웃을 넉넉히 설정.
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .callTimeout(600, TimeUnit.SECONDS)
                .build()

            // form-data로 파일 필드를 구성한다.
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("application/octet-stream".toMediaType())
                )
                .build()

            // /upload 엔드포인트로 POST 요청 생성.
            val request = Request.Builder()
                .url("$serverUrl/upload")
                .post(requestBody)
                .build()

            // 호출 핸들을 저장해 취소를 가능하게 한다.
            val call = client.newCall(request)
            currentCall = call
            call.execute().use { response ->
                if (response.isSuccessful) {
                    // 성공 시 서버 응답을 전달.
                    val responseBody = response.body?.string() ?: "No response"
                    withContext(Dispatchers.Main) { onSuccess(responseBody) }
                } else {
                    // HTTP 오류 코드를 전달.
                    withContext(Dispatchers.Main) { onError("Server error: ${response.code}") }
                }
            }

            // 완료 후 진행률 100% 반영.
            withContext(Dispatchers.Main) { onProgress(100) }
        } catch (e: IOException) {
            // 취소로 인한 예외는 무시한다.
            if (currentCall?.isCanceled() == true) return@withContext
            withContext(Dispatchers.Main) { onError("Network error: ${e.message}") }
        } catch (e: Exception) {
            // 기타 예외는 오류 메시지로 전달.
            withContext(Dispatchers.Main) { onError("Error: ${e.message}") }
        } finally {
            // 다음 업로드를 위해 호출 참조를 비운다.
            currentCall = null
        }
    }
}
