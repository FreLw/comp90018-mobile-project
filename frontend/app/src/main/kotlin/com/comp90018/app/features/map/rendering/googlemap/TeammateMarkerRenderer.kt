package com.comp90018.app.features.map.rendering.googlemap

import com.comp90018.app.features.map.rendering.CURRENT_LOCATION_Z_INDEX
import com.comp90018.app.features.map.rendering.currentLocationIcon
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

/** Retains marker instances between updates; scene clearing forgets their old handles. */
internal class TeammateMarkerRenderer {
    private val markers = mutableMapOf<String, Marker>()

    fun forgetClearedMarkers() {
        markers.clear()
    }

    fun render(context: android.content.Context, map: GoogleMap, team: MapTeamInputs) {
        val teammateLocations = team.locations
        val huntMemberNames = team.memberNames
        val huntOwnerId = team.ownerId
        val visibleTeammateIds = teammateLocations.map { it.uid }.toSet()
        markers.keys.filterNot { it in visibleTeammateIds }.forEach { uid ->
            markers.remove(uid)?.remove()
        }
        teammateLocations.forEach { teammate ->
            val position = LatLng(teammate.coordinate.latitude, teammate.coordinate.longitude)
            val isHost = teammate.uid == huntOwnerId
            val name = huntMemberNames[teammate.uid]?.takeIf { it.isNotBlank() } ?: "Teammate"
            val title = "$name (${if (isHost) "Host" else "Teammate"})"
            val marker = markers[teammate.uid] ?: map.addMarker(
                MarkerOptions().position(position).title(title).snippet("Online · Shared hunt location")
                    .icon(if (isHost) {
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                    } else {
                        currentLocationIcon(
                            context,
                            dotColor = android.graphics.Color.rgb(52, 152, 219),
                            showHeading = false,
                        )
                    })
                    .anchor(0.5f, if (isHost) 1f else 0.72f)
                    .zIndex(CURRENT_LOCATION_Z_INDEX + 1f),
            )?.also { markers[teammate.uid] = it; it.tag = "teammate:${teammate.uid}" }
            marker?.position = position
            marker?.title = title
        }
    }
}
