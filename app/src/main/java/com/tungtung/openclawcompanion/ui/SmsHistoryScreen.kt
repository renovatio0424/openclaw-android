package com.tungtung.openclawcompanion.ui

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tungtung.openclawcompanion.data.SmsHistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SmsHistoryScreen(
    keywords: List<String>,
    blockedKeywords: List<String> = emptyList(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<SmsHistoryEntry>?>(null) }

    LaunchedEffect(keywords, blockedKeywords) {
        withContext(Dispatchers.IO) {
            entries = queryMatchingSms(context, keywords, blockedKeywords)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS 매칭 기록 (${entries?.size ?: "..."})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        val list = entries
        if (list == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        text = "SMS 검색 중...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        } else if (list.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (keywords.isEmpty()) "SMS 포함 키워드를 먼저 등록하세요"
                    else "매칭된 SMS가 없습니다",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list, key = { "${it.timestamp}-${it.sender}-${it.matchedKeyword}" }) { entry ->
                    SmsHistoryCard(entry)
                }
            }
        }
    }
}

private fun queryMatchingSms(context: Context, keywords: List<String>, blockedKeywords: List<String> = emptyList()): List<SmsHistoryEntry> {
    if (keywords.isEmpty()) return emptyList()

    val results = mutableListOf<SmsHistoryEntry>()
    val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
    val projection = arrayOf(
        Telephony.Sms.ADDRESS,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE
    )

    runCatching {
        context.contentResolver.query(
            uri, projection, null, null,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (cursor.moveToNext() && results.size < 200) {
                val body = cursor.getString(bodyIdx) ?: continue
                val matchedKeyword = keywords.firstOrNull {
                    body.contains(it, ignoreCase = true)
                } ?: continue

                if (blockedKeywords.any { body.contains(it, ignoreCase = true) }) continue

                results.add(
                    SmsHistoryEntry(
                        timestamp = cursor.getLong(dateIdx),
                        sender = cursor.getString(addressIdx) ?: "알 수 없음",
                        body = body,
                        matchedKeyword = matchedKeyword
                    )
                )
            }
        }
    }

    return results
}

@Composable
private fun SmsHistoryCard(entry: SmsHistoryEntry) {
    val formatter = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()) }
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatter.format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AssistChip(
                    onClick = {},
                    label = { Text(entry.matchedKeyword) }
                )
            }
            Text(
                text = entry.sender,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = entry.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
