package com.comp90018.app.features.map

import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.comp90018.app.R
import com.comp90018.app.sensors.location.GeoCoordinate

data class MapRelic(
    val id: String,
    val name: String,
    val locationName: String,
    val description: String,
    val clue: String,
    val difficulty: String,
    val taskHint: String,
    val huntType: String,
    val requiredPlayers: Int,
    val story: String,
    @param:DrawableRes val imageRes: Int,
    val coordinate: GeoCoordinate,
    val insideRadiusMeters: Double = 20.0,
    val nearbyRadiusMeters: Double = 120.0,
)

val sampleMapRelics = listOf(
    MapRelic(
        id = "south-lawn-postcard",
        name = "South Lawn Postcard",
        locationName = "South Lawn",
        description = "A weathered campus postcard preserving a quiet view from another generation.",
        clue = "Follow the place where the old lawn mirrors the sky after rain.",
        difficulty = "Easy",
        taskHint = "Hold the relic scanner steady beside the lawn's stone edge.",
        huntType = "Solo",
        requiredPlayers = 1,
        story = "Long before campus photos lived on phones, students carried small printed memories home. This postcard preserves an imagined afternoon beside the lawn and the stories shared there.",
        imageRes = R.drawable.treasure_postcard,
        coordinate = GeoCoordinate(-37.79856, 144.96050),
    ),
    MapRelic(
        id = "old-quad-rosette",
        name = "Old Quad Rosette",
        locationName = "Old Quadrangle",
        description = "A carved stone flower inspired by the repeating patterns hidden around the Old Quad.",
        clue = "Seek the stone flower that never needs sunlight.",
        difficulty = "Medium",
        taskHint = "Two explorers must stabilise their scanners at the same landmark.",
        huntType = "Collaborative",
        requiredPlayers = 2,
        story = "Stonemasons often left rhythm and symbolism in small architectural details. This rosette celebrates the careful craft that rewards explorers who remember to look up.",
        imageRes = R.drawable.treasure_rosette,
        coordinate = GeoCoordinate(-37.798156, 144.960481),
    ),
    MapRelic(
        id = "system-garden-fern",
        name = "Fern Stone",
        locationName = "System Garden",
        description = "A fern impression captured in stone, celebrating the living collection of the System Garden.",
        clue = "Look where living leaves meet a path made for patient observers.",
        difficulty = "Easy",
        taskHint = "Walk slowly until the motion signal settles into the green zone.",
        huntType = "Solo",
        requiredPlayers = 1,
        story = "The System Garden arranges plants as a living map of botanical relationships. This fern stone turns that living lesson into a small relic that can travel through time.",
        imageRes = R.drawable.treasure_fern,
        coordinate = GeoCoordinate(-37.79695, 144.96120),
    ),
    MapRelic(
        id = "atlas-statue",
        name = "Atlas of Knowledge",
        locationName = "Baillieu Library",
        description = "A tiny Atlas carrying a globe of ideas for every explorer who keeps asking questions.",
        clue = "Find the quiet giant carrying more stories than stone.",
        difficulty = "Hard",
        taskHint = "Align both explorers' compass rings before the signal fades.",
        huntType = "Collaborative",
        requiredPlayers = 2,
        story = "Atlas is usually shown carrying the sky. Here, he carries a globe of knowledge instead—a playful reminder that learning can feel heavy, but nobody has to carry it alone.",
        imageRes = R.drawable.treasure_atlas,
        coordinate = GeoCoordinate(-37.79860, 144.96020),
    ),
    MapRelic(
        id = "glasshouse",
        name = "Glasshouse Keeper",
        locationName = "System Garden Glasshouse",
        description = "A miniature glasshouse sheltering the rare plants and patient observations of campus botanists.",
        clue = "Warm glass and green shadows guard the next fragment.",
        difficulty = "Medium",
        taskHint = "Level both phones and keep them still for three signal pulses.",
        huntType = "Collaborative",
        requiredPlayers = 2,
        story = "Glasshouses create tiny climates where plants from distant places can thrive. This miniature remembers the gardeners and researchers who kept those delicate worlds alive.",
        imageRes = R.drawable.treasure_glasshouse,
        coordinate = GeoCoordinate(-37.79725, 144.96145),
    ),
    MapRelic(
        id = "campus-press",
        name = "Campus Story Press",
        locationName = "Arts West",
        description = "An old mechanical press that turns campus discoveries into stories worth carrying home.",
        clue = "Listen for an imaginary clatter where campus stories take shape.",
        difficulty = "Hard",
        taskHint = "Rotate slowly until every compass spark joins the centre mark.",
        huntType = "Solo",
        requiredPlayers = 1,
        story = "Before instant publishing, every printed page depended on careful mechanical work. This little press honours the makers who transformed observations into records for the next explorer.",
        imageRes = R.drawable.treasure_press,
        coordinate = GeoCoordinate(-37.79775, 144.95885),
    ),
)

/** Frontend-only collection state shared by the map hunt and Treasure tab. */
object LocalTreasureCollection {
    var foundIds by mutableStateOf<Set<String>>(emptySet())
        private set

    fun add(relicId: String) {
        foundIds = foundIds + relicId
    }
}
