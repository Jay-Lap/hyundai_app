package com.example.hyundiaboysranger

/** Ephemeral UI state; no vehicle values are persisted or logged. */
internal data class VehicleHealthUiState(
    val reading: VehicleHealth? = null,
    val loading: Boolean = false,
    val timestamp: String = "",
    val startedAt: Long = 0,
)

internal sealed interface VehicleHealthUiEvent {
    data class Started(val at: Long) : VehicleHealthUiEvent
    data class Finished(val reading: VehicleHealth, val timestamp: String) : VehicleHealthUiEvent
}

internal fun reduceVehicleHealth(state: VehicleHealthUiState, event: VehicleHealthUiEvent): VehicleHealthUiState = when (event) {
    is VehicleHealthUiEvent.Started -> VehicleHealthUiState(loading = true, startedAt = event.at)
    is VehicleHealthUiEvent.Finished -> state.copy(reading = event.reading, loading = false, timestamp = event.timestamp)
}
