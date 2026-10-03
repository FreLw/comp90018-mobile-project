package com.comp90018.app.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val australianLocale = Locale.forLanguageTag("en-AU")
private val sameYearMessageTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM, h:mm a", australianLocale)
private val earlierYearMessageTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", australianLocale)

/** Formats a chat timestamp using familiar Australian date order and lower-case am/pm. */
fun formatMessageTimestamp(
    sentAtMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String? = sentAtMillis.takeIf { it > 0 }?.let {
    val sentAt = Instant.ofEpochMilli(it).atZone(zoneId)
    val currentYear = Instant.ofEpochMilli(nowMillis).atZone(zoneId).year
    val formatter = if (sentAt.year == currentYear) {
        sameYearMessageTimestampFormatter
    } else {
        earlierYearMessageTimestampFormatter
    }
    sentAt.format(formatter).replace(" AM", " am").replace(" PM", " pm")
}
