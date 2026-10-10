package com.comp90018.app.ui.images

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads and closes the avatar stream off the UI thread; unavailable sources keep the letter fallback. */
internal suspend fun loadAvatarImage(context: Context, source: String): ImageBitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            val stream = when {
                source.startsWith("content:") -> context.contentResolver.openInputStream(Uri.parse(source))
                source.startsWith("https://") -> URL(source).openStream()
                else -> null
            }
            stream?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        }.getOrNull()
    }
