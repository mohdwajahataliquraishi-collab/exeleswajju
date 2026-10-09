package com.example.engine

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.AccountingBox
import com.example.data.model.Workbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {

    fun exportWorkbookToCsv(context: Context, workbook: Workbook): Pair<File, Uri> {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "EXELESWAJJU_${workbook.monthYear.replace(" ", "_")}_$dateStamp.csv"
        val file = File(exportDir, fileName)

        FileOutputStream(file).bufferedWriter().use { writer ->
            writer.write("EXELESWAJJU PRO - FINANCIAL STATEMENT\n")
            writer.write("Month/Year,${workbook.monthYear}\n")
            writer.write("Exported On,${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US).format(Date())}\n\n")

            for (sheet in workbook.sheets) {
                writer.write("SHEET: ${sheet.name}\n")
                if (sheet.boxes.isNotEmpty()) {
                    for (box in sheet.boxes) {
                        writer.write("ACCOUNT / BOX: ${box.personName}\n")
                        writer.write(box.columns.joinToString(",") + "\n")
                        for (entry in box.entries) {
                            writer.write("${entry.date},\"${entry.description}\",${entry.charbi},${entry.jama},${entry.baki}\n")
                        }
                        writer.write("TOTAL CHARBI,${box.totalCharbi}\n")
                        writer.write("TOTAL JAMA,${box.totalJama}\n")
                        writer.write("NET BALANCE (BAKI),${box.currentBalance}\n\n")
                    }
                } else {
                    // Regular grid export
                    val maxRow = sheet.cells.keys.maxOfOrNull { it.first } ?: 10
                    val maxCol = sheet.cells.keys.maxOfOrNull { it.second } ?: 10
                    for (r in 0..maxRow) {
                        val rowVals = mutableListOf<String>()
                        for (c in 0..maxCol) {
                            val v = sheet.cells[Pair(r, c)]?.rawValue ?: ""
                            rowVals.add("\"${v.replace("\"", "\"\"")}\"")
                        }
                        writer.write(rowVals.joinToString(",") + "\n")
                    }
                }
                writer.write("\n")
            }
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        return Pair(file, uri)
    }

    fun exportBoxToCsv(context: Context, box: AccountingBox, monthYear: String): Pair<File, Uri> {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = box.personName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val file = File(exportDir, "${safeName}_${monthYear.replace(" ", "_")}.csv")

        FileOutputStream(file).bufferedWriter().use { writer ->
            writer.write("EXELESWAJJU PRO - INDIVIDUAL ACCOUNT RECORD\n")
            writer.write("Account Name,${box.personName}\n")
            writer.write("Month/Year,$monthYear\n\n")
            writer.write(box.columns.joinToString(",") + "\n")
            for (entry in box.entries) {
                writer.write("${entry.date},\"${entry.description}\",${entry.charbi},${entry.jama},${entry.baki}\n")
            }
            writer.write("\n")
            writer.write("TOTAL CHARBI,${box.totalCharbi}\n")
            writer.write("TOTAL JAMA,${box.totalJama}\n")
            writer.write("NET BALANCE (BAKI),${box.currentBalance}\n")
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        return Pair(file, uri)
    }
}
