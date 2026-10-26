package com.parking.logger.ui.screens.pullout

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.parking.logger.data.repository.ParkingRepository
import kotlinx.coroutines.launch
import java.io.File

private val PULL_OUT_TYPES = listOf("侧方泊出", "直出")

private val PULL_OUT_ISSUES = listOf(
    "把数太多",
    "没有Park Out Offer",
    "功能中断",
    "碰撞风险",
    "车辆纵向控制",
    "其他"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullOutScreen(
    recordId: Long,
    repository: ParkingRepository,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedType by remember { mutableStateOf("") }
    var moveCount by remember { mutableStateOf("") }
    var isIO by remember { mutableStateOf(true) }
    var selectedIssues by remember { mutableStateOf(setOf<String>()) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoPath by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            photoPath = photoUri.toString()
        }
    }
    fun openCamera() {
        val photoFile = File(
            context.getExternalFilesDir(null),
            "ParkingLogger/photos/photo_${System.currentTimeMillis()}.jpg"
        )
        photoFile.parentFile?.mkdirs()
        photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        cameraLauncher.launch(photoUri!!)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openCamera()
        } else {
            Toast.makeText(context, "需要相机权限", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("泊出记录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
            Text("泊出类型", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                PULL_OUT_TYPES.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = moveCount,
                onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) moveCount = it },
                label = { Text("挪车把数") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // IO / 不IO 大按钮
            Text("IO 结果", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { isIO = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isIO) Color(0xFF2E7D32) else Color(0xFFBDBDBD),
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        if (isIO) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("IO", fontSize = 18.sp)
                }
                Button(
                    onClick = { isIO = false },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isIO) Color(0xFFC62828) else Color(0xFFBDBDBD),
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        if (!isIO) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("不 IO", fontSize = 18.sp)
                }
            }

            // 不 IO 时勾选问题
            if (!isIO) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("勾选问题", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        PULL_OUT_ISSUES.forEach { issue ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = issue in selectedIssues,
                                    onCheckedChange = {
                                        selectedIssues = if (issue in selectedIssues)
                                            selectedIssues - issue
                                        else
                                            selectedIssues + issue
                                    }
                                )
                                Text(issue, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        openCamera()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (photoPath.isNotEmpty()) "重新拍照（已拍）" else "拍照")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (moveCount.isEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("请输入挪车把数")
                        }
                        return@Button
                    }
                    val pullOutResult = if (isIO) "IO" else "不IO,${selectedIssues.joinToString(",")}"
                    scope.launch {
                        val existing = repository.getRecordById(recordId)
                        if (existing != null) {
                            repository.updateRecord(
                                existing.copy(
                                    pullOutType = selectedType,
                                    pullOutMoveCount = moveCount.toIntOrNull() ?: 0,
                                    pullOutResult = pullOutResult,
                                    photoPath = if (photoPath.isNotEmpty()) photoPath else existing.photoPath
                                )
                            )
                            onSaved()
                        } else {
                            snackbarHostState.showSnackbar("记录不存在，无法保存")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedType.isNotEmpty()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存并完成")
            }
        }
    }
}