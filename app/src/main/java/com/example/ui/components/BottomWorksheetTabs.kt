package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun BottomWorksheetTabs(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var isNewSheetDialogVisible by remember { mutableStateOf(false) }
    var newSheetName by remember { mutableStateOf("") }

    Surface(
        color = PureWhite,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Excel sheet switcher icon
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .background(GridHeaderBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TableChart,
                    contentDescription = "Worksheets",
                    tint = EmeraldDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Sheet tabs
            uiState.workbook.sheets.forEachIndexed { index, sheet ->
                val isSelected = index == uiState.workbook.activeSheetIndex

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable { viewModel.switchSheet(index) }
                        .padding(horizontal = 14.dp)
                        .testTag("worksheet_tab_${sheet.name}"),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = sheet.name,
                        color = if (isSelected) Midnight900 else Slate500,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Green underline indicator for active tab (matching Excel Mobile screenshot)
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(3.dp)
                                .background(EmeraldGreen, RoundedCornerShape(2.dp))
                        )
                    } else {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }

            // Add Sheet "+" Button
            IconButton(
                onClick = { isNewSheetDialogVisible = true },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("add_sheet_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Worksheet",
                    tint = Slate700
                )
            }
        }
    }

    if (isNewSheetDialogVisible) {
        AlertDialog(
            onDismissRequest = { isNewSheetDialogVisible = false },
            title = { Text("New Worksheet Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newSheetName,
                    onValueChange = { newSheetName = it },
                    placeholder = { Text("e.g. Sheet6 or Mandi Hisaab") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_sheet_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newSheetName.trim().ifBlank { "Sheet${uiState.workbook.sheets.size + 1}" }
                        viewModel.addNewSheet(name)
                        newSheetName = ""
                        isNewSheetDialogVisible = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                ) {
                    Text("Create Sheet", color = PureWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { isNewSheetDialogVisible = false }) {
                    Text("Cancel", color = Slate500)
                }
            }
        )
    }
}
