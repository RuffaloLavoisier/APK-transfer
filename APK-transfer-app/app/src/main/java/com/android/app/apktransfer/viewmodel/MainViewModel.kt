// 뷰모델 패키지 선언.
package com.android.app.apktransfer.viewmodel

// Application 컨텍스트 접근용.
import android.app.Application
// 파일 선택 결과를 표현하는 Uri.
import android.net.Uri
// AndroidViewModel 기반 ViewModel.
import androidx.lifecycle.AndroidViewModel
// 코루틴 스코프 제공.
import androidx.lifecycle.viewModelScope
// SharedPreferences 접근을 위한 매니저.
import com.android.app.apktransfer.PreferencesManager
// 설치 앱 로딩 리포지토리.
import com.android.app.apktransfer.data.AppRepository
// 자체 파일 서버 제어.
import com.android.app.apktransfer.data.FileServerManager
// 파일 캐시/업로드 처리.
import com.android.app.apktransfer.data.FileTransferRepository
// 앱 정보를 담는 모델.
import com.android.app.apktransfer.model.AppInfo
// 코루틴 디스패처.
import kotlinx.coroutines.Dispatchers
// UI 상태 스트림.
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
// 코루틴 실행.
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
// 업로드 작업 취소 핸들.
import kotlinx.coroutines.Job
// 코루틴 활성 여부 확인.
import kotlinx.coroutines.isActive
// 파일 객체.
import java.io.File


// 화면에서 사용하는 상태를 모아둔 UI 모델.
data class MainUiState(
    // 설치 앱 목록.
    val apps: List<AppInfo> = emptyList(),
    // 검색어 입력값.
    val searchQuery: String = "",
    // 업로드 대상 서버 URL.
    val serverUrl: String = "",
    // 로그 텍스트(줄바꿈 포함).
    val logText: String = "",
    // 업로드 진행 중 여부.
    val isUploading: Boolean = false,
    // 업로드 진행률(0~1).
    val uploadProgress: Float = 0f,
    // 자체 서버 실행 여부.
    val isServerRunning: Boolean = false,
    // 자체 서버 접속 URL.
    val fileServerUrl: String? = null,
    // 전송 모드(원격 업로드/자체 서버).
    val transferMode: TransferMode = TransferMode.REMOTE_UPLOAD,
    // 자체 서버에서 제공할 선택 파일 목록.
    val selectedFiles: List<File> = emptyList(),
    // 자체 서버 모드에서 APK 복사 진행 여부.
    val isCopyingApk: Boolean = false,
    // APK 복사 진행률(0~1).
    val copyProgress: Float = 0f,
    // 복사 중인 앱 패키지명(리스트 표시용).
    val copyingPackageName: String? = null
)

// 전송 모드 구분을 위한 열거형.
enum class TransferMode {
    // 원격 서버로 업로드.
    REMOTE_UPLOAD,
    // 기기에서 파일 서버 실행.
    SELF_SERVER
}

// 메인 화면에서 사용하는 상태와 로직을 관리하는 ViewModel.
class MainViewModel(application: Application) : AndroidViewModel(application) {
    // 설치 앱 목록을 가져오는 리포지토리.
    private val appRepository = AppRepository(application)
    // 파일 복사/업로드 리포지토리.
    private val fileTransferRepository = FileTransferRepository(application)
    // 자체 파일 서버 매니저.
    private val fileServerManager = FileServerManager()
    // 업로드 코루틴 취소를 위한 핸들.
    private var uploadJob: Job? = null
    // APK 복사 작업 취소를 위한 핸들.
    private var copyJob: Job? = null

    // 내부 상태 스트림(쓰기 가능).
    private val _uiState = MutableStateFlow(
        // 저장된 서버 URL을 초기값으로 사용.
        MainUiState(serverUrl = PreferencesManager.getServerUrl(application))
    )
    // 외부 노출용 읽기 전용 상태.
    val uiState = _uiState.asStateFlow()

    init {
        // 앱 시작 시 설치 앱 목록을 불러온다.
        loadApps()
    }

    // 검색어 입력을 업데이트한다.
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    // 서버 URL을 저장하고 상태에 반영한다.
    fun saveServerUrl(newUrl: String) {
        PreferencesManager.saveServerUrl(getApplication(), newUrl)
        _uiState.update { it.copy(serverUrl = newUrl) }
        appendLog("[SETTINGS] Server URL updated")
    }

    // 전송 모드를 변경하며 관련 상태를 정리한다.
    fun setTransferMode(mode: TransferMode) {
        // 현재 모드와 같으면 아무 것도 하지 않는다.
        val current = _uiState.value.transferMode
        if (current == mode) return

        // 원격 업로드로 전환 시 실행 중인 자체 서버를 중지한다.
        if (mode == TransferMode.REMOTE_UPLOAD && fileServerManager.isRunning()) {
            fileServerManager.stop()
            appendLog("[SERVER] Stopped (switch to upload mode)")
        }
        // 자체 서버 모드를 벗어나면 복사 작업을 중단한다.
        if (mode == TransferMode.REMOTE_UPLOAD) {
            copyJob?.cancel()
        }

        // 모드에 맞게 서버 관련 상태를 정리한다.
        _uiState.update {
            it.copy(
                transferMode = mode,
                isServerRunning = if (mode == TransferMode.SELF_SERVER) it.isServerRunning else false,
                fileServerUrl = if (mode == TransferMode.SELF_SERVER) it.fileServerUrl else null,
                isCopyingApk = if (mode == TransferMode.SELF_SERVER) it.isCopyingApk else false,
                copyProgress = if (mode == TransferMode.SELF_SERVER) it.copyProgress else 0f,
                copyingPackageName = if (mode == TransferMode.SELF_SERVER) it.copyingPackageName else null
            )
        }
        // 사용자에게 모드 변경 로그를 남긴다.
        appendLog(
            if (mode == TransferMode.SELF_SERVER) "[MODE] Self server enabled"
            else "[MODE] Remote upload enabled"
        )
    }

    // 파일 탐색기에서 선택한 파일을 처리한다.
    fun onFilePicked(uri: Uri) {
        // 캐시에 복사 후 경로를 얻는다.
        val filePath = fileTransferRepository.copyUriToCache(uri)
        // 로그에 표시할 파일명만 추출한다.
        val fileName = File(filePath).name

        // 자체 서버 모드라면 업로드 없이 캐시 목록만 갱신한다.
        if (_uiState.value.transferMode == TransferMode.SELF_SERVER) {
            val cacheFiles = fileTransferRepository.listCacheFiles()
            _uiState.update { it.copy(selectedFiles = cacheFiles) }
            appendLog("[FILE] Cached: $fileName")
            appendLog("[FILE] Selected ${cacheFiles.size} files")
            return
        }

        // 원격 업로드 모드에서는 파일 선택을 기록한다.
        appendLog("[FILE] Selected: $fileName")

        // 기존 업로드 작업이 있으면 취소한다.
        uploadJob?.cancel()
        uploadJob = viewModelScope.launch {
            uploadFile(filePath)
        }
    }

    // 앱 목록에서 앱을 선택했을 때 처리한다.
    fun onAppSelected(app: AppInfo) {
        // 자체 서버 모드에서는 APK를 캐시에만 모아둔다.
        if (_uiState.value.transferMode == TransferMode.SELF_SERVER) {
            appendLog("[APP] ${app.name} (${app.packageName})")
            appendLog("[INFO] ${app.apkPaths.size} APK file(s)")

            // 기존 복사 작업이 있으면 중단한다.
            copyJob?.cancel()
            copyJob = viewModelScope.launch(Dispatchers.IO) {
                // 복사 시작 상태를 알린다.
                _uiState.update {
                    it.copy(
                        isCopyingApk = true,
                        copyProgress = 0f,
                        copyingPackageName = app.packageName
                    )
                }

                try {
                    // split APK 포함 모든 파일을 캐시에 복사한다.
                    var copiedCount = 0
                    app.apkPaths.forEachIndexed { index, apkPath ->
                        if (!isActive) return@launch
                        val copiedFile = fileTransferRepository.copyApkToCache(apkPath, app.packageName)
                        if (copiedFile != null) copiedCount += 1

                        // 진행률을 업데이트한다.
                        val progress = (index + 1).toFloat() / app.apkPaths.size.toFloat()
                        _uiState.update { it.copy(copyProgress = progress) }
                    }

                    // 캐시 목록을 UI에 반영한다.
                    val cacheFiles = fileTransferRepository.listCacheFiles()
                    _uiState.update {
                        it.copy(
                            selectedFiles = cacheFiles,
                            copyProgress = 1f
                        )
                    }
                    // 완료 상태가 잠깐 보이도록 짧게 유지한다.
                    delay(1000)
                    _uiState.update {
                        it.copy(
                            isCopyingApk = false,
                            copyingPackageName = null
                        )
                    }
                    appendLog("[FILE] Cached $copiedCount/${app.apkPaths.size} APK(s)")
                    appendLog("[FILE] Selected ${cacheFiles.size} files")
                } finally {
                    // 취소/예외 시에도 복사 상태를 정리한다.
                    _uiState.update { it.copy(isCopyingApk = false, copyingPackageName = null) }
                }
            }
            return
        }

        // 원격 업로드 모드에서는 업로드 로그를 남긴다.
        appendLog("[APP] ${app.name} (${app.packageName})")
        appendLog("[INFO] ${app.apkPaths.size} APK file(s)")

        // 기존 업로드를 취소하고 새 업로드를 시작한다.
        uploadJob?.cancel()
        uploadJob = viewModelScope.launch {
            app.apkPaths.forEachIndexed { index, apkPath ->
                // 코루틴이 취소되었으면 중단한다.
                if (!isActive) return@launch
                _uiState.update { it.copy(isUploading = true) }
                appendLog("[${index + 1}/${app.apkPaths.size}] Copying...")

                // APK를 캐시에 복사한 뒤 업로드한다.
                val copiedFile = fileTransferRepository.copyApkToCache(apkPath, app.packageName)
                if (copiedFile != null) {
                    fileTransferRepository.uploadFileToServer(
                        filePath = copiedFile.absolutePath,
                        serverUrl = _uiState.value.serverUrl,
                        onStart = {
                            appendLog("[${index + 1}/${app.apkPaths.size}] Uploading...")
                        },
                        onProgress = { progress ->
                            _uiState.update { it.copy(uploadProgress = progress / 100f) }
                        },
                        onSuccess = { response ->
                            _uiState.update { it.copy(isUploading = false, uploadProgress = 0f) }
                            appendLog("[${index + 1}/${app.apkPaths.size}] Success!")
                            appendLog("\n=== RESULT ===\n$response\n")
                            copiedFile.delete()
                        },
                        onError = { error ->
                            _uiState.update { it.copy(isUploading = false, uploadProgress = 0f) }
                            appendLog("[${index + 1}/${app.apkPaths.size}] Error: $error")
                        }
                    )
                } else {
                    _uiState.update { it.copy(isUploading = false) }
                    appendLog("[${index + 1}/${app.apkPaths.size}] Failed to copy")
                }
            }
        }
    }

    // 캐시 내 파일들을 다시 선택 목록으로 갱신한다.
    fun selectCacheFiles() {
        val cacheFiles = fileTransferRepository.listCacheFiles()
        _uiState.update { it.copy(selectedFiles = cacheFiles) }
        appendLog("[FILE] Selected ${cacheFiles.size} files")
    }

    // 선택 파일을 비우고 캐시를 정리한다.
    fun clearSelectedFiles() {
        _uiState.update {
            it.copy(
                selectedFiles = emptyList(),
                isCopyingApk = false,
                copyProgress = 0f,
                copyingPackageName = null
            )
        }
        appendLog("[CLEAR] Files cleared")
        fileTransferRepository.clearCache()
    }

    // 자체 파일 서버를 시작/중지한다.
    fun toggleServer() {
        // 업로드 모드에서는 자체 서버를 켤 수 없다.
        if (_uiState.value.transferMode != TransferMode.SELF_SERVER) {
            appendLog("[INFO] Self server is disabled in upload mode")
            return
        }
        // 실행 중이면 중지한다.
        if (fileServerManager.isRunning()) {
            fileServerManager.stop()
            _uiState.update { it.copy(isServerRunning = false, fileServerUrl = null) }
            appendLog("[SERVER] Stopped")
            return
        }

        // 선택된 파일 목록으로 서버를 시작한다.
        val startResult = fileServerManager.start(_uiState.value.selectedFiles)
        startResult.onSuccess { url ->
            _uiState.update { it.copy(isServerRunning = true, fileServerUrl = url) }
            appendLog("[SERVER] Started at $url")
        }.onFailure {
            appendLog("[ERROR] No files or WiFi")
        }
    }

    // 진행 중인 업로드를 취소한다.
    fun cancelUpload() {
        // 원격 업로드 모드가 아니면 취소할 업로드가 없다.
        if (_uiState.value.transferMode != TransferMode.REMOTE_UPLOAD) return

        // 네트워크 호출과 코루틴을 모두 취소한다.
        fileTransferRepository.cancelUpload()
        uploadJob?.cancel()
        uploadJob = null
        _uiState.update { it.copy(isUploading = false, uploadProgress = 0f) }
        appendLog("[UPLOAD] Canceled")
    }

    // 로그를 비운다.
    fun clearLog() {
        _uiState.update { it.copy(logText = "") }
    }

    // 외부에서 로그를 추가할 때 사용한다.
    fun addLog(line: String) {
        appendLog(line)
    }

    // 단일 파일 업로드를 수행한다.
    private suspend fun uploadFile(filePath: String) {
        fileTransferRepository.uploadFileToServer(
            filePath = filePath,
            serverUrl = _uiState.value.serverUrl,
            onStart = {
                _uiState.update { it.copy(isUploading = true) }
                appendLog("[UPLOAD] Starting...")
            },
            onProgress = { progress ->
                _uiState.update { it.copy(uploadProgress = progress / 100f) }
            },
            onSuccess = { response ->
                _uiState.update { it.copy(isUploading = false, uploadProgress = 0f) }
                appendLog("[SUCCESS] Upload completed!")
                appendLog("\n=== APKID RESULT ===\n$response\n")
                File(filePath).delete()
            },
            onError = { error ->
                _uiState.update { it.copy(isUploading = false, uploadProgress = 0f) }
                appendLog("[ERROR] $error")
            }
        )
    }

    // 설치된 앱 목록을 백그라운드에서 불러온다.
    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = appRepository.loadInstalledApps()
            _uiState.update { it.copy(apps = apps) }
        }
    }

    // 내부적으로 로그 텍스트를 누적한다.
    private fun appendLog(line: String) {
        _uiState.update { state ->
            val next = if (state.logText.isBlank()) line else state.logText + "\n" + line
            state.copy(logText = next)
        }
    }

    // ViewModel이 제거될 때 서버를 정리한다.
    override fun onCleared() {
        super.onCleared()
        copyJob?.cancel()
        fileServerManager.stop()
    }
}
