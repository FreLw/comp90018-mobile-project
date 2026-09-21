package com.comp90018.app.sensors.address

import com.comp90018.app.sensors.location.GeoCoordinate

class FakeAddressLookup(
    private val result: AddressResult = AddressResult(),
) : AddressLookup {
    override suspend fun reverseGeocode(coordinate: GeoCoordinate): AddressResult = result
}
