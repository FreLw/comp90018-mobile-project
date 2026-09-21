package com.comp90018.app.sensors.address

import com.comp90018.app.sensors.SensorValidity

data class AddressResult(
    val formattedAddress: String? = null,
    val thoroughfare: String? = null,
    val locality: String? = null,
    val adminArea: String? = null,
    val countryName: String? = null,
    val postalCode: String? = null,
    val validity: SensorValidity = SensorValidity.UNKNOWN,
)
