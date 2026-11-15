package com.example.hyundiaboysranger

import androidx.compose.runtime.Composable

@Composable
internal fun VariantParkedContent(evidence: ParkingEvidence, currentParking: () -> ParkingEvidence) {
    DrivingHelperApp(parkingEvidence = evidence, currentParking = currentParking)
}
