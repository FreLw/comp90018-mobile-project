package com.comp90018.app.ui.components

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatTimestampTest {
    @Test
    fun sameYearUsesAustralianDateOrderWithoutYear() {
        val sentAt = Instant.parse("2026-10-02T13:13:00Z").toEpochMilli()
        val now = Instant.parse("2026-12-20T00:00:00Z").toEpochMilli()

        assertEquals("2 Oct, 1:13 pm", formatMessageTimestamp(sentAt, now, ZoneOffset.UTC))
    }

    @Test
    fun earlierYearIncludesYear() {
        val sentAt = Instant.parse("2025-10-02T13:13:00Z").toEpochMilli()
        val now = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli()

        assertEquals("2 Oct 2025, 1:13 pm", formatMessageTimestamp(sentAt, now, ZoneOffset.UTC))
    }

    @Test
    fun unresolvedFirestoreTimestampIsHidden() {
        assertNull(formatMessageTimestamp(0L))
    }
}
