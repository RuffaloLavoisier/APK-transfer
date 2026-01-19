package com.android.app.apktransfer.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.app.apktransfer.model.AppInfo
import com.android.app.apktransfer.viewmodel.MainViewModel
import com.android.app.apktransfer.viewmodel.TransferMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun APKTransferScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFullScreenLog by remember { mutableStateOf(false) }
    var showDeveloperInfo by remember { mutableStateOf(false) }

    val isSelfServerMode = uiState.transferMode == TransferMode.SELF_SERVER
    val isRemoteUploadMode = uiState.transferMode == TransferMode.REMOTE_UPLOAD

    val logListState = rememberLazyListState()

    val filteredPackages = remember(uiState.searchQuery, uiState.apps) {
        if (uiState.searchQuery.isBlank()) uiState.apps
        else uiState.apps.filter {
            it.name.contains(uiState.searchQuery, true) ||
                it.packageName.contains(uiState.searchQuery, true)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onFilePicked(it) }
    }

    LaunchedEffect(uiState.logText) {
        if (uiState.logText.isNotEmpty()) {
            logListState.animateScrollToItem(
                logListState.layoutInfo.totalItemsCount.coerceAtLeast(0)
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                            Text("APK Transfer", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Upload & Analyze",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showDeveloperInfo = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Developer Info")
                    }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

            ModernCard(modifier = Modifier.height(300.dp)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredPackages) { app ->
                        AppListItem(
                            app = app,
                            isUploading = uiState.isUploading,
                            isEnabled = !uiState.isUploading,
                            onClick = { viewModel.onAppSelected(app) }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                                enabled = !uiState.isUploading
                            )
                        }
                    }
                }

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
                                    Text(if (uiState.fileServerUrl != null) "🌐" else "❌", fontSize = 18.sp)
                                    Text(
                                        statusTitle,
                                        color = statusTextColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

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

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            if (uiState.selectedFiles.isEmpty()) {
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

                        Text(
                            "${uiState.selectedFiles.size} file(s) selected",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

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

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .combinedClickable(
                                    onClick = {},
                                    onDoubleClick = { showFullScreenLog = true }
                                ),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            LazyColumn(
                                state = logListState,
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (uiState.logText.isEmpty()) {
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
                                    items(uiState.logText.split("\n")) { line ->
                                        if (line.isNotEmpty()) {
                                            LogLine(line)
                                        }
                                    }
                                }
                            }
                        }

                        if (uiState.isUploading && uiState.uploadProgress > 0) {
                            LinearProgressIndicator(
                                progress = uiState.uploadProgress,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

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

    if (showFullScreenLog) {
        FullScreenLogDialog(
            logText = uiState.logText,
            onDismiss = { showFullScreenLog = false },
            onClear = { viewModel.clearLog() }
        )
    }

    if (showDeveloperInfo) {
        DeveloperInfoDialog(onDismiss = { showDeveloperInfo = false })
    }
}

@Composable
fun SettingsDialog(
    currentServerUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var tempServerUrl by remember { mutableStateOf(currentServerUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        icon = {
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

                    OutlinedTextField(
                        value = tempServerUrl,
                        onValueChange = { tempServerUrl = it },
                        placeholder = { Text("http://192.168.0.100:80") },
                        leadingIcon = { Icon(Icons.Default.Link, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FullScreenLogDialog(
    logText: String,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
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

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .combinedClickable(onClick = {}, onDoubleClick = onDismiss),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (logText.isEmpty()) {
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
                            items(logText.split("\n")) { line ->
                                if (line.isNotEmpty()) {
                                    FullScreenLogLine(line)
                                }
                            }
                        }
                    }
                }

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

@Composable
fun FullScreenLogLine(text: String) {
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

@Composable
fun ModernCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun AppListItem(app: AppInfo, isUploading: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val appIcon = remember(app.packageName) {
        try {
            drawableToBitmap(app.applicationInfo.loadIcon(context.packageManager)).asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

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
                    Image(bitmap = appIcon, contentDescription = app.name, modifier = Modifier.size(48.dp))
                } else {
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

            Icon(
                Icons.Default.CloudUpload,
                null,
                tint = if (isUploading)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun LogLine(text: String) {
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

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }

    val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    } else {
        Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    }

    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
