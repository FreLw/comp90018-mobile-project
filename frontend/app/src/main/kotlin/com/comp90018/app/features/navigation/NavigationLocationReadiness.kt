package com.comp90018.app.features.navigation

import com.comp90018.app.features.map.LocationActionPolicy
import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.location.LocationAvailabilityState
import com.comp90018.app.sensors.location.LocationOutput
import com.comp90018.app.sensors.location.LocationPermissionState

/** Presentation reasons derived from existing sensor metadata, not another GPS validator. */
internal enum class NavigationLocationReadiness {
    READY,
    ACQUIRING,
    ACCESS_REQUIRED,
    IMPROVING_SIGNAL,
    UNAVAILABLE,
}

internal fun navigationLocationReadiness(
    location: LocationOutput,
    debugSimulationActive: Boolean = false,
): NavigationLocationReadiness = when {
    debugSimulationActive -> NavigationLocationReadiness.READY
    LocationActionPolicy.actionableCoordinate(location) != null -> NavigationLocationReadiness.READY
    location.permission == LocationPermissionState.DENIED -> NavigationLocationReadiness.ACCESS_REQUIRED
    location.permission == LocationPermissionState.UNKNOWN -> NavigationLocationReadiness.ACQUIRING
    location.availability == LocationAvailabilityState.EXPIRED ||
        location.availability == LocationAvailabilityState.UNAVAILABLE -> NavigationLocationReadiness.UNAVAILABLE
    location.validity == SensorValidity.UNRELIABLE ||
        location.availability == LocationAvailabilityState.RECOVERING -> NavigationLocationReadiness.IMPROVING_SIGNAL
    else -> NavigationLocationReadiness.ACQUIRING
}

internal fun navigationGuidingThreadAvailable(state: RelicNavigationUiState): Boolean =
    state.locationReadiness == NavigationLocationReadiness.READY && state.distanceMeters != null
