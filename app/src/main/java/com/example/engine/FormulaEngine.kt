package com.example.engine

import com.example.data.model.SpreadsheetCell
import java.util.Locale

object FormulaEngine {

    /**
     * Converts coordinate like "A1" -> Pair(0, 0), "B3" -> Pair(2, 1)
     */
    fun parseCellReference(ref: String): Pair<Int, Int>? {
        val trimmed = ref.trim().uppercase(Locale.ROOT)
        val match = Regex("^([A-Z]+)(\\d+)$").find(trimmed) ?: return null
        val colLetters = match.groupValues[1]
        val rowNumber = match.groupValues[2].toIntOrNull() ?: return null

        var col = 0
        for (char in colLetters) {
            col = col * 26 + (char - 'A' + 1)
        }
        return Pair(rowNumber - 1, col - 1)
    }

    /**
     * Converts Pair(row, col) -> "A1", "B3"
     */
    fun toCellReference(row: Int, col: Int): String {
        var c = col + 1
        val sb = StringBuilder()
        while (c > 0) {
            val rem = (c - 1) % 26
            sb.append(('A' + rem))
            c = (c - 1) / 26
        }
        return "${sb.reverse()}${row + 1}"
    }

    /**
     * Evaluates a formula or raw string given the cell map.
     */
    fun evaluate(
        formulaOrValue: String,
        cellMap: Map<Pair<Int, Int>, SpreadsheetCell>
    ): String {
        val trimmed = formulaOrValue.trim()
        if (!trimmed.startsWith("=")) {
            return trimmed
        }

        val expr = trimmed.substring(1).trim()
        val upperExpr = expr.uppercase(Locale.ROOT)

        try {
            // Function: SUM(range)
            if (upperExpr.startsWith("SUM(") && upperExpr.endsWith(")")) {
                val inner = expr.substring(4, expr.length - 1)
                val sum = sumRange(inner, cellMap)
                return formatNumber(sum)
            }

            // Function: AVERAGE(range)
            if (upperExpr.startsWith("AVERAGE(") && upperExpr.endsWith(")")) {
                val inner = expr.substring(8, expr.length - 1)
                val (sum, count) = rangeStats(inner, cellMap)
                return if (count > 0) formatNumber(sum / count) else "0"
            }

            // Function: COUNT(range)
            if (upperExpr.startsWith("COUNT(") && upperExpr.endsWith(")")) {
                val inner = expr.substring(6, expr.length - 1)
                val (_, count) = rangeStats(inner, cellMap)
                return count.toString()
            }

            // Function: MAX(range)
            if (upperExpr.startsWith("MAX(") && upperExpr.endsWith(")")) {
                val inner = expr.substring(4, expr.length - 1)
                val max = rangeMax(inner, cellMap)
                return formatNumber(max)
            }

            // Arithmetic expression: replace cell references with their double values
            val tokenized = replaceCellReferencesWithValues(expr, cellMap)
            val result = SimpleExpressionParser.evaluate(tokenized)
            return formatNumber(result)
        } catch (e: Exception) {
            return "#VALUE!"
        }
    }

    private fun sumRange(inner: String, cellMap: Map<Pair<Int, Int>, SpreadsheetCell>): Double {
        var total = 0.0
        val parts = inner.split(",")
        for (part in parts) {
            val p = part.trim()
            if (p.contains(":")) {
                val rangeParts = p.split(":")
                val start = parseCellReference(rangeParts[0])
                val end = parseCellReference(rangeParts[1])
                if (start != null && end != null) {
                    val r1 = minOf(start.first, end.first)
                    val r2 = maxOf(start.first, end.first)
                    val c1 = minOf(start.second, end.second)
                    val c2 = maxOf(start.second, end.second)
                    for (r in r1..r2) {
                        for (c in c1..c2) {
                            val v = getCellNumericValue(r, c, cellMap)
                            total += v
                        }
                    }
                }
            } else {
                val ref = parseCellReference(p)
                if (ref != null) {
                    total += getCellNumericValue(ref.first, ref.second, cellMap)
                } else {
                    total += p.toDoubleOrNull() ?: 0.0
                }
            }
        }
        return total
    }

    private fun rangeStats(inner: String, cellMap: Map<Pair<Int, Int>, SpreadsheetCell>): Pair<Double, Int> {
        var total = 0.0
        var count = 0
        val parts = inner.split(",")
        for (part in parts) {
            val p = part.trim()
            if (p.contains(":")) {
                val rangeParts = p.split(":")
                val start = parseCellReference(rangeParts[0])
                val end = parseCellReference(rangeParts[1])
                if (start != null && end != null) {
                    val r1 = minOf(start.first, end.first)
                    val r2 = maxOf(start.first, end.first)
                    val c1 = minOf(start.second, end.second)
                    val c2 = maxOf(start.second, end.second)
                    for (r in r1..r2) {
                        for (c in c1..c2) {
                            val cell = cellMap[Pair(r, c)]
                            if (cell != null && cell.rawValue.isNotBlank()) {
                                val num = cell.rawValue.toDoubleOrNull()
                                if (num != null) {
                                    total += num
                                    count++
                                }
                            }
                        }
                    }
                }
            }
        }
        return Pair(total, count)
    }

    private fun rangeMax(inner: String, cellMap: Map<Pair<Int, Int>, SpreadsheetCell>): Double {
        var maxVal = Double.NEGATIVE_INFINITY
        if (inner.contains(":")) {
            val rangeParts = inner.split(":")
            val start = parseCellReference(rangeParts[0])
            val end = parseCellReference(rangeParts[1])
            if (start != null && end != null) {
                for (r in minOf(start.first, end.first)..maxOf(start.first, end.first)) {
                    for (c in minOf(start.second, end.second)..maxOf(start.second, end.second)) {
                        val num = getCellNumericValue(r, c, cellMap)
                        if (num > maxVal) maxVal = num
                    }
                }
            }
        }
        return if (maxVal == Double.NEGATIVE_INFINITY) 0.0 else maxVal
    }

    private fun getCellNumericValue(row: Int, col: Int, cellMap: Map<Pair<Int, Int>, SpreadsheetCell>): Double {
        val cell = cellMap[Pair(row, col)] ?: return 0.0
        return cell.rawValue.toDoubleOrNull() ?: 0.0
    }

    private fun replaceCellReferencesWithValues(
        expr: String,
        cellMap: Map<Pair<Int, Int>, SpreadsheetCell>
    ): String {
        return Regex("\\b([A-Za-z]+)(\\d+)\\b").replace(expr) { match ->
            val ref = parseCellReference(match.value)
            if (ref != null) {
                val v = getCellNumericValue(ref.first, ref.second, cellMap)
                v.toString()
            } else {
                match.value
            }
        }
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", value)
        }
    }
}

/**
 * Clean recursive descent parser for basic arithmetic expressions (+, -, *, /).
 */
internal object SimpleExpressionParser {

    fun evaluate(str: String): Double {
        val parser = Parser(str)
        return try {
            parser.parse()
        } catch (e: Exception) {
            0.0
        }
    }

    private class Parser(private val str: String) {
        private var pos = -1
        private var ch = -1

        init {
            nextChar()
        }

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            val x = parseExpression()
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val divisor = parseFactor()
                        x = if (divisor != 0.0) x / divisor else 0.0
                    }
                    else -> return x
                }
            }
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDoubleOrNull() ?: 0.0
            } else {
                x = 0.0
            }
            return x
        }
    }
}
