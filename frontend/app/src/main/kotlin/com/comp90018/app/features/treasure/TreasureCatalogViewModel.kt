package com.comp90018.app.features.treasure

/*
 * Provides the shared treasure catalogue, loading state, and retryable loading errors.
 * The shell shares this catalogue with map, collection, room, and navigation interfaces.
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.comp90018.app.data.social.Subscription
import com.comp90018.app.data.treasure.TreasureRepository
import com.comp90018.app.features.map.MapRelic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Catalogue treasures and loading/error presentation shared by the signed-in screens. */
data class TreasureCatalogUiState(
    val treasures: List<MapRelic> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class TreasureCatalogViewModel(
    private val repository: TreasureRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TreasureCatalogUiState())
    val uiState: StateFlow<TreasureCatalogUiState> = mutableUiState.asStateFlow()
    private var subscription: Subscription? = null

    init {
        observeTreasures()
    }

    fun retry() {
        mutableUiState.value = mutableUiState.value.copy(loading = true, error = null)
        observeTreasures()
    }

    private fun observeTreasures() {
        subscription?.cancel()
        subscription = repository.observeEnabledTreasures { treasures, error ->
            mutableUiState.value = TreasureCatalogUiState(
                treasures = treasures,
                loading = false,
                error = error,
            )
        }
    }

    override fun onCleared() {
        subscription?.cancel()
    }

    companion object {
        fun factory(repository: TreasureRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TreasureCatalogViewModel(repository) as T
        }
    }
}
