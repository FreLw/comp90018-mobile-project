package com.comp90018.app.features.map

import org.junit.Assert.*
import org.junit.Test

class MemberHuntQuizViewModelTest {
    private val questions = listOf(
        MemberHuntQuestion("First?", listOf("A", "B"), 1),
        MemberHuntQuestion("Second?", listOf("A", "B"), 0),
    )

    @Test fun requiresArrivalAndCorrectAnswersBeforeSaving() {
        var saves = 0
        val vm = MemberHuntQuizViewModel { saves++; it(null) }
        vm.updateLocation(5.0, 10.0)
        vm.answer(0)
        assertEquals(0, saves)
        vm.updateQuestions(questions)
        vm.updateLocation(11.0, 10.0)
        vm.answer(1)
        assertEquals(0, vm.uiState.value.questionIndex)
        vm.updateLocation(10.0, 10.0)
        vm.answer(-1)
        assertFalse(vm.uiState.value.incorrect)
        vm.answer(0)
        assertTrue(vm.uiState.value.incorrect)
        vm.answer(1)
        assertFalse(vm.uiState.value.incorrect)
        assertEquals(1, vm.uiState.value.questionIndex)
        assertEquals(0, saves)
        vm.answer(0)
        assertTrue(vm.uiState.value.completed)
        assertEquals(1, saves)
        vm.answer(0)
        assertEquals(1, saves)
    }

    @Test fun pendingSubmissionBlocksDuplicateTapsAndFailureAllowsRetry() {
        var callback: ((String?) -> Unit)? = null
        var saves = 0
        val vm = MemberHuntQuizViewModel { saves++; callback = it }
        vm.updateQuestions(questions)
        vm.updateLocation(5.0, 10.0)
        vm.answer(1)
        vm.answer(0)
        assertTrue(vm.uiState.value.submitting)
        vm.answer(0)
        assertEquals(1, saves)
        callback!!("Network error")
        assertFalse(vm.uiState.value.submitting)
        assertFalse(vm.uiState.value.completed)
        assertEquals("Network error", vm.uiState.value.completionError)
        vm.answer(0)
        assertEquals(2, saves)
        assertNull(vm.uiState.value.completionError)
        callback!!(null)
        assertTrue(vm.uiState.value.completed)
    }

    @Test fun liveQuestionChangesResetProgressAndIgnoreOldSubmissionResults() {
        var callback: ((String?) -> Unit)? = null
        val vm = MemberHuntQuizViewModel { callback = it }
        vm.updateQuestions(questions)
        vm.updateLocation(5.0, 10.0)
        vm.answer(1)
        vm.updateQuestions(questions.toList())
        assertEquals(1, vm.uiState.value.questionIndex)
        vm.answer(0)
        vm.updateQuestions(questions.reversed())
        assertEquals(0, vm.uiState.value.questionIndex)
        assertFalse(vm.uiState.value.submitting)
        callback!!(null)
        assertFalse(vm.uiState.value.completed)
        vm.updateQuestions(emptyList())
        assertFalse(vm.uiState.value.canAnswer)
    }

    @Test fun lostOrInvalidLocationBlocksAnswersAndSynchronousErrorsAllowRetry() {
        val vm = MemberHuntQuizViewModel { throw IllegalStateException("Unavailable") }
        vm.updateQuestions(questions.takeLast(1))
        listOf(null, Double.NaN, -1.0, Double.POSITIVE_INFINITY).forEach { distance ->
            vm.updateLocation(distance, 10.0)
            assertFalse(vm.uiState.value.canAnswer)
        }
        vm.updateLocation(5.0, 10.0)
        vm.answer(0)
        assertEquals("Unavailable", vm.uiState.value.completionError)
        assertFalse(vm.uiState.value.submitting)
        assertTrue(vm.uiState.value.canAnswer)
    }
}
