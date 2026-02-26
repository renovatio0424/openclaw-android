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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tungtung.openclawcompanion.data.FilterConfig

@Composable
fun FilterScreen(
    initialConfig: FilterConfig,
    appWhitelist: List<String>,
    onSave: (FilterConfig) -> Unit,
    onBack: () -> Unit,
    onOpenAppPicker: () -> Unit,
    onOpenSmsHistory: () -> Unit
) {
    var smsIncludeKeywords by remember(initialConfig) { mutableStateOf(initialConfig.smsIncludeKeywords) }
    var blockedKeywords by remember(initialConfig) { mutableStateOf(initialConfig.blockedKeywords) }

    var smsKeywordInput by remember { mutableStateOf("") }
    var blockedInput by remember { mutableStateOf("") }

    fun currentConfig() = FilterConfig(
        appWhitelist = appWhitelist,
        smsIncludeKeywords = smsIncludeKeywords,
        blockedKeywords = blockedKeywords
    )

    fun saveNow() { onSave(currentConfig()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "필터 설정") },
                navigationIcon = {
                    IconButton(onClick = {
                        saveNow()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
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
            Section(title = "앱 알림 허용") {
                Text(
                    text = "${appWhitelist.size}개 앱 선택됨",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        saveNow()
                        onOpenAppPicker()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("앱 선택하기")
                }
            }

            Section(title = "SMS 포함 키워드") {
                Text(
                    text = "아래 키워드가 포함된 SMS만 전달됩니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ChipGroup(values = smsIncludeKeywords, onRemove = { value ->
                    smsIncludeKeywords = smsIncludeKeywords.minus(value)
                    saveNow()
                })
                LinedTextField(
                    value = smsKeywordInput,
                    label = "키워드 추가 (예: 인증, 배송, 결제)",
                    onValueChange = { smsKeywordInput = it },
                    onAdd = {
                        val trimmed = smsKeywordInput.trim()
                        if (trimmed.isNotEmpty() && trimmed !in smsIncludeKeywords) {
                            smsIncludeKeywords = listOf(trimmed) + smsIncludeKeywords
                            smsKeywordInput = ""
                            saveNow()
                        }
                    }
                )
                OutlinedButton(
                    onClick = {
                        saveNow()
                        onOpenSmsHistory()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("매칭 기록 보기")
                }
            }

            Section(title = "차단 키워드") {
                Text(
                    text = "알림/SMS에 아래 키워드가 포함되면 차단됩니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ChipGroup(values = blockedKeywords, onRemove = { value ->
                    blockedKeywords = blockedKeywords.minus(value)
                    saveNow()
                })
                LinedTextField(
                    value = blockedInput,
                    label = "키워드 추가",
                    onValueChange = { blockedInput = it },
                    onAdd = {
                        val trimmed = blockedInput.trim()
                        if (trimmed.isNotEmpty() && trimmed !in blockedKeywords) {
                            blockedKeywords = listOf(trimmed) + blockedKeywords
                            blockedInput = ""
                            saveNow()
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
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(text = value) },
        trailingIcon = {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(InputChipDefaults.AvatarSize)
            )
        }
    )
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
