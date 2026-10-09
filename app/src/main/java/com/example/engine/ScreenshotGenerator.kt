package com.example.engine

import android.content.Context
import android.graphics.*
import androidx.core.content.FileProvider
import com.example.data.model.AccountingBox
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

data class GeneratedScreenshot(
    val personName: String,
    val monthYear: String,
    val file: File,
    val uri: android.net.Uri,
    val bitmap: Bitmap,
    val totalCharbi: Double,
    val totalJama: Double,
    val currentBalance: Double,
    val timestamp: Long = System.currentTimeMillis()
)

object ScreenshotGenerator {

    private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    fun generateBoxScreenshot(
        context: Context,
        box: AccountingBox,
        monthYear: String
    ): GeneratedScreenshot {
        val width = 1200
        val headerHeight = 220
        val colHeaderHeight = 60
        val rowHeight = 55
        val summaryHeight = 160
        val footerHeight = 70

        val rowCount = maxOf(box.entries.size, 1)
        val totalHeight = headerHeight + colHeaderHeight + (rowCount * rowHeight) + summaryHeight + footerHeight

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background - Clean Ivory White
        val bgPaint = Paint().apply {
            color = Color.parseColor("#FCFBF7")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), totalHeight.toFloat(), bgPaint)

        // 1. Top Executive Banner (Deep Midnight Blue)
        val bannerPaint = Paint().apply {
            color = Color.parseColor("#0D1B2A")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight.toFloat(), bannerPaint)

        // Gold luxury accent line under banner
        val goldLinePaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            strokeWidth = 6f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(0f, headerHeight.toFloat(), width.toFloat(), headerHeight.toFloat(), goldLinePaint)

        // Brand Title
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("EXELESWAJJU PRO", 60f, 70f, brandPaint)

        // Brand Subtitle
        val subBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8") // Electric Cyan
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("OFFICIAL FINANCIAL STATEMENT & HISABB RECORD", 60f, 105f, subBrandPaint)

        // Person / Box Name Title
        val personPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FBBF24") // Gold
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("HISAAB: ${box.personName.uppercase(Locale.ROOT)}", 60f, 168f, personPaint)

        // Month & Year Tag on Right
        val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 28f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(monthYear, (width - 60).toFloat(), 70f, monthPaint)

        val verifiedTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981") // Emerald Green
            textSize = 22f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("✓ VERIFIED ACCORD", (width - 60).toFloat(), 110f, verifiedTagPaint)

        // 2. Table Column Headers
        val tableMargin = 40f
        val tableWidth = width - (tableMargin * 2)
        val colWidths = floatArrayOf(
            tableWidth * 0.18f, // Date
            tableWidth * 0.32f, // Description
            tableWidth * 0.16f, // Charbi
            tableWidth * 0.16f, // Jama
            tableWidth * 0.18f  // Baki
        )
        val colHeaders = listOf("DATE", "DESCRIPTION / TAFSEEL", "CHARBI (₹)", "JAMA (₹)", "BAKI / BAL (₹)")

        var currentY = headerHeight + 20f

        val colHeaderBgPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.FILL
        }
        canvas.drawRect(tableMargin, currentY, width - tableMargin, currentY + colHeaderHeight, colHeaderBgPaint)

        val colHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var colX = tableMargin
        for (i in colHeaders.indices) {
            val alignRight = i >= 2
            if (alignRight) {
                colHeaderTextPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(colHeaders[i], colX + colWidths[i] - 20f, currentY + 38f, colHeaderTextPaint)
            } else {
                colHeaderTextPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(colHeaders[i], colX + 20f, currentY + 38f, colHeaderTextPaint)
            }
            colX += colWidths[i]
        }

        currentY += colHeaderHeight

        // 3. Rows
        val rowTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 22f
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1.2f
        }
        val altRowPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }

        if (box.entries.isEmpty()) {
            rowTextPaint.textAlign = Paint.Align.CENTER
            rowTextPaint.color = Color.parseColor("#64748B")
            canvas.drawText("No entries recorded for this month", width / 2f, currentY + 35f, rowTextPaint)
            currentY += rowHeight
        } else {
            for ((idx, entry) in box.entries.withIndex()) {
                if (idx % 2 == 1) {
                    canvas.drawRect(tableMargin, currentY, width - tableMargin, currentY + rowHeight, altRowPaint)
                }

                // Grid line under row
                canvas.drawLine(tableMargin, currentY + rowHeight, width - tableMargin, currentY + rowHeight, linePaint)

                var cellX = tableMargin

                // Date
                rowTextPaint.textAlign = Paint.Align.LEFT
                rowTextPaint.color = Color.parseColor("#334155")
                rowTextPaint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText(entry.date, cellX + 20f, currentY + 36f, rowTextPaint)
                cellX += colWidths[0]

                // Description
                rowTextPaint.typeface = Typeface.DEFAULT
                rowTextPaint.color = Color.parseColor("#0F172A")
                val descText = if (entry.description.length > 25) entry.description.take(23) + "..." else entry.description
                canvas.drawText(descText, cellX + 20f, currentY + 36f, rowTextPaint)
                cellX += colWidths[1]

                // Charbi
                rowTextPaint.textAlign = Paint.Align.RIGHT
                rowTextPaint.color = if (entry.charbi > 0) Color.parseColor("#B91C1C") else Color.parseColor("#64748B")
                canvas.drawText(formatAmt(entry.charbi), cellX + colWidths[2] - 20f, currentY + 36f, rowTextPaint)
                cellX += colWidths[2]

                // Jama
                rowTextPaint.color = if (entry.jama > 0) Color.parseColor("#047857") else Color.parseColor("#64748B")
                canvas.drawText(formatAmt(entry.jama), cellX + colWidths[3] - 20f, currentY + 36f, rowTextPaint)
                cellX += colWidths[3]

                // Baki
                rowTextPaint.color = Color.parseColor("#0F172A")
                rowTextPaint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText(formatAmt(entry.baki), cellX + colWidths[4] - 20f, currentY + 36f, rowTextPaint)

                currentY += rowHeight
            }
        }

        // Table outer border
        val tableBorderPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }
        canvas.drawRect(tableMargin, headerHeight + 20f, width - tableMargin, currentY, tableBorderPaint)

        // 4. Totals & Balance Summary Box
        currentY += 25f
        val summaryBgPaint = Paint().apply {
            color = Color.parseColor("#F0FDF4") // Soft emerald background
            style = Paint.Style.FILL
        }
        val summaryBorderPaint = Paint().apply {
            color = Color.parseColor("#10B981")
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }
        val summaryRect = RectF(tableMargin, currentY, width - tableMargin, currentY + 115f)
        canvas.drawRoundRect(summaryRect, 16f, 16f, summaryBgPaint)
        canvas.drawRoundRect(summaryRect, 16f, 16f, summaryBorderPaint)

        // Summary labels and values
        val sumLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#065F46")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sumValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Total Charbi
        canvas.drawText("TOTAL CHARBI:", tableMargin + 35f, currentY + 45f, sumLabelPaint)
        canvas.drawText("₹ ${formatAmt(box.totalCharbi)}", tableMargin + 35f, currentY + 88f, sumValPaint)

        // Total Jama
        canvas.drawText("TOTAL JAMA (PAID):", tableMargin + 420f, currentY + 45f, sumLabelPaint)
        sumValPaint.color = Color.parseColor("#047857")
        canvas.drawText("₹ ${formatAmt(box.totalJama)}", tableMargin + 420f, currentY + 88f, sumValPaint)

        // Current Balance (Baki)
        canvas.drawText("NET BALANCE (BAKI):", tableMargin + 820f, currentY + 45f, sumLabelPaint)
        sumValPaint.color = Color.parseColor("#1E1B4B")
        canvas.drawText("₹ ${formatAmt(box.currentBalance)}", tableMargin + 820f, currentY + 88f, sumValPaint)

        currentY += 140f

        // 5. Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "EXELESWAJJU PRO • Real Mobile Accounting System • Confidential Financial Document",
            width / 2f,
            totalHeight - 30f,
            footerPaint
        )

        // Save Bitmap to File
        val screenshotsDir = File(context.cacheDir, "screenshots").apply { mkdirs() }
        val safeName = box.personName.replace("[^a-zA-Z0-9_-]".toRegex(), "_").lowercase(Locale.ROOT)
        val safeMonth = monthYear.replace("[^a-zA-Z0-9_-]".toRegex(), "_").lowercase(Locale.ROOT)
        val imageFile = File(screenshotsDir, "${safeName}_${safeMonth}.png")

        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, imageFile)

        return GeneratedScreenshot(
            personName = box.personName,
            monthYear = monthYear,
            file = imageFile,
            uri = uri,
            bitmap = bitmap,
            totalCharbi = box.totalCharbi,
            totalJama = box.totalJama,
            currentBalance = box.currentBalance
        )
    }

    private fun formatAmt(amt: Double): String {
        return if (amt % 1.0 == 0.0) {
            String.format(Locale.US, "%,d", amt.toLong())
        } else {
            String.format(Locale.US, "%,.2f", amt)
        }
    }
}
