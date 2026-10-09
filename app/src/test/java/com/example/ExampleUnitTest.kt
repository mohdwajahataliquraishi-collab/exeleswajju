package com.example

import com.example.data.model.SpreadsheetCell
import com.example.engine.FormulaEngine
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testCellReferenceConversion() {
        assertEquals(Pair(0, 0), FormulaEngine.parseCellReference("A1"))
        assertEquals(Pair(1, 1), FormulaEngine.parseCellReference("B2"))
        assertEquals(Pair(9, 3), FormulaEngine.parseCellReference("D10"))

        assertEquals("A1", FormulaEngine.toCellReference(0, 0))
        assertEquals("B2", FormulaEngine.toCellReference(1, 1))
        assertEquals("D10", FormulaEngine.toCellReference(9, 3))
    }

    @Test
    fun testFormulaEvaluation() {
        val cellMap = mapOf(
            Pair(0, 0) to SpreadsheetCell(row = 0, col = 0, rawValue = "250"),
            Pair(1, 0) to SpreadsheetCell(row = 1, col = 0, rawValue = "500"),
            Pair(2, 0) to SpreadsheetCell(row = 2, col = 0, rawValue = "150")
        )

        // Sum range
        val sumRes = FormulaEngine.evaluate("=SUM(A1:A3)", cellMap)
        assertEquals("900", sumRes)

        // Basic arithmetic
        val addRes = FormulaEngine.evaluate("=A1+A2", cellMap)
        assertEquals("750", addRes)

        val multRes = FormulaEngine.evaluate("=10*25", cellMap)
        assertEquals("250", multRes)
    }
}
