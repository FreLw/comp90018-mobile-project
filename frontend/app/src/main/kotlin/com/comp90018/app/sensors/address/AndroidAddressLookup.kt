package com.comp90018.app.sensors.address

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.location.GeoCoordinate
import java.io.IOException
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class AndroidAddressLookup(context: Context) : AddressLookup {
    private val geocoder = Geocoder(context.applicationContext)

    override suspend fun reverseGeocode(coordinate: GeoCoordinate): AddressResult {
        if (!Geocoder.isPresent()) return AddressResult(validity = SensorValidity.UNRELIABLE)
        return try {
            val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocodeAsync(coordinate)
            } else {
                geocodeBlocking(coordinate)
            }
            address?.toAddressResult() ?: AddressResult(validity = SensorValidity.UNKNOWN)
        } catch (e: IOException) {
            AddressResult(validity = SensorValidity.UNRELIABLE)
        }
    }

    private suspend fun geocodeAsync(coordinate: GeoCoordinate): Address? =
        suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocation(coordinate.latitude, coordinate.longitude, 1) { addresses ->
                continuation.resume(addresses.firstOrNull())
            }
        }

    @Suppress("DEPRECATION")
    private suspend fun geocodeBlocking(coordinate: GeoCoordinate): Address? =
        withContext(Dispatchers.IO) {
            geocoder.getFromLocation(coordinate.latitude, coordinate.longitude, 1)?.firstOrNull()
        }

    private fun Address.toAddressResult() = AddressResult(
        formattedAddress = (0..maxAddressLineIndex).joinToString(", ") { getAddressLine(it) }.ifBlank { null },
        thoroughfare = thoroughfare,
        locality = locality,
        adminArea = adminArea,
        countryName = countryName,
        postalCode = postalCode,
        validity = SensorValidity.VALID,
    )
}
