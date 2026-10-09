package com.example.data.repository

import com.example.data.local.AccountingBoxEntity
import com.example.data.local.AccountingDao
import com.example.data.local.WorkbookEntity
import com.example.data.model.AccountingBox
import com.example.data.model.AccountingEntry
import com.example.data.model.Workbook
import com.example.data.model.Worksheet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class AccountingRepository(private val dao: AccountingDao) {

    fun getBoxesForMonth(monthYear: String): Flow<List<AccountingBox>> {
        return dao.getBoxesForMonth(monthYear).map { list ->
            list.map { entity -> entity.toDomainModel() }
        }
    }

    fun getAllMonths(): Flow<List<String>> = dao.getAllMonths()

    suspend fun saveBox(box: AccountingBox) {
        dao.insertBox(box.toEntity())
    }

    suspend fun saveBoxes(boxes: List<AccountingBox>) {
        dao.insertBoxes(boxes.map { it.toEntity() })
    }

    suspend fun deleteBox(id: String) {
        dao.deleteBox(id)
    }

    fun getWorkbook(monthYear: String): Flow<Workbook?> {
        return dao.getWorkbook(monthYear).map { entity ->
            entity?.toWorkbookDomain()
        }
    }

    suspend fun saveWorkbook(workbook: Workbook) {
        // Save workbook entity
        dao.saveWorkbook(workbook.toWorkbookEntity())
        // Also persist all individual boxes for relational querying & gallery
        val allBoxes = workbook.sheets.flatMap { it.boxes }
        if (allBoxes.isNotEmpty()) {
            dao.insertBoxes(allBoxes.map { it.toEntity() })
        }
    }

    private fun AccountingBox.toEntity(): AccountingBoxEntity {
        val entriesArr = JSONArray()
        for (e in entries) {
            val obj = JSONObject().apply {
                put("id", e.id)
                put("date", e.date)
                put("desc", e.description)
                put("charbi", e.charbi)
                put("jama", e.jama)
                put("baki", e.baki)
                put("remarks", e.remarks)
            }
            entriesArr.put(obj)
        }

        val colsArr = JSONArray(columns)

        return AccountingBoxEntity(
            id = id,
            personName = personName,
            monthYear = monthYear,
            startRow = startRow,
            startCol = startCol,
            headerBgColor = headerBgColor,
            headerTextColor = headerTextColor,
            columnsJson = colsArr.toString(),
            entriesJson = entriesArr.toString(),
            totalCharbi = totalCharbi,
            totalJama = totalJama,
            currentBalance = currentBalance,
            isVerified = isVerified,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun AccountingBoxEntity.toDomainModel(): AccountingBox {
        val colsList = mutableListOf<String>()
        try {
            val colsArr = JSONArray(columnsJson)
            for (i in 0 until colsArr.length()) {
                colsList.add(colsArr.getString(i))
            }
        } catch (e: Exception) {
            colsList.addAll(listOf("Date", "Tafseel", "Charbi", "Jama", "Baki"))
        }

        val entriesList = mutableListOf<AccountingEntry>()
        try {
            val entriesArr = JSONArray(entriesJson)
            for (i in 0 until entriesArr.length()) {
                val obj = entriesArr.getJSONObject(i)
                entriesList.add(
                    AccountingEntry(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        date = obj.optString("date", ""),
                        description = obj.optString("desc", ""),
                        charbi = obj.optDouble("charbi", 0.0),
                        jama = obj.optDouble("jama", 0.0),
                        baki = obj.optDouble("baki", 0.0),
                        remarks = obj.optString("remarks", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return AccountingBox(
            id = id,
            personName = personName,
            monthYear = monthYear,
            startRow = startRow,
            startCol = startCol,
            headerBgColor = headerBgColor,
            headerTextColor = headerTextColor,
            columns = colsList,
            entries = entriesList,
            isVerified = isVerified
        )
    }

    private fun Workbook.toWorkbookEntity(): WorkbookEntity {
        val root = JSONObject().apply {
            put("id", id)
            put("name", name)
            put("monthYear", monthYear)
            put("activeSheetIndex", activeSheetIndex)
            val sheetsArr = JSONArray()
            for (sheet in sheets) {
                val sObj = JSONObject().apply {
                    put("id", sheet.id)
                    put("name", sheet.name)
                    val boxesArr = JSONArray()
                    for (b in sheet.boxes) {
                        boxesArr.put(JSONObject().apply {
                            put("id", b.id)
                            put("name", b.personName)
                            put("monthYear", b.monthYear)
                            put("headerBg", b.headerBgColor)
                            put("headerText", b.headerTextColor)
                            val entriesArr = JSONArray()
                            for (e in b.entries) {
                                entriesArr.put(JSONObject().apply {
                                    put("id", e.id)
                                    put("date", e.date)
                                    put("desc", e.description)
                                    put("charbi", e.charbi)
                                    put("jama", e.jama)
                                    put("baki", e.baki)
                                })
                            }
                            put("entries", entriesArr)
                        })
                    }
                    put("boxes", boxesArr)
                }
                sheetsArr.put(sObj)
            }
            put("sheets", sheetsArr)
        }

        return WorkbookEntity(
            monthYear = monthYear,
            workbookName = name,
            activeSheetIndex = activeSheetIndex,
            workbookJson = root.toString(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun WorkbookEntity.toWorkbookDomain(): Workbook {
        return try {
            val root = JSONObject(workbookJson)
            val sheetsList = mutableListOf<Worksheet>()
            val sheetsArr = root.optJSONArray("sheets")
            if (sheetsArr != null) {
                for (s in 0 until sheetsArr.length()) {
                    val sObj = sheetsArr.getJSONObject(s)
                    val sheetName = sObj.optString("name", "Sheet")
                    val boxesList = mutableListOf<AccountingBox>()
                    val boxesArr = sObj.optJSONArray("boxes")
                    if (boxesArr != null) {
                        for (b in 0 until boxesArr.length()) {
                            val bObj = boxesArr.getJSONObject(b)
                            val entriesList = mutableListOf<AccountingEntry>()
                            val eArr = bObj.optJSONArray("entries")
                            if (eArr != null) {
                                for (e in 0 until eArr.length()) {
                                    val eObj = eArr.getJSONObject(e)
                                    entriesList.add(
                                        AccountingEntry(
                                            id = eObj.optString("id"),
                                            date = eObj.optString("date"),
                                            description = eObj.optString("desc"),
                                            charbi = eObj.optDouble("charbi"),
                                            jama = eObj.optDouble("jama"),
                                            baki = eObj.optDouble("baki")
                                        )
                                    )
                                }
                            }
                            boxesList.add(
                                AccountingBox(
                                    id = bObj.optString("id"),
                                    personName = bObj.optString("name"),
                                    monthYear = bObj.optString("monthYear", monthYear),
                                    headerBgColor = bObj.optLong("headerBg", 0xFF0D1B2AL),
                                    headerTextColor = bObj.optLong("headerText", 0xFFFFFFFFL),
                                    entries = entriesList
                                )
                            )
                        }
                    }
                    sheetsList.add(
                        Worksheet(
                            id = sObj.optString("id"),
                            name = sheetName,
                            boxes = boxesList
                        )
                    )
                }
            }
            Workbook(
                id = root.optString("id"),
                name = workbookName,
                monthYear = monthYear,
                activeSheetIndex = activeSheetIndex,
                sheets = sheetsList
            )
        } catch (e: Exception) {
            Workbook(monthYear = monthYear)
        }
    }
}
