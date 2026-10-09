package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun VoiceFloatingControl(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val voiceState by viewModel.voiceState.collectAsState()
    val isListening = voiceState.isListening

    // Pulsing animation when listening
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing pulse ring
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(CyanBright.copy(alpha = 0.5f), Color.Transparent)))
            )
        }

        // Primary Floating Microphone Button
        FloatingActionButton(
            onClick = { viewModel.toggleVoiceDialog(true) },
            containerColor = VioletPrimary,
            contentColor = PureWhite,
            shape = CircleShape,
            modifier = Modifier
                .size(56.dp)
                .testTag("floating_voice_button")
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.MicNone else Icons.Default.Mic,
                contentDescription = "Voice Assistant",
                tint = if (isListening) CyanBright else PureWhite,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val voiceState by viewModel.voiceState.collectAsState()
    var manualTextInput by remember { mutableStateOf("") }
    val chipScroll = rememberScrollState()

    val quickVoiceSuggestions = listOf(
        "Salman ka box banao",
        "Charbi mein 250 likho",
        "Jama mein 5000 likho",
        "500 aur add karo",
        "Date 12 October daalo",
        "Is mahine ka total bata",
        "Mahine ka poora hisaab complete karo",
        "Mahine ka hisaab final karo",
        "Heading ka colour violet kar",
        "Undo karo"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Midnight900,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Slate500)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Assistant Title + Active Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = CyanBright,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXELESWAJJU VOICE ASSISTANT",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    color = if (voiceState.isListening) VioletLight else Midnight700,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (voiceState.isListening) "Listening..." else "Ready",
                        color = PureWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Large Microphone Action Sphere
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = if (voiceState.isListening) listOf(CyanBright, VioletPrimary) else listOf(Midnight700, Midnight600)
                        )
                    )
                    .clickable {
                        if (voiceState.isListening) {
                            viewModel.voiceManager.stopListening()
                        } else {
                            viewModel.voiceManager.startListening()
                        }
                    }
                    .testTag("voice_dialog_mic_circle")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Toggle Speech Recognition",
                    tint = PureWhite,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recognized Speech Transcript Box
            Surface(
                color = Midnight800,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Midnight600),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "RECOGNIZED TRANSCRIPT (HINDI / ENGLISH / HYDERABADI):",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = voiceState.lastTranscript.ifBlank { "Tap microphone and speak in Roman Hindi or English..." },
                        color = if (voiceState.lastTranscript.isNotBlank()) PureWhite else Slate500,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (voiceState.intendedActionDescription.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Action: ${voiceState.intendedActionDescription}",
                                color = CyanBright,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    voiceState.speechError?.let { err ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = err,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Manual Text Command Input Fallback
            OutlinedTextField(
                value = manualTextInput,
                onValueChange = { manualTextInput = it },
                placeholder = { Text("Or type command: 'Salman ka box banao'...", color = Slate400, fontSize = 13.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (manualTextInput.isNotBlank()) {
                        viewModel.voiceManager.processVoiceInput(manualTextInput)
                        manualTextInput = ""
                    }
                }),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (manualTextInput.isNotBlank()) {
                                viewModel.voiceManager.processVoiceInput(manualTextInput)
                                manualTextInput = ""
                            }
                        },
                        modifier = Modifier.testTag("send_manual_voice_command")
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = CyanBright)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanBright,
                    unfocusedBorderColor = Midnight600,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_voice_text_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Hinglish Voice Chips
            Text(
                text = "TRY QUICK COMMANDS:",
                color = Slate400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickVoiceSuggestions.forEach { suggestion ->
                    Surface(
                        color = Midnight700,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .clickable { viewModel.voiceManager.processVoiceInput(suggestion) }
                            .testTag("voice_chip_$suggestion")
                    ) {
                        Text(
                            text = suggestion,
                            color = CyanGlow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
