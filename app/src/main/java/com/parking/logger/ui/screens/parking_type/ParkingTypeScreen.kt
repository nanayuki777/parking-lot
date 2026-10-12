package com.parking.logger.ui.screens.parking_type

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DriveEta
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.parking.logger.data.model.ParkingRecord
import com.parking.logger.data.model.ParkingType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingTypeScreen(
    records: List<ParkingRecord>,
    onTypeSelected: (ParkingType) -> Unit,
    onEndTest: () -> Unit,
    onBack: () -> Unit
) {
    val countById = records.groupingBy { it.parkingType }.eachCount()
    val totalRecords = records.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择泊车类型") },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "本次已记录 $totalRecords 条",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onEndTest) {
                    Text("结束测试", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            TypeCard(
                icon = Icons.Default.DriveEta,
                label = "正入",
                description = "车头正向驶入车位",
                count = countById[ParkingType.FRONT_IN.name] ?: 0,
                onClick = { onTypeSelected(ParkingType.FRONT_IN) }
            )
            TypeCard(
                icon = Icons.Default.UnfoldMore,
                label = "反入",
                description = "倒车入库",
                count = countById[ParkingType.REVERSE_IN.name] ?: 0,
                onClick = { onTypeSelected(ParkingType.REVERSE_IN) }
            )
            TypeCard(
                icon = Icons.Default.SwapHoriz,
                label = "侧方",
                description = "路边侧方停车",
                count = countById[ParkingType.SIDE_PARKING.name] ?: 0,
                onClick = { onTypeSelected(ParkingType.SIDE_PARKING) }
            )
            TypeCard(
                icon = Icons.Default.DirectionsCar,
                label = "手动泊入后的泊出",
                description = "泊入已完成，记录泊出表现",
                count = countById[ParkingType.MANUAL_PULL_OUT.name] ?: 0,
                onClick = { onTypeSelected(ParkingType.MANUAL_PULL_OUT) }
            )
        }
    }
}

@Composable
private fun TypeCard(
    icon: ImageVector,
    label: String,
    description: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Badge {
                Text("$count 次")
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
