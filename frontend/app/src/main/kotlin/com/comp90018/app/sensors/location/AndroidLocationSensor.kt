package com.comp90018.app.sensors.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.comp90018.app.diagnostics.SensorComponent
import com.comp90018.app.diagnostics.SensorErrorReporter
import com.comp90018.app.sensors.SensorValidity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationAvailability as GoogleLocationAvailability
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidLocationSensor(
    context: Context,
    config: LocationConfig = LocationConfig(),
) : LocationSensor {
    private val appContext = context.applicationContext
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(appContext)
    private val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL_MILLIS)
        .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL_MILLIS)
        .setMaxUpdateDelayMillis(LOCATION_MAX_DELAY_MILLIS)
        .build()

    private val _output = MutableStateFlow(
        LocationOutput(
            permission = permissionState(),
            availability = LocationAvailabilityState.UNKNOWN,
        ),
    )
    override val output: StateFlow<LocationOutput> = _output.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var activeConfig = config
    private var targetLocation: GeoCoordinate? = null
    private var lastReading: LocationReading? = null
    private var started = false
    private var providerAvailable: Boolean? = null

    /** State for the "reading went stale -> retry getCurrentLocation" recovery cycle. */
    private var staleRecoveryPending = false
    private var staleRecoveryAttempts = 0
    private var expired = false
    private val staleRecoveryRunnable = Runnable { attemptStaleRecovery() }

    private val staleRefresh = object : Runnable {
        override fun run() {
            if (!started) return
            refreshOutput()
            handler.postDelayed(this, STALE_REFRESH_INTERVAL_MILLIS)
        }
    }

    private val callback = object : LocationCallback() {
        override fun onLocationAvailability(availability: GoogleLocationAvailability) {
            providerAvailable = availability.isLocationAvailable
            refreshOutput()
        }

        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            onLocation(location)
        }
    }

    override fun setTargetLocation(targetLocation: GeoCoordinate?) {
        this.targetLocation = targetLocation
        refreshOutput()
    }

    override fun setTargetLocation(
        targetLocation: GeoCoordinate?,
        insideRadiusMeters: Double,
        nearbyRadiusMeters: Double,
    ) {
        activeConfig = activeConfig.copy(
            insideRadiusMeters = insideRadiusMeters,
            nearbyRadiusMeters = nearbyRadiusMeters,
        )
        setTargetLocation(targetLocation)
    }

    @SuppressLint("MissingPermission")
    override fun start() {
        if (permissionState() != LocationPermissionState.GRANTED) {
            started = false
            providerAvailable = null
            handler.removeCallbacks(staleRefresh)
            fusedLocationClient.removeLocationUpdates(callback)
            resetStaleRecovery()
            refreshOutput()
            return
        }
        if (started) {
            refreshOutput()
            return
        }
        started = true
        providerAvailable = null
        resetStaleRecovery()
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) onLocation(location)
        }
        fusedLocationClient.locationAvailability.addOnSuccessListener { availability ->
            providerAvailable = availability.isLocationAvailable
            refreshOutput()
        }
        fusedLocationClient.requestLocationUpdates(request, callback, appContext.mainLooper)
        handler.removeCallbacks(staleRefresh)
        handler.postDelayed(staleRefresh, STALE_REFRESH_INTERVAL_MILLIS)
        refreshOutput()
    }

    override fun stop() {
        started = false
        providerAvailable = null
        handler.removeCallbacks(staleRefresh)
        fusedLocationClient.removeLocationUpdates(callback)
        resetStaleRecovery()
        refreshOutput()
    }

    fun refreshPermissionState() {
        if (permissionState() != LocationPermissionState.GRANTED) {
            started = false
            providerAvailable = null
            handler.removeCallbacks(staleRefresh)
            fusedLocationClient.removeLocationUpdates(callback)
            resetStaleRecovery()
        }
        refreshOutput()
    }

    private fun onLocation(location: Location) {
        lastReading = LocationReading(
            coordinate = GeoCoordinate(location.latitude, location.longitude),
            accuracyMeters = location.accuracy.takeIf { location.hasAccuracy() }?.toDouble(),
            timestampNanos = location.elapsedRealtimeNanos,
        )
        resetStaleRecovery()
        refreshOutput()
    }

    /**
     * A reading is stale once it hasn't been refreshed within [LocationConfig.staleTimeoutNanos].
     * Rather than declaring the location dead immediately, actively re-poll
     * [FusedLocationProviderClient.getCurrentLocation] a couple of times before giving up - the
     * passive [callback] subscription may simply be lagging (batching, weak signal, etc.).
     */
    private fun refreshOutput() {
        val permission = permissionState()
        val now = SystemClock.elapsedRealtimeNanos()
        val canUseReading = permission == LocationPermissionState.GRANTED && started
        val reading = if (canUseReading) {
            LocationReadingFilter.accepted(lastReading, now, activeConfig)
        } else {
            null
        }
        val readingValidity = if (canUseReading) {
            LocationReadingFilter.validity(lastReading, now, activeConfig)
        } else {
            SensorValidity.UNKNOWN
        }

        val isStale = canUseReading && lastReading != null &&
            now - lastReading!!.timestampNanos > activeConfig.staleTimeoutNanos
        if (isStale && !expired && !staleRecoveryPending) {
            beginStaleRecovery()
        }

        val availability = when {
            permission != LocationPermissionState.GRANTED -> LocationAvailabilityState.UNAVAILABLE
            !started -> LocationAvailabilityState.UNKNOWN
            expired -> LocationAvailabilityState.EXPIRED
            staleRecoveryPending -> LocationAvailabilityState.RECOVERING
            providerAvailable == true -> LocationAvailabilityState.AVAILABLE
            providerAvailable == false -> LocationAvailabilityState.UNAVAILABLE
            else -> LocationAvailabilityState.UNKNOWN
        }

        _output.value = if (reading == null) {
            LocationOutput(
                currentLocation = null,
                targetLocation = targetLocation,
                proximity = ProximityState.UNKNOWN,
                validity = readingValidity,
                permission = permission,
                availability = availability,
                accuracyMeters = lastReading?.accuracyMeters,
                timestampNanos = lastReading?.timestampNanos,
            )
        } else {
            LocationCalculator.buildOutput(
                currentLocation = reading.coordinate,
                targetLocation = targetLocation,
                timestampNanos = reading.timestampNanos,
                config = activeConfig,
                permission = permission,
                availability = availability,
                accuracyMeters = reading.accuracyMeters,
            )
        }
    }

    private fun beginStaleRecovery() {
        staleRecoveryPending = true
        staleRecoveryAttempts = 0
        attemptStaleRecovery()
    }

    @SuppressLint("MissingPermission")
    private fun attemptStaleRecovery() {
        if (permissionState() != LocationPermissionState.GRANTED) {
            staleRecoveryPending = false
            return
        }
        staleRecoveryAttempts++
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                if (location != null) onLocation(location) else onStaleRecoveryAttemptFailed()
            }
            .addOnFailureListener { onStaleRecoveryAttemptFailed() }
    }

    private fun onStaleRecoveryAttemptFailed() {
        if (staleRecoveryAttempts >= MAX_STALE_RECOVERY_ATTEMPTS) {
            staleRecoveryPending = false
            expired = true
            SensorErrorReporter.report(
                SensorComponent.GPS,
                "Location hasn't updated in a while and could not be refreshed.",
            )
            refreshOutput()
            return
        }
        handler.postDelayed(staleRecoveryRunnable, STALE_RECOVERY_RETRY_INTERVAL_MILLIS)
    }

    private fun resetStaleRecovery() {
        handler.removeCallbacks(staleRecoveryRunnable)
        staleRecoveryPending = false
        staleRecoveryAttempts = 0
        expired = false
    }

    private fun permissionState(): LocationPermissionState {
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        return if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            LocationPermissionState.GRANTED
        } else {
            LocationPermissionState.DENIED
        }
    }

    private companion object {
        const val LOCATION_UPDATE_INTERVAL_MILLIS = 5_000L
        const val LOCATION_FASTEST_INTERVAL_MILLIS = 2_000L
        const val LOCATION_MAX_DELAY_MILLIS = 5_000L
        const val STALE_REFRESH_INTERVAL_MILLIS = 1_000L
        const val MAX_STALE_RECOVERY_ATTEMPTS = 2
        const val STALE_RECOVERY_RETRY_INTERVAL_MILLIS = 10_000L
    }
}
