package com.android.app.apktransfer.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.app.apktransfer.PreferencesManager
import com.android.app.apktransfer.data.AppRepository
import com.android.app.apktransfer.data.FileServerManager
import com.android.app.apktransfer.data.FileTransferRepository
import com.android.app.apktransfer.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import java.io.File


data class MainUiState(
    val apps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val serverUrl: String = "",
    val logText: String = "",
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val isServerRunning: Boolean = false,
    val fileServerUrl: String? = null,
    val transferMode: TransferMode = TransferMode.REMOTE_UPLOAD,
    val selectedFiles: List<File> = emptyList()
)

enum class TransferMode {
    REMOTE_UPLOAD,
    SELF_SERVER
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appRepository = AppRepository(application)
    private val fileTransferRepository = FileTransferRepository(application)
    private val fileServerManager = FileServerManager()
    private var uploadJob: Job? = null

    private val _uiState = MutableStateFlow(
        MainUiState(serverUrl = PreferencesManager.getServerUrl(application))
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadApps()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun saveServerUrl(newUrl: String) {
        PreferencesManager.saveServerUrl(getApplication(), newUrl)
        _uiState.update { it.copy(serverUrl = newUrl) }
        appendLog("[SETTINGS] Server URL updated")
    }

    fun setTransferMode(mode: TransferMode) {
        val current = _uiState.value.transferMode
        if (current == mode) return

        if (mode == TransferMode.REMOTE_UPLOAD && fileServerManager.isRunning()) {
            fileServerManager.stop()
            appendLog("[SERVER] Stopped (switch to upload mode)")
        }

        _uiState.update {
            it.copy(
                transferMode = mode,
                isServerRunning = if (mode == TransferMode.SELF_SERVER) it.isServerRunning else false,
                fileServerUrl = if (mode == TransferMode.SELF_SERVER) it.fileServerUrl else null
            )
        }
        appendLog(
            if (mode == TransferMode.SELF_SERVER) "[MODE] Self server enabled"
            else "[MODE] Remote upload enabled"
        )
    }

    fun onFilePicked(uri: Uri) {
        val filePath = fileTransferRepository.copyUriToCache(uri)
        val fileName = File(filePath).name

        if (_uiState.value.transferMode == TransferMode.SELF_SERVER) {
            val cacheFiles = fileTransferRepository.listCacheFiles()
            _uiState.update { it.copy(selectedFiles = cacheFiles) }
            appendLog("[FILE] Cached: $fileName")
            appendLog("[FILE] Selected ${cacheFiles.size} files")
            return
        }

        appendLog("[FILE] Selected: $fileName")

        uploadJob?.cancel()
        uploadJob = viewModelScope.launch {
            uploadFile(filePath)
        }
    }

    fun onAppSelected(app: AppInfo) {
        if (_uiState.value.transferMode == TransferMode.SELF_SERVER) {
            appendLog("[APP] ${app.name} (${app.packageName})")
            appendLog("[INFO] ${app.apkPaths.size} APK file(s)")

            var copiedCount = 0
            app.apkPaths.forEach { apkPath ->
                val copiedFile = fileTransferRepository.copyApkToCache(apkPath, app.packageName)
                if (copiedFile != null) copiedCount += 1
            }

            val cacheFiles = fileTransferRepository.listCacheFiles()
            _uiState.update { it.copy(selectedFiles = cacheFiles) }
            appendLog("[FILE] Cached $copiedCount/${app.apkPaths.size} APK(s)")
            appendLog("[FILE] Selected ${cacheFiles.size} files")
            return
        }

        appendLog("[APP] ${app.name} (${app.packageName})")
        appendLog("[INFO] ${app.apkPaths.size} APK file(s)")

        uploadJob?.cancel()
        uploadJob = viewModelScope.launch {
            app.apkPaths.forEachIndexed { index, apkPath ->
                if (!isActive) return@launch
                _uiState.update { it.copy(isUploading = true) }
                appendLog("[${index + 1}/${app.apkPaths.size}] Copying...")

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

    fun selectCacheFiles() {
        val cacheFiles = fileTransferRepository.listCacheFiles()
        _uiState.update { it.copy(selectedFiles = cacheFiles) }
        appendLog("[FILE] Selected ${cacheFiles.size} files")
    }

    fun clearSelectedFiles() {
        _uiState.update { it.copy(selectedFiles = emptyList()) }
        appendLog("[CLEAR] Files cleared")
        fileTransferRepository.clearCache()
    }

    fun toggleServer() {
        if (_uiState.value.transferMode != TransferMode.SELF_SERVER) {
            appendLog("[INFO] Self server is disabled in upload mode")
            return
        }
        if (fileServerManager.isRunning()) {
            fileServerManager.stop()
            _uiState.update { it.copy(isServerRunning = false, fileServerUrl = null) }
            appendLog("[SERVER] Stopped")
            return
        }

        val startResult = fileServerManager.start(_uiState.value.selectedFiles)
        startResult.onSuccess { url ->
            _uiState.update { it.copy(isServerRunning = true, fileServerUrl = url) }
            appendLog("[SERVER] Started at $url")
        }.onFailure {
            appendLog("[ERROR] No files or WiFi")
        }
    }

    fun cancelUpload() {
        if (_uiState.value.transferMode != TransferMode.REMOTE_UPLOAD) return

        fileTransferRepository.cancelUpload()
        uploadJob?.cancel()
        uploadJob = null
        _uiState.update { it.copy(isUploading = false, uploadProgress = 0f) }
        appendLog("[UPLOAD] Canceled")
    }

    fun clearLog() {
        _uiState.update { it.copy(logText = "") }
    }

    fun addLog(line: String) {
        appendLog(line)
    }

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

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = appRepository.loadInstalledApps()
            _uiState.update { it.copy(apps = apps) }
        }
    }

    private fun appendLog(line: String) {
        _uiState.update { state ->
            val next = if (state.logText.isBlank()) line else state.logText + "\n" + line
            state.copy(logText = next)
        }
    }

    override fun onCleared() {
        super.onCleared()
        fileServerManager.stop()
    }
}
