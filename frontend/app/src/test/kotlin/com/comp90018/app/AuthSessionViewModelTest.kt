package com.comp90018.app

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.comp90018.app.data.social.Subscription
import com.comp90018.app.data.treasure.TreasureRepository
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.treasure.TreasureCatalogViewModel
import com.comp90018.app.sensors.location.GeoCoordinate
import org.junit.Assert.*
import org.junit.Test

class AuthSessionViewModelTest {
    @Test
    fun switchingAccountsAutomaticallyLoadsCatalogWithANewListener() {
        val session = AuthSessionViewModel()
        val repository = SessionTreasureRepository()
        val oldCatalog = catalog(session, "old-user", repository)
        repository.failListener()
        assertNotNull(oldCatalog.uiState.value.error)

        session.updateUser("new-user")
        val newCatalog = catalog(session, "new-user", repository)

        assertNotSame(oldCatalog, newCatalog)
        assertEquals(1, repository.cancellations)
        assertEquals(2, repository.subscriptions)
        assertEquals("catalog-2", newCatalog.uiState.value.treasures.single().id)
        assertFalse(newCatalog.uiState.value.loading)
        assertNull(newCatalog.uiState.value.error)
    }

    @Test
    fun signingBackIntoTheSameAccountAutomaticallyRestartsListeners() {
        val session = AuthSessionViewModel()
        val repository = SessionTreasureRepository()
        val beforeLogout = catalog(session, "user", repository)

        session.updateUser(null)
        assertEquals(1, repository.cancellations)
        val afterLogin = catalog(session, "user", repository)

        assertNotSame(beforeLogout, afterLogin)
        assertEquals(2, repository.subscriptions)
        assertNull(afterLogin.uiState.value.error)
        assertFalse(afterLogin.uiState.value.loading)
    }

    @Test
    fun recompositionAndRotationKeepTheCurrentCatalogSubscription() {
        val session = AuthSessionViewModel()
        val repository = SessionTreasureRepository()
        val before = catalog(session, "user", repository)

        session.updateUser("user")
        val after = catalog(session, "user", repository)

        assertSame(before, after)
        assertEquals(1, repository.subscriptions)
        assertEquals(0, repository.cancellations)
    }

    @Test
    fun clearingActivityStateAlsoDetachesTheSignedInListeners() {
        val activityOwner = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
        val session = ViewModelProvider(
            activityOwner,
            ViewModelProvider.NewInstanceFactory(),
        )[AuthSessionViewModel::class.java]
        val repository = SessionTreasureRepository()
        catalog(session, "user", repository)

        activityOwner.viewModelStore.clear()

        assertEquals(1, repository.cancellations)
    }

    private fun catalog(
        session: AuthSessionViewModel,
        uid: String,
        repository: TreasureRepository,
    ): TreasureCatalogViewModel = ViewModelProvider(
        session.ownerFor(uid),
        TreasureCatalogViewModel.factory(repository),
    )[TreasureCatalogViewModel::class.java]
}

private class SessionTreasureRepository : TreasureRepository {
    var subscriptions = 0
    var cancellations = 0
    private var listener: ((List<MapRelic>, String?) -> Unit)? = null

    override fun observeEnabledTreasures(onChange: (List<MapRelic>, String?) -> Unit): Subscription {
        subscriptions++
        listener = onChange
        onChange(
            listOf(MapRelic("catalog-$subscriptions", "Relic", "Campus", coordinate = GeoCoordinate(-37.8, 145.0))),
            null,
        )
        return Subscription {
            cancellations++
            listener = null
        }
    }

    fun failListener() {
        listener?.invoke(emptyList(), "Permission denied")
    }
}
