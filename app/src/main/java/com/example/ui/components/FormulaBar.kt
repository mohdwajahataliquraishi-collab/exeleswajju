package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.FormulaEngine
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun FormulaBar(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val cellRef = FormulaEngine.toCellReference(uiState.selectedCell.first, uiState.selectedCell.second)

    Surface(
        color = PureWhite,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Undo & Redo buttons
            IconButton(
                onClick = { viewModel.undo() },
                enabled = uiState.canUndo,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (uiState.canUndo) Midnight800 else Slate400,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { viewModel.redo() },
                enabled = uiState.canRedo,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("redo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (uiState.canRedo) Midnight800 else Slate400,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Cell Coordinate Pill (e.g., A1, B3)
            Surface(
                color = GridHeaderBg,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.testTag("cell_reference_badge")
            ) {
                Text(
                    text = cellRef,
                    color = Midnight800,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Excel fx indicator
            Text(
                text = "fx",
                color = EmeraldDark,
                fontWeight = FontWeight.Black,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Formula / Value Input Box
            Surface(
                color = GridBackground,
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GridBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            ) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    if (uiState.formulaText.isEmpty()) {
                        Text(
                            text = "Enter text or formula here",
                            color = Slate400,
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = uiState.formulaText,
                        onValueChange = { viewModel.updateFormulaText(it) },
                        textStyle = TextStyle(
                            color = Slate900,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(EmeraldGreen),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("formula_input_field")
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Commit Checkmark button
            IconButton(
                onClick = { viewModel.commitCurrentCell(uiState.formulaText) },
                modifier = Modifier
                    .size(34.dp)
                    .testTag("commit_formula_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Commit Formula",
                    tint = EmeraldGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Clear button
            if (uiState.formulaText.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.updateFormulaText("") },
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("clear_formula_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
