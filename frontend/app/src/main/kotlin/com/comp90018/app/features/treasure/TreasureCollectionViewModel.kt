package com.comp90018.app.features.treasure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.comp90018.app.data.social.Subscription
import com.comp90018.app.data.treasure.TreasureCollectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TreasureCollectionUiState(
    val discoveredIds: Set<String> = emptySet(),
    val loading: Boolean = true,
    val error: String? = null,
    val savingTreasureId: String? = null,
)

class TreasureCollectionViewModel(
    private val repository: TreasureCollectionRepository,
    private val userId: String,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TreasureCollectionUiState())
    val uiState: StateFlow<TreasureCollectionUiState> = mutableUiState.asStateFlow()
    private var subscription: Subscription? = null

    init {
        observeCollection()
    }

    fun retry() {
        mutableUiState.value = mutableUiState.value.copy(loading = true, error = null)
        observeCollection()
    }

    fun addDiscoveredTreasure(treasureId: String, onComplete: (String?) -> Unit = {}) {
        if (treasureId in mutableUiState.value.discoveredIds) {
            onComplete(null)
            return
        }
        if (mutableUiState.value.savingTreasureId != null) return

        mutableUiState.value = mutableUiState.value.copy(savingTreasureId = treasureId, error = null)
        repository.addDiscoveredTreasure(userId, treasureId) { error ->
            mutableUiState.value = mutableUiState.value.copy(
                savingTreasureId = null,
                error = error,
            )
            onComplete(error)
        }
    }

    private fun observeCollection() {
        subscription?.cancel()
        subscription = repository.observeDiscoveredTreasureIds(userId) { discoveredIds, error ->
            mutableUiState.value = mutableUiState.value.copy(
                discoveredIds = discoveredIds,
                loading = false,
                error = error,
            )
        }
    }

    override fun onCleared() {
        subscription?.cancel()
    }

    companion object {
        fun factory(repository: TreasureCollectionRepository, userId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TreasureCollectionViewModel(repository, userId) as T
        }
    }
}
