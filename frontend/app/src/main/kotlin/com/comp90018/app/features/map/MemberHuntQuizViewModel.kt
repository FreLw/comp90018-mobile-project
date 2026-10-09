package com.comp90018.app.features.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MemberHuntQuizUiState(
    val questions: List<MemberHuntQuestion> = emptyList(),
    val questionIndex: Int = 0,
    val closeEnough: Boolean = false,
    val incorrect: Boolean = false,
    val submitting: Boolean = false,
    val completionError: String? = null,
    val completed: Boolean = false,
) {
    val currentQuestion: MemberHuntQuestion? get() = questions.getOrNull(questionIndex)
    val canAnswer: Boolean get() = closeEnough && currentQuestion != null && !submitting && !completed
}

/** Owns quiz progress; the injected action uses the existing room persistence flow. */
class MemberHuntQuizViewModel(
    private val completeTask: ((String?) -> Unit) -> Unit,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MemberHuntQuizUiState())
    val uiState: StateFlow<MemberHuntQuizUiState> = mutableUiState.asStateFlow()
    private var revision = 0

    fun updateQuestions(questions: List<MemberHuntQuestion>) {
        if (questions == mutableUiState.value.questions) return
        revision += 1
        mutableUiState.value = MemberHuntQuizUiState(
            questions = questions.toList(),
            closeEnough = mutableUiState.value.closeEnough,
        )
    }

    fun updateLocation(distanceMeters: Double?, radiusMeters: Double) {
        val closeEnough = distanceMeters != null && distanceMeters.isFinite() && distanceMeters >= 0 &&
            radiusMeters.isFinite() && radiusMeters > 0 && distanceMeters <= radiusMeters
        mutableUiState.value = mutableUiState.value.copy(closeEnough = closeEnough)
    }

    fun answer(optionIndex: Int) {
        val state = mutableUiState.value
        val question = state.currentQuestion ?: return
        if (!state.canAnswer || optionIndex !in question.options.indices) return
        if (optionIndex != question.correctAnswerIndex) {
            mutableUiState.value = state.copy(incorrect = true, completionError = null)
            return
        }
        if (state.questionIndex < state.questions.lastIndex) {
            mutableUiState.value = state.copy(questionIndex = state.questionIndex + 1, incorrect = false)
            return
        }
        mutableUiState.value = state.copy(submitting = true, incorrect = false, completionError = null)
        val submittedRevision = revision
        try {
            completeTask { error -> finishSubmission(submittedRevision, error) }
        } catch (error: Exception) {
            finishSubmission(submittedRevision, error.localizedMessage ?: "Unable to save task progress")
        }
    }

    private fun finishSubmission(submittedRevision: Int, error: String?) {
        // A cloud edit or cleared session must not receive an old request's result.
        if (submittedRevision != revision || !mutableUiState.value.submitting) return
        mutableUiState.value = mutableUiState.value.copy(
            submitting = false, completionError = error, completed = error == null,
        )
    }

    override fun onCleared() { revision += 1 }

    companion object {
        fun factory(completeTask: ((String?) -> Unit) -> Unit) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MemberHuntQuizViewModel(completeTask) as T
        }
    }
}
