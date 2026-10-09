package com.example.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.ui.theme.*

/**
 * Core spreadsheet cell model supporting real styling, formulas, and values.
 */
data class SpreadsheetCell(
    val row: Int,
    val col: Int,
    val rawValue: String = "",
    val formula: String? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val fontSize: Int = 13,
    val textColor: Long = 0xFF0F172AL, // Slate900
    val backgroundColor: Long = 0xFFFFFFFFL, // Clean ivory/white
    val borderStyle: BorderStyle = BorderStyle.THIN,
    val alignment: CellAlignment = CellAlignment.LEFT,
    val numberFormat: NumberFormat = NumberFormat.GENERAL
) {
    val displayValue: String
        get() = rawValue
}

enum class BorderStyle {
    NONE, THIN, MEDIUM, DOUBLE
}

enum class CellAlignment {
    LEFT, CENTER, RIGHT
}

enum class NumberFormat {
    GENERAL, CURRENCY, NUMBER, DATE
}

/**
 * Represents a dynamic accounting box for an individual person or account
 * (e.g., Salman, Naveed Quraishi, Imran, Charbi Account).
 */
data class AccountingBox(
    val id: String = java.util.UUID.randomUUID().toString(),
    val personName: String,
    val monthYear: String,
    val startRow: Int = 1,
    val startCol: Int = 0,
    val headerBgColor: Long = 0xFF0D1B2AL, // Midnight Blue
    val headerTextColor: Long = 0xFFFFFFFFL,
    val columns: List<String> = listOf("Date", "Tafseel / Desc", "Charbi", "Jama", "Baki"),
    val entries: List<AccountingEntry> = emptyList(),
    val isVerified: Boolean = false
) {
    val totalCharbi: Double
        get() = entries.sumOf { it.charbi }

    val totalJama: Double
        get() = entries.sumOf { it.jama }

    val currentBalance: Double
        get() = if (entries.isNotEmpty()) entries.last().baki else 0.0
}

/**
 * Individual accounting entry in a person's box.
 */
data class AccountingEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val date: String,
    val description: String = "",
    val charbi: Double = 0.0,
    val jama: Double = 0.0,
    val baki: Double = 0.0,
    val remarks: String = ""
)

/**
 * A worksheet inside the workbook (e.g., "charbi account", "Salman", "Sheet4", "Sheet5").
 */
data class Worksheet(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val cells: Map<Pair<Int, Int>, SpreadsheetCell> = emptyMap(),
    val boxes: List<AccountingBox> = emptyList()
)

/**
 * Complete Excel workbook containing worksheets and monthly tracking.
 */
data class Workbook(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "charbi account.xlsx",
    val monthYear: String = "October 2026",
    val activeSheetIndex: Int = 0,
    val sheets: List<Worksheet> = emptyList(),
    val lastSavedMillis: Long = System.currentTimeMillis()
)
