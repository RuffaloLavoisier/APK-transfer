// UI 컴포저블을 모아둔 패키지 선언.
package com.android.app.apktransfer.ui

// 앱 아이콘을 비트맵으로 변환하기 위해 필요.
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
// 파일 선택 ActivityResult API.
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
// Compose 기반 UI 구성 요소들.
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// ViewModel 주입 헬퍼.
import androidx.lifecycle.viewmodel.compose.viewModel
// 앱 정보 모델.
import com.android.app.apktransfer.model.AppInfo
// 상태/로직을 제공하는 ViewModel.
import com.android.app.apktransfer.viewmodel.MainViewModel
import com.android.app.apktransfer.viewmodel.TransferMode

// 메인 화면 컴포저블: 전체 UI 상태와 동작을 연결한다.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun APKTransferScreen(viewModel: MainViewModel = viewModel()) {
    // ViewModel의 상태 스트림을 Compose 상태로 변환한다.
    val uiState by viewModel.uiState.collectAsState()

    // 설정 다이얼로그 표시 여부.
    var showSettingsDialog by remember { mutableStateOf(false) }
    // 로그를 전체 화면으로 볼지 여부.
    var showFullScreenLog by remember { mutableStateOf(false) }
    // 개발자 정보 다이얼로그 표시 여부.
    var showDeveloperInfo by remember { mutableStateOf(false) }

    // 현재 모드별 UI 분기를 쉽게 하기 위한 플래그.
    val isSelfServerMode = uiState.transferMode == TransferMode.SELF_SERVER
    val isRemoteUploadMode = uiState.transferMode == TransferMode.REMOTE_UPLOAD

    // 로그 리스트 스크롤 상태를 기억해 자동 스크롤에 활용한다.
    val logListState = rememberLazyListState()

    // 검색어에 따라 앱 목록을 필터링한다.
    val filteredPackages = remember(uiState.searchQuery, uiState.apps) {
        if (uiState.searchQuery.isBlank()) uiState.apps
        else uiState.apps.filter {
            it.name.contains(uiState.searchQuery, true) ||
                it.packageName.contains(uiState.searchQuery, true)
        }
    }

    // 파일 선택기를 띄우는 런처를 준비한다.
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        // 선택된 파일이 있으면 ViewModel로 전달한다.
        uri?.let { viewModel.onFilePicked(it) }
    }

    // 로그가 갱신될 때마다 최하단으로 스크롤한다.
    LaunchedEffect(uiState.logText) {
        if (uiState.logText.isNotEmpty()) {
            logListState.animateScrollToItem(
                logListState.layoutInfo.totalItemsCount.coerceAtLeast(0)
            )
        }
    }

    // 상단 앱바/본문을 포함하는 스캐폴드.
    Scaffold(
        // 전체 배경색을 테마 배경색으로 통일.
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // 상단 앱바: 브랜딩과 빠른 액션.
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 그라데이션 아이콘 배경 박스.
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF4FACFE),
                                            Color(0xFF00F2FE)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Upload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            // 앱 제목.
                            Text("APK Transfer", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            // 부제목으로 기능을 간단히 설명.
                            Text(
                                "Upload & Analyze",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                actions = {
                    // 개발자 정보 다이얼로그 열기.
                    IconButton(onClick = { showDeveloperInfo = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Developer Info")
                    }
                    // 설정 다이얼로그 열기.
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.shadow(4.dp)
            )
        }
    ) { paddingValues ->
        // 스크롤 콘텐츠와 상단 진행 표시를 겹쳐 배치한다.
        Box(modifier = Modifier.fillMaxSize()) {
            // 화면 전체를 스크롤 가능 컬럼으로 구성.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 검색 섹션 카드.
                ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Search Applications",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    // 검색 입력 필드.
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("name or package...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 검색 결과 개수를 표시한다.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "${filteredPackages.size} applications found",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // 앱 목록 카드(높이를 제한해 스크롤 성능 확보).
            ModernCard(modifier = Modifier.height(300.dp)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredPackages) { app ->
                        val isCopyingApp = isSelfServerMode &&
                            uiState.isCopyingApk &&
                            uiState.copyingPackageName == app.packageName
                        AppListItem(
                            app = app,
                            isUploading = uiState.isUploading,
                            isEnabled = !uiState.isUploading,
                            isCopying = isCopyingApp,
                            copyProgress = uiState.copyProgress,
                            onClick = { viewModel.onAppSelected(app) }
                        )
                    }
                }
            }

            // 전송 설정과 서버 상태를 묶는 컬럼.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 전송 모드 전환 카드.
                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "Transfer Mode",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        // 자체 서버/원격 업로드 모드 전환 UI.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Self-hosted server", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    if (isSelfServerMode) "Remote upload disabled"
                                    else "Upload to connected server",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = isSelfServerMode,
                                onCheckedChange = { enabled ->
                                    viewModel.setTransferMode(
                                        if (enabled) TransferMode.SELF_SERVER else TransferMode.REMOTE_UPLOAD
                                    )
                                },
                                // 업로드 중에는 모드 변경을 막아 상태 꼬임을 예방한다.
                                enabled = !uiState.isUploading
                            )
                        }
                    }
                }

                // 자체 파일 서버 상태 카드.
                ModernCard(modifier = Modifier.fillMaxHeight()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "File Transfer Server",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        // 서버 상태에 맞게 색/텍스트를 계산한다.
                        val statusColor = when {
                            !isSelfServerMode -> MaterialTheme.colorScheme.surfaceVariant
                            uiState.fileServerUrl != null -> Color(0xFF4CAF50)
                            else -> Color(0xFFF44336)
                        }
                        val statusTextColor = if (isSelfServerMode) Color.White else MaterialTheme.colorScheme.onSurface
                        val statusTitle = when {
                            !isSelfServerMode -> "Self Server Disabled"
                            uiState.fileServerUrl != null -> "Server Running"
                            else -> "Server Stopped"
                        }
                        val statusSubtitle = when {
                            !isSelfServerMode -> "Switch to self server mode to start"
                            uiState.fileServerUrl != null -> uiState.fileServerUrl
                            else -> "- Ready"
                        }

                        // 서버 상태 요약 영역.
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = statusColor
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 실행 여부에 따라 아이콘을 다르게 보여준다.
                                    Text(if (uiState.fileServerUrl != null) "🌐" else "❌", fontSize = 18.sp)
                                    Text(
                                        statusTitle,
                                        color = statusTextColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                // 서버 URL 또는 안내 문구를 표시한다.
                                if (statusSubtitle != null) {
                                    Text(
                                        statusSubtitle,
                                        color = statusTextColor.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // 선택된 파일 목록 표시 영역.
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            if (uiState.selectedFiles.isEmpty()) {
                                // 파일이 없으면 안내 문구를 중앙에 표시한다.
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No files selected",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                // 파일 목록을 스크롤 리스트로 보여준다.
                                LazyColumn(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(uiState.selectedFiles) { file ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.InsertDriveFile,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                file.name,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 선택 파일 수를 요약한다.
                        Text(
                            "${uiState.selectedFiles.size} file(s) selected",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        // 서버 제어/파일 선택 버튼 영역.
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.toggleServer() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isSelfServerMode,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.isServerRunning) Color(0xFFF44336) else Color(0xFF4CAF50)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    if (uiState.isServerRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (uiState.isServerRunning) "Stop Server" else "Start Server", fontSize = 13.sp)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.selectCacheFiles() },
                                    modifier = Modifier.weight(1f),
                                    enabled = isSelfServerMode,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.FolderOpen, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Select", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { viewModel.clearSelectedFiles() },
                                    modifier = Modifier.weight(1f),
                                    enabled = isSelfServerMode,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Clear", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // 활동 로그 카드.
                ModernCard(modifier = Modifier.height(350.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("Activity Log", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }

                            // 업로드 중이면 상태 배지를 표시한다.
                            if (uiState.isUploading) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⏳", fontSize = 14.sp)
                                    Text(
                                        "Uploading...",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // 로그 내용을 보여주는 영역(더블클릭 시 전체 화면).
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .pointerInput(Unit) {
                                    detectTapGestures(onDoubleTap = { showFullScreenLog = true })
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            LazyColumn(
                                state = logListState,
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (uiState.logText.isEmpty()) {
                                    // 로그가 없을 때 안내 UI.
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                    modifier = Modifier.size(40.dp)
                                                )
                                                Text(
                                                    "No activity yet",
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // 줄 단위로 로그를 분리해 렌더링한다.
                                    items(uiState.logText.split("\n")) { line ->
                                        if (line.isNotEmpty()) {
                                            LogLine(line)
                                        }
                                    }
                                }
                            }
                        }

                        // 업로드 진행 중일 때 진행 바를 보여준다.
                        if (uiState.isUploading && uiState.uploadProgress > 0) {
                            LinearProgressIndicator(
                                progress = uiState.uploadProgress,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // 원격 업로드 중에는 취소 버튼을 제공한다.
                        if (uiState.isUploading && isRemoteUploadMode) {
                            Button(
                                onClick = { viewModel.cancelUpload() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF44336)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Cancel Upload", fontSize = 12.sp)
                            }
                        }

                        // 하단 액션 버튼(탐색기/로그 지우기).
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    filePickerLauncher.launch("*/*")
                                    viewModel.addLog("[ACTION] Opening file explorer...")
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isUploading,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Explorer", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.clearLog() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Clear", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

        }
    }
    }

    // 설정 다이얼로그 표시 조건.
    if (showSettingsDialog) {
        SettingsDialog(
            currentServerUrl = uiState.serverUrl,
            onDismiss = { showSettingsDialog = false },
            onSave = { newUrl ->
                viewModel.saveServerUrl(newUrl)
                showSettingsDialog = false
            }
        )
    }

    // 로그 전체 화면 다이얼로그 표시 조건.
    if (showFullScreenLog) {
        FullScreenLogDialog(
            logText = uiState.logText,
            onDismiss = { showFullScreenLog = false },
            onClear = { viewModel.clearLog() }
        )
    }

    // 개발자 정보 다이얼로그 표시 조건.
    if (showDeveloperInfo) {
        DeveloperInfoDialog(onDismiss = { showDeveloperInfo = false })
    }
}

// 서버 URL을 수정하는 설정 다이얼로그.
@Composable
fun SettingsDialog(
    currentServerUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    // 입력 필드의 임시 값(저장 전까지 로컬 보관).
    var tempServerUrl by remember { mutableStateOf(currentServerUrl) }

    AlertDialog(
        // 바깥 클릭/뒤로가기 시 닫힘 처리.
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        icon = {
            // 상단 아이콘 배지.
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        },
        title = {
            Text(
                "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Cloud,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Server Configuration", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }

                    // 서버 URL 입력 필드.
                    OutlinedTextField(
                        value = tempServerUrl,
                        onValueChange = { tempServerUrl = it },
                        placeholder = { Text("http://192.168.0.100:80") },
                        leadingIcon = { Icon(Icons.Default.Link, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 입력 안내 문구.
                    Text(
                        "Enter the URL of your APK analysis server",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(tempServerUrl) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    )
}

// 로그를 전체 화면으로 보여주는 다이얼로그.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FullScreenLogDialog(
    logText: String,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    // 기본 Dialog를 확장해 전체 화면을 사용한다.
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Terminal, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Column {
                            Text("Activity Log", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Double-click to exit",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                // 전체 화면 로그 본문.
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = { onDismiss() })
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (logText.isEmpty()) {
                            // 로그가 없을 때 안내 메시지.
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Text("No activity yet", fontSize = 16.sp)
                                    }
                                }
                            }
                        } else {
                            // 줄바꿈 단위로 로그를 렌더링한다.
                            items(logText.split("\n")) { line ->
                                if (line.isNotEmpty()) {
                                    FullScreenLogLine(line)
                                }
                            }
                        }
                    }
                }

                // 하단 액션 버튼.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onClear(); onDismiss() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Clear Log", color = MaterialTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Close")
                    }
                }
            }
        }
    }
}

// 전체 화면 로그 라인을 아이콘/색과 함께 렌더링한다.
@Composable
fun FullScreenLogLine(text: String) {
    // 로그 프리픽스에 따라 아이콘과 색상을 매핑한다.
    val (icon, color) = when {
        text.startsWith("[APP]") -> Icons.Default.Android to Color(0xFF4FACFE)
        text.startsWith("[FILE]") -> Icons.Default.InsertDriveFile to Color(0xFF00F2FE)
        text.startsWith("[UPLOAD]") -> Icons.Default.CloudUpload to Color(0xFFFFA726)
        text.startsWith("[SUCCESS]") -> Icons.Default.CheckCircle to Color(0xFF66BB6A)
        text.startsWith("[ERROR]") -> Icons.Default.Error to Color(0xFFEF5350)
        text.startsWith("[INFO]") -> Icons.Default.Info to Color(0xFF42A5F5)
        text.startsWith("[ACTION]") -> Icons.Default.PlayArrow to Color(0xFF9575CD)
        text.startsWith("[SETTINGS]") -> Icons.Default.Settings to Color(0xFF26C6DA)
        text.startsWith("===") -> Icons.Default.Terminal to Color(0xFFFFD54F)
        else -> null to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 아이콘이 있을 때만 렌더링한다.
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = color)
        }
        Text(
            text,
            fontSize = 14.sp,
            color = color,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            modifier = Modifier.weight(1f),
            lineHeight = 20.sp
        )
    }
}

// 개발자/앱 정보를 보여주는 다이얼로그.
@Composable
fun DeveloperInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Upload, null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("APK Transfer", fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Version 1.0.0",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                InfoSection(
                    Icons.Default.Description,
                    "About",
                    "Upload and analyze APK files. Extract detailed information about Android applications."
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                InfoSection(Icons.Default.Person, "Developer", "Ruffalo Lavoisier")
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                InfoSection(Icons.Default.Email, "Contact", "ruffalolavoisier@gmail.com")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Close")
            }
        }
    )
}

// 아이콘/제목/본문으로 구성된 정보 섹션.
@Composable
fun InfoSection(icon: ImageVector, title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
        Text(content, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
    }
}

// 공통 카드 스타일을 재사용하기 위한 컴포저블.
@Composable
fun ModernCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        // 내부 패딩으로 콘텐츠 여백을 확보한다.
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

// 앱 리스트의 한 항목을 렌더링한다.
@Composable
fun AppListItem(
    app: AppInfo,
    isUploading: Boolean,
    isEnabled: Boolean,
    isCopying: Boolean,
    copyProgress: Float,
    onClick: () -> Unit
) {
    // 아이콘 로딩에 필요한 컨텍스트.
    val context = LocalContext.current
    // 패키지명 기준으로 아이콘 캐시를 유지한다.
    val appIcon = remember(app.packageName) {
        try {
            drawableToBitmap(app.applicationInfo.loadIcon(context.packageManager)).asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = copyProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "copyProgress"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        enabled = isEnabled && !isUploading,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (appIcon != null) {
                    // 앱 아이콘이 있으면 그대로 표시한다.
                    Image(bitmap = appIcon, contentDescription = app.name, modifier = Modifier.size(48.dp))
                } else {
                    // 아이콘 로딩 실패 시 대체 그래픽을 사용한다.
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Brush.linearGradient(listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Android, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }

            // 앱 이름/패키지명 정보 영역.
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    app.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    app.packageName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // split APK가 있으면 파일 개수를 표시한다.
                if (app.apkPaths.size > 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.InsertDriveFile,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${app.apkPaths.size} files",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 업로드/복사 상태 아이콘.
            val iconSize = 20.dp
            if (isCopying) {
                Box(
                    modifier = Modifier.size(iconSize),
                    contentAlignment = Alignment.Center
                ) {
                    // 진행률에 따라 아래에서부터 채워지는 레이어.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clipToBounds()
                            .align(Alignment.BottomCenter)
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    scaleY = animatedProgress
                                    transformOrigin = TransformOrigin(0.5f, 1f)
                                }
                        )
                    }
                }
            } else {
                Icon(
                    Icons.Default.CloudUpload,
                    null,
                    tint = if (isUploading)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

// 기본 로그 라인을 아이콘과 함께 렌더링한다.
@Composable
fun LogLine(text: String) {
    // 로그 프리픽스에 따라 아이콘/색상을 매핑한다.
    val (icon, color) = when {
        text.startsWith("[APP]") -> Icons.Default.Android to Color(0xFF4FACFE)
        text.startsWith("[FILE]") -> Icons.Default.InsertDriveFile to Color(0xFF00F2FE)
        text.startsWith("[UPLOAD]") -> Icons.Default.CloudUpload to Color(0xFFFFA726)
        text.startsWith("[SUCCESS]") -> Icons.Default.CheckCircle to Color(0xFF66BB6A)
        text.startsWith("[ERROR]") -> Icons.Default.Error to Color(0xFFEF5350)
        text.startsWith("[INFO]") -> Icons.Default.Info to Color(0xFF42A5F5)
        text.startsWith("[ACTION]") -> Icons.Default.PlayArrow to Color(0xFF9575CD)
        text.startsWith("[SETTINGS]") -> Icons.Default.Settings to Color(0xFF26C6DA)
        text.startsWith("===") -> Icons.Default.Terminal to Color(0xFFFFD54F)
        text.startsWith("[SERVER]") -> Icons.Default.Cloud to Color(0xFF26C6DA)
        text.startsWith("[CLEAR]") -> Icons.Default.Delete to Color(0xFFFF9800)
        else -> null to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 아이콘이 있으면 함께 표시한다.
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = color)
        }
        Text(
            text,
            fontSize = 12.sp,
            color = color,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}

// Drawable을 Bitmap으로 변환해 Compose Image에서 사용 가능하게 만든다.
private fun drawableToBitmap(drawable: Drawable): Bitmap {
    // 이미 BitmapDrawable이면 내부 비트맵을 재사용한다.
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }

    // 크기가 없는 Drawable은 1x1 비트맵으로 안전하게 처리한다.
    val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    } else {
        Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    }

    // 캔버스에 Drawable을 그려 실제 비트맵을 만든다.
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
