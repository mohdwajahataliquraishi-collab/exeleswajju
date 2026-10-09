package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountingBox
import com.example.data.model.AccountingEntry
import com.example.data.model.SpreadsheetCell
import com.example.engine.FormulaEngine
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SpreadsheetGrid(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentSheet = uiState.workbook.sheets.getOrNull(uiState.workbook.activeSheetIndex)
    val boxes = currentSheet?.boxes ?: emptyList()

    val horizontalScrollState = rememberScrollState()

    // Dialog state for adding a new entry to an accounting box
    var activeBoxForEntry by remember { mutableStateOf<AccountingBox?>(null) }

    // Column widths: Date(90dp), Desc(150dp), Charbi(95dp), Jama(95dp), Baki(100dp), Extra F..J(80dp each)
    val colHeaders = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
    val colWidths = listOf(95.dp, 160.dp, 100.dp, 100.dp, 110.dp, 85.dp, 85.dp, 85.dp, 85.dp, 85.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GridBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Sticky Column Letters Header: A, B, C, D, E...
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GridHeaderBg)
                    .horizontalScroll(horizontalScrollState)
            ) {
                // Top-left corner cell (empty corner above row numbers)
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(28.dp)
                        .border(0.5.dp, GridBorder)
                        .background(Slate200),
                    contentAlignment = Alignment.Center
                ) {
                    Text("◢", color = Slate400, fontSize = 9.sp)
                }

                // Column letter labels
                colHeaders.forEachIndexed { index, letter ->
                    Box(
                        modifier = Modifier
                            .width(colWidths[index])
                            .height(28.dp)
                            .border(0.5.dp, GridBorder)
                            .background(GridHeaderBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter,
                            color = Slate700,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Main Spreadsheet Canvas with Horizontal & Vertical Scrolling
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight()
                ) {
                    // 1. Accounting Boxes rendered directly as organized Excel tables
                    boxes.forEach { box ->
                        item(key = "box_header_${box.id}") {
                            AccountingBoxHeaderRow(
                                box = box,
                                totalWidth = colWidths.take(5).sumOf { it.value.toDouble() }.dp + 42.dp,
                                onAddEntryClick = { activeBoxForEntry = box }
                            )
                        }

                        // Column headers for this box (Date, Description, Charbi, Jama, Baki)
                        item(key = "box_cols_${box.id}") {
                            AccountingBoxColumnsRow(box = box, colWidths = colWidths)
                        }

                        // Entry rows
                        items(box.entries, key = { it.id }) { entry ->
                            AccountingBoxEntryRow(
                                box = box,
                                entry = entry,
                                colWidths = colWidths,
                                selectedCell = uiState.selectedCell,
                                onCellClick = { r, c -> viewModel.selectCell(r, c) }
                            )
                        }

                        // Totals summary row
                        item(key = "box_total_${box.id}") {
                            AccountingBoxTotalsRow(box = box, colWidths = colWidths)
                        }

                        // Spacer between boxes
                        item(key = "box_space_${box.id}") {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // 2. Extra Free Grid Rows for General Excel Cell Editing & Calculations
                    val startingRow = boxes.sumOf { it.entries.size + 4 } + 1
                    items(25) { rOffset ->
                        val rowIndex = startingRow + rOffset
                        FreeformGridRow(
                            rowIndex = rowIndex,
                            colHeaders = colHeaders,
                            colWidths = colWidths,
                            cells = currentSheet?.cells ?: emptyMap(),
                            selectedCell = uiState.selectedCell,
                            onCellClick = { r, c -> viewModel.selectCell(r, c) }
                        )
                    }
                }
            }
        }

        // Add Entry Modal Dialog
        activeBoxForEntry?.let { targetBox ->
            AddEntryDialog(
                box = targetBox,
                onDismiss = { activeBoxForEntry = null },
                onConfirm = { date, desc, charbi, jama ->
                    viewModel.addEntryToBox(targetBox.id, date, desc, charbi, jama)
                    activeBoxForEntry = null
                }
            )
        }
    }
}

@Composable
private fun AccountingBoxHeaderRow(
    box: AccountingBox,
    totalWidth: androidx.compose.ui.unit.Dp,
    onAddEntryClick: () -> Unit
) {
    Surface(
        color = Color(box.headerBgColor),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        modifier = Modifier
            .width(totalWidth)
            .padding(top = 10.dp, start = 4.dp, end = 4.dp)
            .testTag("box_header_${box.personName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = box.personName.uppercase(Locale.ROOT),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Surface(
                    color = Midnight700,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Net Baki: ₹${formatNum(box.currentBalance)}",
                        color = if (box.currentBalance > 0) CyanBright else EmeraldGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Quick Add Entry Button
            Button(
                onClick = onAddEntryClick,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .height(28.dp)
                    .testTag("add_entry_button_${box.personName}")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add Entry",
                    color = PureWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AccountingBoxColumnsRow(
    box: AccountingBox,
    colWidths: List<androidx.compose.ui.unit.Dp>
) {
    Row(
        modifier = Modifier
            .padding(start = 4.dp, end = 4.dp)
            .background(Midnight700)
    ) {
        // Row label placeholder (42dp)
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(30.dp)
                .border(0.5.dp, GridBorder)
                .background(Slate800),
            contentAlignment = Alignment.Center
        ) {
            Text("#", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        val titles = listOf("Date", "Tafseel / Desc", "Charbi (₹)", "Jama (₹)", "Baki (₹)")
        titles.forEachIndexed { idx, title ->
            Box(
                modifier = Modifier
                    .width(colWidths[idx])
                    .height(30.dp)
                    .border(0.5.dp, GridBorder)
                    .padding(horizontal = 6.dp),
                contentAlignment = if (idx >= 2) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Text(
                    text = title,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun AccountingBoxEntryRow(
    box: AccountingBox,
    entry: AccountingEntry,
    colWidths: List<androidx.compose.ui.unit.Dp>,
    selectedCell: Pair<Int, Int>,
    onCellClick: (Int, Int) -> Unit
) {
    val rowIdx = box.entries.indexOf(entry) + box.startRow

    Row(
        modifier = Modifier
            .padding(start = 4.dp, end = 4.dp)
            .background(PureWhite)
    ) {
        // Row Number
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(34.dp)
                .border(0.5.dp, GridBorder)
                .background(GridHeaderBg),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "$rowIdx", color = Slate500, fontSize = 10.sp)
        }

        // 5 Columns: Date, Desc, Charbi, Jama, Baki
        val values = listOf(
            entry.date,
            entry.description,
            if (entry.charbi > 0) formatNum(entry.charbi) else "-",
            if (entry.jama > 0) formatNum(entry.jama) else "-",
            formatNum(entry.baki)
        )

        values.forEachIndexed { colIdx, text ->
            val isSelected = selectedCell.first == rowIdx && selectedCell.second == colIdx
            val alignEnd = colIdx >= 2

            Box(
                modifier = Modifier
                    .width(colWidths[colIdx])
                    .height(34.dp)
                    .background(if (isSelected) CyanGlow else PureWhite)
                    .border(
                        width = if (isSelected) 2.dp else 0.5.dp,
                        color = if (isSelected) EmeraldDark else GridBorder
                    )
                    .clickable { onCellClick(rowIdx, colIdx) }
                    .padding(horizontal = 8.dp),
                contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Text(
                    text = text,
                    color = when {
                        colIdx == 2 && entry.charbi > 0 -> Color(0xFFDC2626) // Charbi (Red/Amber)
                        colIdx == 3 && entry.jama > 0 -> EmeraldDark // Jama (Green)
                        colIdx == 4 -> Midnight900 // Baki (Navy)
                        else -> Slate900
                    },
                    fontWeight = if (colIdx == 4 || colIdx == 0) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Corner resize handle indicator if selected (matching Excel mobile)
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(6.dp)
                            .background(EmeraldDark)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountingBoxTotalsRow(
    box: AccountingBox,
    colWidths: List<androidx.compose.ui.unit.Dp>
) {
    Row(
        modifier = Modifier
            .padding(start = 4.dp, end = 4.dp)
            .background(EmeraldLight)
            .border(1.dp, EmeraldGreen)
    ) {
        // Row Number placeholder
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(34.dp)
                .border(0.5.dp, GridBorder)
                .background(EmeraldLight),
            contentAlignment = Alignment.Center
        ) {
            Text("Σ", color = EmeraldDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        // Date & Desc span
        Box(
            modifier = Modifier
                .width(colWidths[0] + colWidths[1])
                .height(34.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "TOTALS & BALANCE",
                color = EmeraldDark,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        // Total Charbi
        Box(
            modifier = Modifier
                .width(colWidths[2])
                .height(34.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "₹${formatNum(box.totalCharbi)}",
                color = Color(0xFFB91C1C),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        // Total Jama
        Box(
            modifier = Modifier
                .width(colWidths[3])
                .height(34.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "₹${formatNum(box.totalJama)}",
                color = EmeraldDark,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        // Net Baki
        Box(
            modifier = Modifier
                .width(colWidths[4])
                .height(34.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "₹${formatNum(box.currentBalance)}",
                color = Midnight900,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun FreeformGridRow(
    rowIndex: Int,
    colHeaders: List<String>,
    colWidths: List<androidx.compose.ui.unit.Dp>,
    cells: Map<Pair<Int, Int>, SpreadsheetCell>,
    selectedCell: Pair<Int, Int>,
    onCellClick: (Int, Int) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Sticky Row Number on Left
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(32.dp)
                .border(0.5.dp, GridBorder)
                .background(GridHeaderBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rowIndex",
                color = Slate500,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Cells A..J
        colHeaders.forEachIndexed { colIndex, _ ->
            val isSelected = selectedCell.first == rowIndex && selectedCell.second == colIndex
            val cell = cells[Pair(rowIndex, colIndex)]
            val displayVal = cell?.rawValue ?: ""

            Box(
                modifier = Modifier
                    .width(colWidths[colIndex])
                    .height(32.dp)
                    .background(
                        if (isSelected) CyanGlow else if (cell != null) Color(cell.backgroundColor) else PureWhite
                    )
                    .border(
                        width = if (isSelected) 2.dp else 0.5.dp,
                        color = if (isSelected) EmeraldDark else GridBorder
                    )
                    .clickable { onCellClick(rowIndex, colIndex) }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = displayVal,
                    color = if (cell != null) Color(cell.textColor) else Slate900,
                    fontWeight = if (cell?.isBold == true) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(6.dp)
                            .background(EmeraldDark)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddEntryDialog(
    box: AccountingBox,
    onDismiss: () -> Unit,
    onConfirm: (date: String, desc: String, charbi: Double, jama: Double) -> Unit
) {
    var dateText by remember { mutableStateOf(SimpleDateFormat("dd MMM", Locale.US).format(Date())) }
    var descText by remember { mutableStateOf("") }
    var charbiText by remember { mutableStateOf("") }
    var jamaText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Entry: ${box.personName}",
                color = Midnight900,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (e.g. 12 Oct)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("entry_date_input")
                )
                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text("Description / Tafseel") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("entry_desc_input")
                )
                OutlinedTextField(
                    value = charbiText,
                    onValueChange = { charbiText = it },
                    label = { Text("Charbi Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("entry_charbi_input")
                )
                OutlinedTextField(
                    value = jamaText,
                    onValueChange = { jamaText = it },
                    label = { Text("Jama / Cash Paid (₹)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("entry_jama_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val charbi = charbiText.toDoubleOrNull() ?: 0.0
                    val jama = jamaText.toDoubleOrNull() ?: 0.0
                    onConfirm(dateText, descText, charbi, jama)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                modifier = Modifier.testTag("submit_entry_button")
            ) {
                Text("Save Entry", color = PureWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate500)
            }
        }
    )
}

private fun formatNum(value: Double): String {
    return if (value % 1.0 == 0.0) String.format(Locale.US, "%,d", value.toLong()) else String.format(Locale.US, "%,.2f", value)
}
