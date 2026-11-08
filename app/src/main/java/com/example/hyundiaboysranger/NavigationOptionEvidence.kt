package com.example.hyundiaboysranger

/** Observations do not establish causality: the SDK exposes no request identifier. */
data class NavigationOptionEvidence(
    val before: String,
    val requested: String,
    val callback: String? = null,
    val after: String? = null,
    val phase: OptionPhase = OptionPhase.AWAITING_CALLBACK,
) {
    fun onCallback(option: String): NavigationOptionEvidence =
        if (phase != OptionPhase.AWAITING_CALLBACK) this
        else copy(callback = option, phase = if (option == requested) OptionPhase.AWAITING_REQUERY else phase)
    fun onRequery(option: String): NavigationOptionEvidence =
        if (phase != OptionPhase.AWAITING_REQUERY) this else copy(after = option, phase = OptionPhase.OBSERVED)
    fun interrupted(): NavigationOptionEvidence =
        if (phase == OptionPhase.OBSERVED) this else copy(phase = OptionPhase.UNCONFIRMED)
}

enum class OptionPhase { AWAITING_CALLBACK, AWAITING_REQUERY, OBSERVED, UNCONFIRMED }
