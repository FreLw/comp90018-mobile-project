package com.comp90018.app.features.map

import com.comp90018.app.R
import com.comp90018.app.contextengine.challenge.RelicChallengeConfig
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import com.comp90018.app.features.treasure.treasureArtworkResource
import com.comp90018.app.sensors.location.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostChallengeRevealSessionTest {
    private val coordinate = GeoCoordinate(-37.8, 144.96)

    @Test fun unionPhotoSurvivesDirectTreasureRevealAndStory() {
        val relic = relic("union_lawn_lost_lake", "The Lost Lake Photograph", "Union Lawn", "treasure_postcard",
            RelicChallengeType.UNION_LAWN_PHOTO).copy(
            historicalImageUrl = "https://example.org/history.jpg",
            story = "The lake was filled as campus grew.",
        )
        val session = PostChallengeRevealSession(relic, "content://local/captured-photo")
        assertEquals(PostChallengePage.TREASURE, session.page)
        assertEquals("content://local/captured-photo", session.capturedPhotoUri)
        assertTrue(session.hasHistoricalImage)
        session.showHistorical()
        session.continueToTreasure()
        session.viewStory()
        assertEquals(PostChallengePage.STORY, session.page)
        assertEquals(relic.story, session.relic.story)
        session.backToTreasure()
        assertEquals(PostChallengePage.TREASURE, session.page)
        assertTrue(session.historicalShown)
        assertEquals("content://local/captured-photo", session.capturedPhotoUri)
    }

    @Test fun missingHistoricalImageKeepsPhotoAndAllowsTreasureReveal() {
        val relic = relic("union_lawn_lost_lake", "The Lost Lake Photograph", "Union Lawn", "treasure_postcard",
            RelicChallengeType.UNION_LAWN_PHOTO)
        val session = PostChallengeRevealSession(relic, "content://local/captured-photo")
        assertFalse(session.hasHistoricalImage)
        assertEquals(PostChallengePage.TREASURE, session.page)
        session.continueToTreasure()
        assertEquals(PostChallengePage.TREASURE, session.page)
    }

    @Test fun successfulRevealHandoffOccursOnceAndNeverSavesAgain() {
        val relic = relic("union_lawn_lost_lake", "The Lost Lake Photograph", "Union Lawn", "treasure_postcard",
            RelicChallengeType.UNION_LAWN_PHOTO)
        val coordinator = PostChallengeRevealCoordinator()
        coordinator.openAfterSave(relic, "content://local/first-photo")
        val original = coordinator.session
        coordinator.openAfterSave(relic, "content://local/duplicate-photo")
        assertTrue(original === coordinator.session)
        assertEquals("content://local/first-photo", coordinator.session?.capturedPhotoUri)
        coordinator.session?.continueToTreasure()
        coordinator.session?.viewStory()
        assertEquals(PostChallengePage.STORY, coordinator.session?.page)
        // Reveal navigation has no persistence callback or repository dependency.
    }

    @Test fun otherTreasuresOpenTheirOwnContent() {
        val cases = listOf(
            Triple("wilson_hall_rosette", "The Surviving Stone Rosette", "treasure_rosette"),
            Triple("old_quad_fossil", "Miniature Fossil Specimen", "treasure_fern"),
            Triple("south_lawn_atlas", "Reconstructed Miniature Atlas Figure", "treasure_atlas"),
        )
        cases.forEach { (id, name, artwork) ->
            val relic = relic(id, name, "Campus", artwork, RelicChallengeType.WILSON_HALL_OBSERVATION)
            val session = PostChallengeRevealSession(relic, null)
            assertEquals(PostChallengePage.TREASURE, session.page)
            assertEquals(name, session.relic.name)
            assertEquals(artwork, session.relic.artworkKey)
        }
    }

    @Test fun firestoreArtworkKeysResolveToTheRequestedTreasureArt() {
        assertEquals(R.drawable.treasure_rosette, treasureArtworkResource("treasure_rosette"))
        assertEquals(R.drawable.treasure_fern, treasureArtworkResource("treasure_fern"))
        assertEquals(R.drawable.treasure_atlas, treasureArtworkResource("treasure_atlas"))
        assertEquals(R.drawable.treasure_unknown, treasureArtworkResource(""))
    }

    private fun relic(id: String, name: String, location: String, artwork: String, type: RelicChallengeType) =
        MapRelic(
            id = id, name = name, locationName = location, coordinate = coordinate,
            description = "Firestore description", artworkKey = artwork,
            challengeConfig = RelicChallengeConfig(
                challengeId = "challenge-$id", type = type, targetLocation = coordinate,
                insideRadiusMeters = 20.0, requiredHeadingDegrees = null, headingToleranceDegrees = 12.0,
                requiresStationary = false, requiresStability = false, requiresRotationStill = false,
                requiresHorizontal = false, holdDurationNanos = 0L, photoActionRequired = type == RelicChallengeType.UNION_LAWN_PHOTO,
            ),
        )
}
