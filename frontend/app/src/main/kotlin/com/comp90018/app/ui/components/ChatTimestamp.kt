package com.comp90018.app.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val messageTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())

/** Formats a Firestore message timestamp in the device's local time zone. */
fun formatMessageTimestamp(sentAtMillis: Long): String? =
    sentAtMillis.takeIf { it > 0 }
        ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(messageTimestampFormatter) }
