package com.comp90018.app.data.chat

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

/** Shared Firebase Storage uploader used by both direct and team-room chats. */
object FirebaseChatImageStorage {
    fun upload(
        chatType: String,
        roomId: String,
        senderId: String,
        imageUri: Uri,
        onComplete: (String?, String?) -> Unit,
    ) {
        val reference = FirebaseStorage.getInstance().reference
            .child("chatImages/$chatType/$roomId/$senderId/${UUID.randomUUID()}.jpg")
        reference.putFile(imageUri)
            .continueWithTask { upload ->
                if (!upload.isSuccessful) {
                    throw (upload.exception ?: IllegalStateException("Photo upload failed"))
                }
                reference.downloadUrl
            }
            .addOnCompleteListener { task ->
                onComplete(
                    if (task.isSuccessful) task.result.toString() else null,
                    task.exception?.localizedMessage ?: "Unable to upload photo",
                )
            }
    }
}
