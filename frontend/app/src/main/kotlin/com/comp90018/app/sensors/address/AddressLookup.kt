package com.comp90018.app.sensors.address

import com.comp90018.app.sensors.location.GeoCoordinate

interface AddressLookup {
    suspend fun reverseGeocode(coordinate: GeoCoordinate): AddressResult
}
