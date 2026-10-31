package com.alamshowkat.bluekit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alamshowkat.bluekit.hid.ConsumerUsage
import com.alamshowkat.bluekit.hid.HidKey
import com.alamshowkat.bluekit.hid.HidState
import com.alamshowkat.bluekit.ui.theme.Amber
import com.alamshowkat.bluekit.ui.theme.Electric
import com.alamshowkat.bluekit.ui.theme.Ink
import com.alamshowkat.bluekit.ui.theme.Line
import com.alamshowkat.bluekit.ui.theme.Mint
import com.alamshowkat.bluekit.ui.theme.Muted
import com.alamshowkat.bluekit.ui.theme.Panel
import com.alamshowkat.bluekit.ui.theme.Paper
import com.alamshowkat.bluekit.ui.theme.Rose

@Composable
fun RemoteScreen(
    state: HidState,
    onBack: () -> Unit,
    onMediaDown: (Int) -> Unit,
    onMediaUp: () -> Unit,
    onMediaTap: (Int) -> Unit,
    onShortcut: (Int) -> Unit
) {
    var playing by remember { mutableStateOf(false) }

    ModeScaffold(
        title = "Media",
        subtitle = "Playback, volume, host navigation and slides",
        state = state,
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            DisconnectedNote(state, Modifier.padding(bottom = 10.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                SectionLabel("Playback")
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RemoteKey(
                        label = "Prev",
                        icon = Icons.Filled.SkipPrevious,
                        modifier = Modifier.weight(1f),
                        onPress = { onMediaDown(ConsumerUsage.PREVIOUS_TRACK) },
                        onRelease = onMediaUp
                    )
                    RemoteKey(
                        label = "Rewind",
                        icon = Icons.Filled.FastRewind,
                        modifier = Modifier.weight(1f),
                        onPress = { onMediaDown(ConsumerUsage.REWIND) },
                        onRelease = onMediaUp
                    )
                    RemoteKey(
                        label = if (playing) "Pause" else "Play",
                        icon = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        accent = Mint,
                        primary = true,
                        modifier = Modifier.weight(1.25f),
                        onPress = {
                            playing = !playing
                            onMediaTap(ConsumerUsage.PLAY_PAUSE)
                        },
                        onRelease = {}
                    )
                    RemoteKey(
                        label = "Forward",
                        icon = Icons.Filled.FastForward,
                        modifier = Modifier.weight(1f),
                        onPress = { onMediaDown(ConsumerUsage.FAST_FORWARD) },
                        onRelease = onMediaUp
                    )
                    RemoteKey(
                        label = "Next",
                        icon = Icons.Filled.SkipNext,
                        modifier = Modifier.weight(1f),
                        onPress = { onMediaDown(ConsumerUsage.NEXT_TRACK) },
                        onRelease = onMediaUp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                SectionLabel("Volume")
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RemoteKey(
                        label = "Down",
                        icon = Icons.AutoMirrored.Filled.VolumeDown,
                        modifier = Modifier.weight(1f),
                        onPress = { onMediaDown(ConsumerUsage.VOLUME_DOWN) },
                        onRelease = onMediaUp
                    )
                    RemoteKey(
                        label = "Mute",
                        icon = Icons.AutoMirrored.Filled.VolumeOff,
                        accent = Amber,
                        modifier = Modifier.weight(1f),
                        onPress = { onMediaTap(ConsumerUsage.MUTE) },
                        onRelease = {}
                    )
                    RemoteKey(
                        label = "Up",
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        modifier = Modifier.weight(1f),
                        onPress = { onMediaDown(ConsumerUsage.VOLUME_UP) },
                        onRelease = onMediaUp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                SectionLabel("Host navigation")
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RemoteKey(
                        label = "Home",
                        icon = Icons.Filled.Home,
                        accent = Electric,
                        modifier = Modifier.weight(1f),
                        onPress = { onMediaTap(ConsumerUsage.AC_HOME) },
                        onRelease = {}
                    )
                    RemoteKey(
                        label = "Back",
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        accent = Electric,
                        modifier = Modifier.weight(1f),
                        onPress = { onMediaTap(ConsumerUsage.AC_BACK) },
                        onRelease = {}
                    )
                    RemoteKey(
                        label = "Play/Pause",
                        icon = Icons.Filled.PlayArrow,
                        modifier = Modifier.weight(1f),
                        onPress = { onMediaTap(ConsumerUsage.PLAY_PAUSE) },
                        onRelease = {}
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                SectionLabel("Slides")
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RemoteKey(
                        label = "Previous",
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        modifier = Modifier.weight(1f),
                        onPress = { onShortcut(HidKey.PAGE_UP) },
                        onRelease = {}
                    )
                    RemoteKey(
                        label = "Next",
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        accent = Mint,
                        modifier = Modifier.weight(1f),
                        onPress = { onShortcut(HidKey.PAGE_DOWN) },
                        onRelease = {}
                    )
                    RemoteKey(
                        label = "Start",
                        icon = Icons.Filled.Slideshow,
                        accent = Amber,
                        modifier = Modifier.weight(1f),
                        onPress = { onShortcut(HidKey.F5) },
                        onRelease = {}
                    )
                    RemoteKey(
                        label = "End",
                        icon = Icons.Filled.KeyboardArrowDown,
                        accent = Rose,
                        modifier = Modifier.weight(1f),
                        onPress = { onShortcut(HidKey.ESCAPE) },
                        onRelease = {}
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArrowKey("Up", Icons.Filled.KeyboardArrowUp, Modifier.weight(1f)) {
                        onShortcut(HidKey.UP)
                    }
                    ArrowKey("Down", Icons.Filled.KeyboardArrowDown, Modifier.weight(1f)) {
                        onShortcut(HidKey.DOWN)
                    }
                    ArrowKey("Left", Icons.AutoMirrored.Filled.KeyboardArrowLeft, Modifier.weight(1f)) {
                        onShortcut(HidKey.LEFT)
                    }
                    ArrowKey("Right", Icons.AutoMirrored.Filled.KeyboardArrowRight, Modifier.weight(1f)) {
                        onShortcut(HidKey.RIGHT)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ArrowKey(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    RemoteKey(
        label = label,
        icon = icon,
        modifier = modifier,
        onPress = onClick,
        onRelease = {}
    )
}

@Composable
private fun RemoteKey(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = Electric,
    primary: Boolean = false,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    var held by remember { mutableStateOf(false) }
    val currentPress by rememberUpdatedState(onPress)
    val currentRelease by rememberUpdatedState(onRelease)
    val shape = RoundedCornerShape(16.dp)
    val container = when {
        primary && held -> accent
        primary -> accent.copy(alpha = 0.2f)
        held -> accent.copy(alpha = 0.24f)
        else -> Panel.copy(alpha = 0.75f)
    }
    val content = when {
        primary && held -> Ink
        primary -> accent
        held -> accent
        else -> Paper
    }

    Column(
        modifier = modifier
            .clip(shape)
            .background(container)
            .border(1.dp, if (held || primary) accent.copy(alpha = 0.55f) else Line.copy(alpha = 0.7f), shape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        held = true
                        currentPress()
                        try {
                            tryAwaitRelease()
                        } finally {
                            held = false
                            currentRelease()
                        }
                    }
                )
            }
            .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = content,
                modifier = Modifier.size(if (primary) 28.dp else 22.dp)
            )
            Spacer(Modifier.height(5.dp))
        }
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
