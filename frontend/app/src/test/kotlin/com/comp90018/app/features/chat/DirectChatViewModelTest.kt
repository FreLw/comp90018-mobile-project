package com.comp90018.app.features.chat

import android.net.Uri
import com.comp90018.app.data.chat.ChatMessage
import com.comp90018.app.data.chat.ChatRepository
import com.comp90018.app.data.social.Subscription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DirectChatViewModelTest {
    @Test
    fun failedSendKeepsDraftVisible() {
        val repository = FakeChatRepository()
        val viewModel = createViewModel(repository)
        viewModel.updateInput("Meet me at the Old Quad")

        viewModel.send()
        repository.completeSend("Permission denied")

        assertEquals("Meet me at the Old Quad", viewModel.uiState.value.input)
        assertEquals("Permission denied", viewModel.uiState.value.error)
    }

    @Test
    fun successfulSendClearsOnlyTheSubmittedDraft() {
        val repository = FakeChatRepository()
        val viewModel = createViewModel(repository)
        viewModel.updateInput("First message")

        viewModel.send()
        viewModel.updateInput("A new draft")
        repository.completeSend(null)

        assertEquals("A new draft", viewModel.uiState.value.input)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun successfulSendClearsUnchangedDraft() {
        val repository = FakeChatRepository()
        val viewModel = createViewModel(repository)
        viewModel.updateInput("Sent message")

        viewModel.send()
        repository.completeSend(null)

        assertEquals("", viewModel.uiState.value.input)
    }

    private fun createViewModel(repository: ChatRepository) = DirectChatViewModel(
        repository = repository,
        roomId = "direct-current-friend",
        currentUid = "current",
        friendUid = "friend",
        currentUsername = "explorer",
        currentAvatarUrl = "",
    )
}

private class FakeChatRepository : ChatRepository {
    private var sendCompletion: ((String?) -> Unit)? = null

    override fun observeMessages(
        roomId: String,
        onChange: (List<ChatMessage>, String?) -> Unit,
    ): Subscription {
        onChange(emptyList(), null)
        return Subscription { }
    }

    override fun markMessagesRead(currentUid: String, friendUid: String) = Unit

    override fun sendMessage(
        roomId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String,
        text: String,
        onComplete: (String?) -> Unit,
    ) {
        sendCompletion = onComplete
    }

    fun completeSend(error: String?) {
        requireNotNull(sendCompletion)(error)
        sendCompletion = null
    }

    override fun sendImage(
        roomId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String,
        imageUri: Uri,
        onComplete: (String?) -> Unit,
    ) = onComplete(null)

    override fun sendTreasureSticker(
        roomId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String,
        treasureId: String,
        onComplete: (String?) -> Unit,
    ) = onComplete(null)
}
