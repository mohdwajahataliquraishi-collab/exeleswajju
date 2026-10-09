package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var monthMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = Midnight800,
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Top Row: Brand Monogram + Title + Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Brand Monogram & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("brand_header")
                ) {
                    // Logo Mark
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(GoldAccent, VioletPrimary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "EW",
                            color = PureWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "EXELESWAJJU",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                color = GoldAccent,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    color = Midnight900,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = uiState.workbook.name,
                            color = CyanBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Action Icons: Gallery, Validate, Calculator, Design, Export
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Monthly Screenshot Gallery Button (Mandatory CUJ)
                    IconButton(
                        onClick = { viewModel.openScreenshotGallery() },
                        modifier = Modifier
                            .testTag("open_gallery_button")
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Monthly Screenshot Gallery",
                            tint = CyanBright
                        )
                    }

                    // Month-End Validation / Audit Button
                    IconButton(
                        onClick = { viewModel.runMonthEndValidation() },
                        modifier = Modifier
                            .testTag("validate_month_button")
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Month-End Audit",
                            tint = EmeraldGreen
                        )
                    }

                    // Side Calculator Toggle
                    IconButton(
                        onClick = { viewModel.toggleCalculator() },
                        modifier = Modifier
                            .testTag("calculator_toggle_button")
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Calculator",
                            tint = if (uiState.isCalculatorOpen) GoldAccent else PureWhite
                        )
                    }

                    // Design Studio Toggle
                    IconButton(
                        onClick = { viewModel.toggleDesignStudio() },
                        modifier = Modifier
                            .testTag("design_studio_toggle_button")
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Design Studio",
                            tint = if (uiState.isDesignStudioOpen) VioletLight else PureWhite
                        )
                    }

                    // Export / Share
                    IconButton(
                        onClick = { viewModel.exportToExcel(context) },
                        modifier = Modifier
                            .testTag("export_excel_button")
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export & Share",
                            tint = PureWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-row: Month Selector Dropdown & Autosave status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Month Selector Pill
                Box {
                    Surface(
                        color = Midnight700,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .clickable { monthMenuExpanded = true }
                            .testTag("month_selector_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = CyanBright,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.workbook.monthYear,
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = monthMenuExpanded,
                        onDismissRequest = { monthMenuExpanded = false }
                    ) {
                        uiState.availableMonths.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    viewModel.switchMonth(m)
                                    monthMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Autosave Emerald Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("autosave_status")
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = uiState.autosaveStatus,
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
