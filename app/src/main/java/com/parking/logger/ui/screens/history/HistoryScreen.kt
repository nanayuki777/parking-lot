package com.parking.logger.ui.screens.history

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.parking.logger.data.model.ParkingRecord
import com.parking.logger.data.model.ParkingType
import com.parking.logger.data.repository.ParkingRepository
import com.parking.logger.data.repository.SessionWithTester
import com.parking.logger.util.AudioPlayer
import com.parking.logger.util.BackupExporter
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    repository: ParkingRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var data by remember { mutableStateOf<List<SessionWithTester>>(emptyList()) }
    var testerCount by remember { mutableStateOf(0) }
    var expandedSession by remember { mutableStateOf<Long?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var expandedRecordAudio by remember { mutableStateOf<Long?>(null) }

    suspend fun reload() {
        val all = repository.getAllDataForBackup()
        data = all
        testerCount = all.map { it.testerName }.distinct().size
    }

    LaunchedEffect(Unit) { reload() }

    val totalRecords = data.sumOf { it.records.size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史记录 & 备份") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            val all = repository.getAllDataForBackup()
                            val content = BackupExporter.buildJson(all)
                            val path = BackupExporter.saveBackup(
                                context, content,
                                all.map { it.testerName }.distinct().size,
                                all.size,
                                all.sumOf { it.records.size }
                            )
                            snackbarHostState.showSnackbar("备份已导出到: $path")
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "备份导出")
                    }
                    IconButton(onClick = { showClearConfirm = true }) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = "清除缓存",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (data.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "暂无历史记录",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "数据保存在本机，不会随退出丢失",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                item {
                    Text(
                        text = "共 ${data.size} 次测试 · $totalRecords 条泊车记录",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(data, key = { it.session.id }) { sw ->
                    SessionCard(
                        sw = sw,
                        isExpanded = expandedSession == sw.session.id,
                        onToggle = {
                            expandedSession = if (expandedSession == sw.session.id) null else sw.session.id
                        },
                        expandedRecordAudio = expandedRecordAudio,
                        onToggleAudio = { recordId ->
                            expandedRecordAudio = if (expandedRecordAudio == recordId) null else recordId
                        }
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清除所有缓存") },
            text = { Text("将永久删除本机所有测试记录和人员，此操作不可恢复！确定继续？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.clearAllData()
                        showClearConfirm = false
                        data = emptyList()
                    }
                }) {
                    Text("清除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SessionCard(
    sw: SessionWithTester,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    expandedRecordAudio: Long?,
    onToggleAudio: (Long) -> Unit
) {
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onToggle)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${sw.testerName} · ${sw.session.location}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${format.format(Date(sw.session.startTime))} · ${sw.records.size} 条记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                sw.records.forEach { r ->
                    RecordRow(r, expandedRecordAudio, onToggleAudio)
                }
            }
        }
    }
}

@Composable
private fun RecordRow(
    record: ParkingRecord,
    expandedRecordId: Long?,
    onToggleAudio: (Long) -> Unit
) {
    val context = LocalContext.current
    val displayIssue = if (record.issue.isNotEmpty()) record.issue else record.parkingType
    val ioLabel = if (record.isIO) "IO" else "不IO"
    val audioPlayer = remember { AudioPlayer(context) }
    var isPlaying by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // 基本信息行
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayIssue,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(80.dp)
            )
            Text(
                text = "${record.totalCount}把",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = ioLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = if (record.isIO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            // 有照片时显示图标
            if (record.photoPath.isNotBlank() && File(record.photoPath).exists()) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            // 有录音时显示播放按钮
            if (record.audioPath.isNotBlank() && File(record.audioPath).exists()) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { onToggleAudio(record.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (expandedRecordId == record.id) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (expandedRecordId == record.id) "停止播放" else "播放录音",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (record.note.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = record.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        // 展开的照片
        if (record.photoPath.isNotBlank() && File(record.photoPath).exists()) {
            Spacer(modifier = Modifier.height(4.dp))
            AsyncImage(
                model = File(record.photoPath),
                contentDescription = "现场照片",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )
        }

        // 展开的录音播放
        if (expandedRecordId == record.id && record.audioPath.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = {
                    if (isPlaying) {
                        audioPlayer.stop()
                        isPlaying = false
                    } else {
                        audioPlayer.play(record.audioPath)
                        isPlaying = true
                        audioPlayer.onCompletion = { isPlaying = false }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isPlaying) "停止播放" else "播放录音")
            }
        }
    }
}