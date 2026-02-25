package com.tungtung.openclawcompanion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tungtung.openclawcompanion.data.ConnectionState
import com.tungtung.openclawcompanion.data.ForwardedEvent
import com.tungtung.openclawcompanion.data.GatewayClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel() {
    val connectionState: StateFlow<ConnectionState> =
        GatewayClient.state.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            GatewayClient.state.value
        )

    private val _events = MutableStateFlow<List<ForwardedEvent>>(emptyList())
    val events: StateFlow<List<ForwardedEvent>> = _events.asStateFlow()

    fun addEvent(event: ForwardedEvent) {
        _events.update { current ->
            val updated = listOf(event) + current
            if (updated.size > 50) updated.take(50) else updated
        }
    }
}
