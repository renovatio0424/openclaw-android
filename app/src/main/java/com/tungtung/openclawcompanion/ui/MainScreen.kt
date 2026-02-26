package com.tungtung.openclawcompanion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tungtung.openclawcompanion.R
import com.tungtung.openclawcompanion.data.ConnectionState
import com.tungtung.openclawcompanion.data.ForwardedEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    events: List<ForwardedEvent>,
    state: ConnectionState,
    appCount: Int,
    smsKeywords: List<String>,
    onToggleService: () -> Unit,
    onOpenAppPicker: () -> Unit,
    onOpenSmsFilter: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onToggleService) {
                if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ConnectionStatusBadge(state = state)
            ShortcutCards(
                appCount = appCount,
                smsKeywords = smsKeywords,
                onOpenAppPicker = onOpenAppPicker,
                onOpenSmsFilter = onOpenSmsFilter
            )
            EventList(events = events, modifier = Modifier.fillMaxSize())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShortcutCards(
    appCount: Int,
    smsKeywords: List<String>,
    onOpenAppPicker: () -> Unit,
    onOpenSmsFilter: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ShortcutCard(
            icon = Icons.Default.Notifications,
            title = "알림 앱 설정",
            subtitle = if (appCount > 0) "${appCount}개 앱 선택됨" else "앱을 선택하세요",
            onClick = onOpenAppPicker
        )
        ShortcutCard(
            icon = Icons.Default.Message,
            title = "SMS 키워드 설정",
            subtitle = if (smsKeywords.isNotEmpty()) smsKeywords.joinToString(", ")
            else "키워드를 등록하세요",
            onClick = onOpenSmsFilter
        )
    }
}

@Composable
private fun ShortcutCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConnectionStatusBadge(state: ConnectionState) {
    val color = when (state) {
        ConnectionState.CONNECTED -> Color(0xFF2E7D32)
        ConnectionState.CONNECTING -> Color(0xFFF9A825)
        ConnectionState.ERROR -> Color(0xFFC62828)
        ConnectionState.DISCONNECTED -> Color(0xFF546E7A)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Text(
            text = statusLabel(state),
            modifier = Modifier
                .background(color.copy(alpha = 0.15f))
                .padding(16.dp),
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EventList(events: List<ForwardedEvent>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(events, key = { "${it.timestamp}-${it.source}-${it.type}" }) { event ->
            EventCard(event)
        }
    }
}

@Composable
private fun EventCard(event: ForwardedEvent) {
    val formatter = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()) }
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = formatter.format(Date(event.timestamp)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AssistChip(
                onClick = {},
                label = { Text(text = event.type) },
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = event.source,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = event.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun statusLabel(state: ConnectionState): String = when (state) {
    ConnectionState.CONNECTED -> "연결됨"
    ConnectionState.CONNECTING -> "연결 중"
    ConnectionState.ERROR -> "오류"
    ConnectionState.DISCONNECTED -> "연결 끊김"
}
