package com.parking.logger.ui.screens.evaluation

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parking.logger.data.repository.ParkingRepository
import com.parking.logger.util.AudioRecorder
import com.parking.logger.util.VoiceDistanceListener
import com.parking.logger.util.VoiceDistanceParser
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationScreen(
    sessionId: Long,
    parkingTypeName: String,
    conditionName: String,
    boundaryName: String,
    totalCount: Int,
    repository: ParkingRepository,
    onRecordSaved: (Long, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: EvaluationViewModel = viewModel(
        key = "$sessionId-$parkingTypeName-$conditionName-$boundaryName-$totalCount",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return EvaluationViewModel(
                    repository, sessionId, parkingTypeName, conditionName, boundaryName, totalCount
                ) as T
            }
        }
    )

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val isIO by viewModel.isIO.collectAsState()
    val selectedIssues by viewModel.selectedIssues.collectAsState()
    val note by viewModel.note.collectAsState()
    val distanceBL by viewModel.distanceBL.collectAsState()
    val distanceBack by viewModel.distanceBack.collectAsState()
    val distanceBR by viewModel.distanceBR.collectAsState()
    val distanceFR by viewModel.distanceFR.collectAsState()
    val distanceFront by viewModel.distanceFront.collectAsState()
    val distanceFL by viewModel.distanceFL.collectAsState()
    val savedRecordId by viewModel.savedRecordId.collectAsState()
    val error by viewModel.error.collectAsState()

    // Camera launcher
    var photoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            viewModel.setPhotoPath(photoUri.toString())
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val photoFile = File(
                context.getExternalFilesDir(null), "ParkingLogger/photos/photo_${System.currentTimeMillis()}.jpg"
            )
            photoFile.parentFile?.mkdirs()
            photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            cameraLauncher.launch(photoUri!!)
        } else {
            Toast.makeText(context, "需要相机权限", Toast.LENGTH_SHORT).show()
        }
    }

    // Audio recording
    var isRecording by remember { mutableStateOf(false) }
    var audioFilename by remember { mutableStateOf<String?>(null) }
    val audioRecorder = remember { AudioRecorder(context) }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val file = audioRecorder.start()
                audioFilename = file.absolutePath
                viewModel.setAudioPath(file.toString())
                isRecording = true
            } catch (e: Exception) {
                Toast.makeText(context, "录音启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "需要录音权限", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleRecording() {
        if (isRecording) {
            val path = audioRecorder.stop()
            if (path != null) {
                audioFilename = path
                viewModel.setAudioPath(path)
                Toast.makeText(context, "录音已保存", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "录音文件为空，未保存", Toast.LENGTH_SHORT).show()
            }
            isRecording = false
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    val file = audioRecorder.start()
                    audioFilename = file.absolutePath
                    viewModel.setAudioPath(file.toString())
                    isRecording = true
                } catch (e: Exception) {
                    Toast.makeText(context, "录音启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // ------ 语音距离录入 ------
    val voiceListener = remember { VoiceDistanceListener(context) }
    var isVoiceInputActive by remember { mutableStateOf(false) }
    var skippedIndices by remember { mutableStateOf(setOf<Int>()) }
    val hasSpeechRecognition = remember { voiceListener.isAvailable }

    val voicePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceListener.start()
            isVoiceInputActive = true
        } else {
            Toast.makeText(context, "需要录音权限才能语音录入", Toast.LENGTH_SHORT).show()
        }
    }

    fun startVoiceInput() {
        if (!hasSpeechRecognition) {
            Toast.makeText(context, "设备不支持语音识别", Toast.LENGTH_SHORT).show()
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            voiceListener.start()
            isVoiceInputActive = true
        } else {
            voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopVoiceInput() {
        voiceListener.stop()
        isVoiceInputActive = false
    }

    // 处理语音识别结果：填到对应字段
    voiceListener.onSpoken = { targetIndex, value, skip, _ ->
        val setters: List<(String) -> Unit> = listOf(
            viewModel::updateDistanceBL, viewModel::updateDistanceBack, viewModel::updateDistanceBR,
            viewModel::updateDistanceFR, viewModel::updateDistanceFront, viewModel::updateDistanceFL
        )
        fun firstAvailable(filled: List<Boolean>, skipped: Set<Int>): Int? {
            for (i in 0 until 6) {
                if (!filled[i] && i !in skipped) return i
            }
            return null
        }
        val filledByIndex = listOf(
            distanceBL.isNotBlank(), distanceBack.isNotBlank(), distanceBR.isNotBlank(),
            distanceFR.isNotBlank(), distanceFront.isNotBlank(), distanceFL.isNotBlank()
        )

        when {
            // 明确的第N个 / 标签字段
            targetIndex != null -> {
                if (skip) {
                    skippedIndices = skippedIndices + targetIndex
                    Toast.makeText(context, "${VoiceDistanceParser.FIELDS[targetIndex]} 已跳过", Toast.LENGTH_SHORT).show()
                } else if (value != null) {
                    setters[targetIndex](value.toString())
                    skippedIndices = skippedIndices - targetIndex
                }
            }
            // 无编号，但有数字 → 顺序填下一个可用
            value != null -> {
                val idx = firstAvailable(filledByIndex, skippedIndices)
                if (idx != null) {
                    setters[idx](value.toString())
                } else {
                    Toast.makeText(context, "六个距离已填满", Toast.LENGTH_SHORT).show()
                    stopVoiceInput()
                }
            }
            // 无编号，仅跳过词 → 跳过下一个可用字段
            skip -> {
                val idx = firstAvailable(filledByIndex, skippedIndices)
                if (idx != null) {
                    skippedIndices = skippedIndices + idx
                    Toast.makeText(context, "${VoiceDistanceParser.FIELDS[idx]} 已跳过", Toast.LENGTH_SHORT).show()
                }
            }
            else -> Unit
        }
    }
    voiceListener.onStatusChanged = { active ->
        isVoiceInputActive = active
    }
    voiceListener.onError = { msg ->
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        isVoiceInputActive = false
    }

    DisposableEffect(Unit) {
        onDispose {
            audioRecorder.release()
            voiceListener.release()
        }
    }

    LaunchedEffect(savedRecordId) {
        savedRecordId?.let {
            onRecordSaved(it, viewModel.needsPullOut)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("结果评估") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回上一步")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Section: Summary card (total count + parking type)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "泊车类型: ${viewModel.parkingTypeLabel}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "本次泊车共用 $totalCount 把",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "条件: $conditionName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section: IO choice
            Text(
                text = "IO 结果",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.FilterChip(
                    selected = isIO,
                    onClick = { viewModel.setIsIO(true) },
                    label = { Text("IO") },
                    leadingIcon = {
                        Icon(
                            if (isIO) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.padding(end = 8.dp)
                )
                androidx.compose.material3.FilterChip(
                    selected = !isIO,
                    onClick = { viewModel.setIsIO(false) },
                    label = { Text("不 IO") },
                    leadingIcon = {
                        Icon(
                            if (!isIO) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                )
            }

            // Section: Issues (only when not IO)
            if (!isIO) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "勾选问题",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        EvaluationViewModel.ISSUES.forEach { issue ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = issue in selectedIssues,
                                    onCheckedChange = { viewModel.toggleIssue(issue) }
                                )
                                Text(
                                    text = issue,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Note field
            OutlinedTextField(
                value = note,
                onValueChange = viewModel::updateNote,
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Distance measurements section
            Text(
                text = "距离测量",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = distanceBL,
                    onValueChange = viewModel::updateDistanceBL,
                    label = { Text("BL") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("BL ") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = distanceBack,
                    onValueChange = viewModel::updateDistanceBack,
                    label = { Text("Back") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = distanceBR,
                    onValueChange = viewModel::updateDistanceBR,
                    label = { Text("BR") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("BR ") }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = distanceFR,
                    onValueChange = viewModel::updateDistanceFR,
                    label = { Text("FR") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("FR ") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = distanceFront,
                    onValueChange = viewModel::updateDistanceFront,
                    label = { Text("Front") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = distanceFL,
                    onValueChange = viewModel::updateDistanceFL,
                    label = { Text("FL") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("FL ") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 语音录入距离
            OutlinedButton(
                onClick = {
                    if (isVoiceInputActive) stopVoiceInput()
                    else startVoiceInput()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = hasSpeechRecognition
            ) {
                Icon(
                    if (isVoiceInputActive) Icons.Default.Stop else Icons.Default.GraphicEq,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (isVoiceInputActive) "正在监听… 可说：第一个/第二个…或 BL/FR 标签，或直接报数字；跳过/不填可留空"
                    else "语音录入距离"
                )
            }
            if (isVoiceInputActive) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "● 连续监听中：报“第一个 2.5”或“BL 2.5”填对应字段；报“跳过”可留空该字段",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Photo & Audio buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            val photoFile = File(
                                context.getExternalFilesDir(null),
                                "ParkingLogger/photos/photo_${System.currentTimeMillis()}.jpg"
                            )
                            photoFile.parentFile?.mkdirs()
                            photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                            cameraLauncher.launch(photoUri!!)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("拍照")
                }

                OutlinedButton(
                    onClick = { toggleRecording() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isRecording) "结束录音" else "录音")
                }
            }

            if (isRecording) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "● 正在录音…",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存记录")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

