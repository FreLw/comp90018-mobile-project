package com.comp90018.app.features.treasure

import com.comp90018.app.data.social.Subscription
import com.comp90018.app.data.treasure.TreasureCollectionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TreasureCollectionViewModelTest {
    @Test
    fun observedDiscoveriesReplaceLocalCollectionState() {
        val repository = FakeTreasureCollectionRepository(setOf("old_quad_fossil"))
        val viewModel = TreasureCollectionViewModel(repository, "user-1")

        assertEquals(setOf("old_quad_fossil"), viewModel.uiState.value.discoveredIds)
        assertFalse(viewModel.uiState.value.loading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun collectingTreasurePersistsAndUpdatesObservedState() {
        val repository = FakeTreasureCollectionRepository()
        val viewModel = TreasureCollectionViewModel(repository, "user-1")
        var completionError: String? = "not completed"

        viewModel.addDiscoveredTreasure("wilson_hall_rosette") { completionError = it }

        assertNull(completionError)
        assertEquals(setOf("wilson_hall_rosette"), viewModel.uiState.value.discoveredIds)
        assertNull(viewModel.uiState.value.savingTreasureId)
    }
}

private class FakeTreasureCollectionRepository(
    initialIds: Set<String> = emptySet(),
) : TreasureCollectionRepository {
    private var ids = initialIds
    private var observer: ((Set<String>, String?) -> Unit)? = null

    override fun observeDiscoveredTreasureIds(
        userId: String,
        onChange: (Set<String>, String?) -> Unit,
    ): Subscription {
        observer = onChange
        onChange(ids, null)
        return Subscription { observer = null }
    }

    override fun addDiscoveredTreasure(
        userId: String,
        treasureId: String,
        onComplete: (String?) -> Unit,
    ) {
        ids = ids + treasureId
        observer?.invoke(ids, null)
        onComplete(null)
    }
}
