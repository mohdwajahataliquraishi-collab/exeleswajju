package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounting_boxes")
data class AccountingBoxEntity(
    @PrimaryKey
    val id: String,
    val personName: String,
    val monthYear: String,
    val startRow: Int,
    val startCol: Int,
    val headerBgColor: Long,
    val headerTextColor: Long,
    val columnsJson: String,
    val entriesJson: String,
    val totalCharbi: Double,
    val totalJama: Double,
    val currentBalance: Double,
    val isVerified: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "workbooks")
data class WorkbookEntity(
    @PrimaryKey
    val monthYear: String, // e.g. "October 2026"
    val workbookName: String,
    val activeSheetIndex: Int,
    val workbookJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)
