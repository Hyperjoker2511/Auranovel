package com.example.ui.screens.reader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.TtsManager
import com.example.ui.theme.ArcaneGold
import com.example.ui.theme.ArcanePrimary

@Composable
fun TtsControlBar(
    ttsManager: TtsManager,
    onClose: () -> Unit
) {
    val isPlaying by ttsManager.isPlaying.collectAsState()
    val speed by ttsManager.speed.collectAsState()
    val currentParagraphIndex by ttsManager.currentParagraphIndex.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 10.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = if (isPlaying) ArcaneGold else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Voice Narration (TTS)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ArcanePrimary
                    )
                    Text(
                        text = "Paragraph #${currentParagraphIndex + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { ttsManager.previous() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Prev Paragraph")
                }

                IconButton(
                    onClick = {
                        if (isPlaying) ttsManager.pause() else ttsManager.resume()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("tts_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = "Play/Pause",
                        tint = ArcaneGold,
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(onClick = { ttsManager.next() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next Paragraph")
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Speed button
                TextButton(
                    onClick = {
                        val nextSpeed = when {
                            speed < 1.0f -> 1.0f
                            speed < 1.25f -> 1.25f
                            speed < 1.5f -> 1.5f
                            speed < 2.0f -> 2.0f
                            else -> 0.8f
                        }
                        ttsManager.setSpeed(nextSpeed)
                    },
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text("${speed}x", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close TTS")
                }
            }
        }
    }
}
