package com.unimelb.losttreasures.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unimelb.losttreasures.ui.components.RelicBadge
import com.unimelb.losttreasures.ui.data.SampleTeamCode
import com.unimelb.losttreasures.ui.data.discoveredRelics
import com.unimelb.losttreasures.ui.data.initialChatMessages
import com.unimelb.losttreasures.ui.model.ChatMessage
import com.unimelb.losttreasures.ui.model.Relic
import com.unimelb.losttreasures.ui.theme.Ink
import com.unimelb.losttreasures.ui.theme.RelicBlue
import com.unimelb.losttreasures.ui.theme.RelicGold
import com.unimelb.losttreasures.ui.theme.RelicGreen
import com.unimelb.losttreasures.ui.theme.RelicRed
import com.unimelb.losttreasures.ui.theme.toColor

@Composable
fun TeamScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages = remember { mutableStateListOf<ChatMessage>().also { it.addAll(initialChatMessages) } }
    var draft by rememberSaveable { mutableStateOf("") }
    var showAttachmentChooser by rememberSaveable { mutableStateOf(false) }
    var showPackPanel by rememberSaveable { mutableStateOf(false) }
    var progressExpanded by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val messageTopPadding = if (progressExpanded) 232.dp else 92.dp

    fun sendTextMessage() {
        val message = draft.trim()
        if (message.isNotEmpty()) {
            messages.add(ChatMessage("You", message, true))
            draft = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
    ) {
        ChatHeader(onBack = onBack)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFF4F7F5))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = messageTopPadding,
                    bottom = 18.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message = message)
                }
            }

            TaskProgressCard(
                expanded = progressExpanded,
                onToggle = { progressExpanded = !progressExpanded },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        if (showAttachmentChooser) {
            AttachmentChooser(
                onPickGallery = {
                    messages.add(ChatMessage("You", "Selected a photo from Gallery.", true))
                    showAttachmentChooser = false
                },
                onOpenCamera = {
                    messages.add(ChatMessage("You", "Opened Camera for a clue photo.", true))
                    showAttachmentChooser = false
                }
            )
        }

        if (showPackPanel) {
            PackPanel(
                onEmojiSelected = { emoji ->
                    messages.add(ChatMessage("You", emoji, true))
                    showPackPanel = false
                },
                onItemSelected = {
                    messages.add(ChatMessage("You", "Sent a collected fragment.", true))
                    showPackPanel = false
                }
            )
        }

        ChatInputBar(
            draft = draft,
            packSelected = showPackPanel,
            addSelected = showAttachmentChooser,
            onDraftChange = { draft = it },
            onMessageFocused = {
                showAttachmentChooser = false
                showPackPanel = false
            },
            onVoiceClick = {
                focusManager.clearFocus()
                showAttachmentChooser = false
                showPackPanel = false
                messages.add(ChatMessage("You", "Voice message placeholder.", true))
            },
            onPackClick = {
                focusManager.clearFocus()
                showAttachmentChooser = false
                showPackPanel = !showPackPanel
            },
            onAddClick = {
                focusManager.clearFocus()
                showPackPanel = false
                showAttachmentChooser = !showAttachmentChooser
            },
            onSend = ::sendTextMessage
        )
    }
}

@Composable
private fun ChatHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Team Chat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Text(
                text = "Room $SampleTeamCode",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TaskProgressCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, RelicBlue.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Baillieu Lantern",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Team progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "64%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = RelicBlue
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { 0.64f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = RelicBlue,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                LanternPuzzleSummary()
            }
        }
    }
}

@Composable
private fun LanternPuzzleSummary() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PuzzleGrid()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PuzzleNote(
                title = "Collected",
                value = "Lens, Handle, Wick",
                color = RelicGreen,
                modifier = Modifier.weight(1f)
            )
            PuzzleNote(
                title = "Missing",
                value = "Frame, Flame",
                color = RelicRed,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PuzzleGrid() {
    val pieces = listOf(true, true, false, true, false, true)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        pieces.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEachIndexed { index, collected ->
                    val color = if (collected) RelicGold else Color(0xFFCFD7D2)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = color.copy(alpha = if (collected) 0.28f else 0.42f),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (collected) {
                                RelicBadge(
                                    color = RelicGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = "?",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7A817E)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PuzzleNote(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.mine) {
            ChatAvatar(label = message.author, color = RelicBlue)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 286.dp),
            horizontalAlignment = if (message.mine) Alignment.End else Alignment.Start
        ) {
            Text(
                text = message.author,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (message.mine) RelicGreen else Color.White,
                border = if (message.mine) null else BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.mine) Color.White else Ink
                )
            }
        }

        if (message.mine) {
            Spacer(modifier = Modifier.width(8.dp))
            ChatAvatar(label = message.author, color = RelicGreen)
        }
    }
}

@Composable
private fun ChatAvatar(
    label: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.initials(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun AttachmentChooser(
    onPickGallery: () -> Unit,
    onOpenCamera: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AttachmentOption(
            icon = Icons.Rounded.Image,
            label = "Gallery",
            onClick = onPickGallery,
            modifier = Modifier.weight(1f)
        )
        AttachmentOption(
            icon = Icons.Rounded.PhotoCamera,
            label = "Camera",
            onClick = onOpenCamera,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AttachmentOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(70.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = RelicBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PackPanel(
    onEmojiSelected: (String) -> Unit,
    onItemSelected: (Relic) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(226.dp)
            .background(Color.White)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EmojiPicker(
            onEmojiSelected = onEmojiSelected,
            modifier = Modifier.weight(1f)
        )
        ItemPicker(
            onItemSelected = onItemSelected,
            modifier = Modifier.weight(2f)
        )
    }
}

@Composable
private fun EmojiPicker(
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val emojis = listOf("🙂", "😄", "👏", "🧭", "✨", "🎒", "📍", "🏛️")

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Emoji",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        emojis.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                row.forEach { emoji ->
                    EmojiCell(
                        emoji = emoji,
                        onClick = { onEmojiSelected(emoji) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmojiCell(
    emoji: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun ItemPicker(
    onItemSelected: (Relic) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = discoveredRelics.filter { it.progress > 0f }.take(6)

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Items",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        items.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { relic ->
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clickable { onItemSelected(relic) },
                        contentAlignment = Alignment.Center
                    ) {
                        RelicBadge(
                            color = relic.tone.toColor(),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    draft: String,
    packSelected: Boolean,
    addSelected: Boolean,
    onDraftChange: (String) -> Unit,
    onMessageFocused: () -> Unit,
    onVoiceClick: () -> Unit,
    onPackClick: () -> Unit,
    onAddClick: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleToolButton(
            icon = Icons.Rounded.Mic,
            contentDescription = "Voice",
            onClick = onVoiceClick
        )
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        onMessageFocused()
                    }
                },
            singleLine = true,
            placeholder = { Text("Message") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() })
        )
        CircleToolButton(
            icon = Icons.Rounded.EmojiEmotions,
            contentDescription = "Emoji and items",
            selected = packSelected,
            onClick = onPackClick
        )
        CircleToolButton(
            icon = Icons.Rounded.Add,
            contentDescription = "More",
            selected = addSelected,
            onClick = onAddClick
        )
        if (draft.isNotBlank()) {
            CircleToolButton(
                icon = Icons.AutoMirrored.Rounded.Send,
                contentDescription = "Send",
                selected = true,
                onClick = onSend
            )
        }
    }
}

@Composable
private fun CircleToolButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (selected) RelicGreen else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .size(42.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) RelicGreen.copy(alpha = 0.14f) else Color(0xFFF0F3F1)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = color
            )
        }
    }
}

private fun String.initials(): String {
    return split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString(separator = "") { it.first().uppercaseChar().toString() }
        .ifBlank { "LT" }
}
