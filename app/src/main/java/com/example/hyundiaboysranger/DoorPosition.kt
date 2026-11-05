package com.example.hyundiaboysranger

enum class DoorPosition {
    OPEN, CLOSED, UNKNOWN;

    companion object {
        /** Interpret only the binary range documented by the SDK for this door area. */
        fun fromReading(value: Int?, minimum: Int?, maximum: Int?): DoorPosition {
            if (minimum != 0 || maximum != 1) return UNKNOWN
            return when (value) {
                0 -> CLOSED
                1 -> OPEN
                else -> UNKNOWN
            }
        }
    }
}
