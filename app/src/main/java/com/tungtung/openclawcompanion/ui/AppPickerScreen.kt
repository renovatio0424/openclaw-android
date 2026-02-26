package com.tungtung.openclawcompanion.ui

import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Bitmap
)

@Composable
fun AppPickerScreen(
    selectedPackages: List<String>,
    onSelectionChanged: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var allApps by remember { mutableStateOf<List<AppInfo>?>(null) }
    var search by remember { mutableStateOf("") }
    var selected by remember(selectedPackages) { mutableStateOf(selectedPackages.toSet()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .mapNotNull { app ->
                    runCatching {
                        AppInfo(
                            packageName = app.packageName,
                            label = app.loadLabel(pm).toString(),
                            icon = app.loadIcon(pm).toBitmap(48, 48)
                        )
                    }.getOrNull()
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
            allApps = apps
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("알림 앱 선택") },
                navigationIcon = {
                    IconButton(onClick = {
                        onSelectionChanged(selected.toList())
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        val apps = allApps
        if (apps == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        text = "앱 목록 불러오는 중...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("앱 검색") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
                Text(
                    text = "${selected.size}개 선택됨 / ${apps.size}개 앱",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val filtered = remember(search, apps, selected) {
                    val base = if (search.isBlank()) apps
                    else apps.filter {
                        it.label.contains(search, ignoreCase = true) ||
                                it.packageName.contains(search, ignoreCase = true)
                    }
                    // 선택된 앱을 상단에 표시
                    base.sortedByDescending { it.packageName in selected }
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.packageName }) { app ->
                        val checked = app.packageName in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (checked) selected - app.packageName
                                    else selected + app.packageName
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                bitmap = app.icon.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    selected = if (checked) selected - app.packageName
                                    else selected + app.packageName
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
