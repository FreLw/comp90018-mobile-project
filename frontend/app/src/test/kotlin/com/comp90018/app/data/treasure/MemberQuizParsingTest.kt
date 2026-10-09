package com.comp90018.app.data.treasure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemberQuizParsingTest {
    private fun question(answer: Any = 1L) = mapOf(
        "prompt" to " Which year? ",
        "options" to listOf("1856", "1882"),
        "correctAnswerIndex" to answer,
    )

    @Test fun readsFirestoreIntegersAndPreservesQuestionOrder() {
        val parsed = parseMemberQuizQuestions(mapOf("questions" to listOf(question(), question(0L))))
        assertEquals(2, parsed.size)
        assertEquals("Which year?", parsed[0].prompt)
        assertEquals(listOf("1856", "1882"), parsed[0].options)
        assertEquals(listOf(1, 0), parsed.map { it.correctAnswerIndex })
    }

    @Test fun missingOrMalformedQuizzesCannotBecomeCompletableTasks() {
        assertTrue(parseMemberQuizQuestions(null).isEmpty())
        assertTrue(parseMemberQuizQuestions(mapOf("questions" to emptyList<Any>())).isEmpty())
        val malformed = listOf(
            question(-1L), question(2L), question(0.5), question(Double.NaN), question("1"),
            question() + ("prompt" to " "),
            question() + ("options" to listOf("1856")),
            question() + ("options" to listOf("1856", " ")),
            question() + ("options" to listOf("1856", 1882)),
        )
        malformed.forEach { invalid ->
            assertTrue(parseMemberQuizQuestions(mapOf("questions" to listOf(question(), invalid))).isEmpty())
        }
    }
}
