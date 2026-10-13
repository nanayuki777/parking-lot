package com.parking.logger.ui.screens.pullout

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.parking.logger.data.repository.ParkingRepository
import kotlinx.coroutines.launch
import java.io.File

private val PULL_OUT_TYPES = listOf("侧方泊出", "直出")
private val PULL_OUT_RESULTS = listOf("OK", "不OK", "把数太多", "没有Park Out Offer", "功能中断", "碰撞风险", "车辆纵向控制")

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
    var selectedResult by remember { mutableStateOf("") }
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

            Text("结果", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            PULL_OUT_RESULTS.forEach { result ->
                FilterChip(
                    selected = selectedResult == result,
                    onClick = { selectedResult = result },
                    label = { Text(result) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
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
                    scope.launch {
                        val existing = repository.getRecordById(recordId)
                        if (existing != null) {
                            repository.updateRecord(
                                existing.copy(
                                    pullOutType = selectedType,
                                    pullOutMoveCount = moveCount.toIntOrNull() ?: 0,
                                    pullOutResult = selectedResult,
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
                enabled = selectedType.isNotEmpty() && selectedResult.isNotEmpty()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存并完成")
            }
        }
    }
}
