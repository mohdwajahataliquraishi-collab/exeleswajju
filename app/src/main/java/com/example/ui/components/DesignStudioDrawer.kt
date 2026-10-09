package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun DesignStudioDrawer(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFontSize by remember { mutableStateOf(14) }

    Surface(
        color = Midnight800,
        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
        shadowElevation = 16.dp,
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .testTag("design_studio_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = VioletLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DESIGN STUDIO",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Text Style Toggles (Bold, Italic, Underline)
            Text("TEXT FORMATTING", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.applyFormatting("bold", true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Midnight700),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("format_bold_button")
                ) {
                    Text("B", fontWeight = FontWeight.Black, fontSize = 16.sp, color = PureWhite)
                }
                Button(
                    onClick = { viewModel.applyFormatting("italic", true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Midnight700),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("format_italic_button")
                ) {
                    Text("I", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 16.sp, color = PureWhite)
                }
                Button(
                    onClick = { viewModel.applyFormatting("border", true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Midnight700),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("format_border_button")
                ) {
                    Icon(imageVector = Icons.Default.BorderAll, contentDescription = "Border", tint = PureWhite, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Font Size Stepper
            Text("FONT SIZE ($selectedFontSize pt)", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        if (selectedFontSize > 10) {
                            selectedFontSize -= 2
                            viewModel.applyFormatting("font_size", selectedFontSize.toString())
                        }
                    },
                    modifier = Modifier.background(Midnight700, RoundedCornerShape(6.dp))
                ) {
                    Text("-", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                Text(
                    text = "$selectedFontSize",
                    color = CyanBright,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                IconButton(
                    onClick = {
                        if (selectedFontSize < 24) {
                            selectedFontSize += 2
                            viewModel.applyFormatting("font_size", selectedFontSize.toString())
                        }
                    },
                    modifier = Modifier.background(Midnight700, RoundedCornerShape(6.dp))
                ) {
                    Text("+", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Cell Background Color Palette
            Text("CELL BACKGROUND COLOR", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val bgColors = listOf(
                Pair("soft_blue", Color(0xFFE0F2FE)),
                Pair("soft_green", Color(0xFFD1FAE5)),
                Pair("soft_yellow", Color(0xFFFEF3C7)),
                Pair("soft_violet", Color(0xFFEDE9FE)),
                Pair("white", Color(0xFFFFFFFF))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                bgColors.forEach { (name, color) ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, Slate400, CircleShape)
                            .clickable { viewModel.applyFormatting("background", name) }
                            .testTag("bg_color_$name")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Header Color Preset
            Text("HEADER THEME PRESET", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val headerThemes = listOf(
                Pair("Midnight Navy", "blue"),
                Pair("Royal Violet", "violet"),
                Pair("Emerald Mandi", "emerald")
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                headerThemes.forEach { (title, key) ->
                    Surface(
                        color = Midnight700,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.applyFormatting("header_color", key) }
                            .testTag("header_preset_$key")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (key) {
                                            "violet" -> VioletPrimary
                                            "emerald" -> EmeraldGreen
                                            else -> Midnight900
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(title, color = PureWhite, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
