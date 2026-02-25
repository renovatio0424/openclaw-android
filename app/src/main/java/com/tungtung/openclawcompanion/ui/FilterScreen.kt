package com.tungtung.openclawcompanion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tungtung.openclawcompanion.data.FilterConfig

@Composable
fun FilterScreen(
    initialConfig: FilterConfig,
    onSave: (FilterConfig) -> Unit,
    onBack: () -> Unit
) {
    var appWhitelist by remember(initialConfig) { mutableStateOf(initialConfig.appWhitelist) }
    var smsWhitelist by remember(initialConfig) { mutableStateOf(initialConfig.smsWhitelist) }
    var blockedKeywords by remember(initialConfig) { mutableStateOf(initialConfig.blockedKeywords) }
    var smsWhitelistAll by remember(initialConfig) { mutableStateOf(initialConfig.smsWhitelistAll) }

    var appInput by remember { mutableStateOf("") }
    var smsInput by remember { mutableStateOf("") }
    var keywordInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "필터 설정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val config = FilterConfig(
                        appWhitelist = appWhitelist,
                        smsWhitelist = smsWhitelist,
                        smsWhitelistAll = smsWhitelistAll,
                        blockedKeywords = blockedKeywords
                    )
                    onSave(config)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = "저장")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Section(title = "앱 알림 허용 목록") {
                ChipGroup(values = appWhitelist, onRemove = { value ->
                    appWhitelist = appWhitelist.minus(value)
                })
                LinedTextField(
                    value = appInput,
                    label = "패키지 이름 추가",
                    onValueChange = { appInput = it },
                    onAdd = {
                        val trimmed = appInput.trim()
                        if (trimmed.isNotEmpty() && trimmed !in appWhitelist) {
                            appWhitelist = listOf(trimmed) + appWhitelist
                            appInput = ""
                        }
                    }
                )
            }

            Section(title = "SMS 필터") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "모든 번호 허용")
                    Switch(checked = smsWhitelistAll, onCheckedChange = { smsWhitelistAll = it })
                }
                ChipGroup(values = smsWhitelist, onRemove = { value ->
                    smsWhitelist = smsWhitelist.minus(value)
                })
                LinedTextField(
                    value = smsInput,
                    label = "번호 추가",
                    onValueChange = { smsInput = it },
                    onAdd = {
                        val trimmed = smsInput.trim()
                        if (trimmed.isNotEmpty() && trimmed !in smsWhitelist) {
                            smsWhitelist = listOf(trimmed) + smsWhitelist
                            smsInput = ""
                        }
                    }
                )
            }

            Section(title = "차단 키워드") {
                ChipGroup(values = blockedKeywords, onRemove = { value ->
                    blockedKeywords = blockedKeywords.minus(value)
                })
                LinedTextField(
                    value = keywordInput,
                    label = "키워드 추가",
                    onValueChange = { keywordInput = it },
                    onAdd = {
                        val trimmed = keywordInput.trim()
                        if (trimmed.isNotEmpty() && trimmed !in blockedKeywords) {
                            blockedKeywords = listOf(trimmed) + blockedKeywords
                            keywordInput = ""
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(values: List<String>, onRemove: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        values.forEach { value ->
            ChipItem(value = value, onRemove = { onRemove(value) })
        }
    }
}

@Composable
private fun ChipItem(value: String, onRemove: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.small) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = value)
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = null)
            }
        }
    }
}

@Composable
private fun LinedTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            TextButton(onClick = onAdd) {
                Text(text = "추가")
            }
        }
    )
}
