package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountingDao {

    @Query("SELECT * FROM accounting_boxes WHERE monthYear = :monthYear ORDER BY updatedAt ASC")
    fun getBoxesForMonth(monthYear: String): Flow<List<AccountingBoxEntity>>

    @Query("SELECT * FROM accounting_boxes ORDER BY updatedAt DESC")
    fun getAllBoxes(): Flow<List<AccountingBoxEntity>>

    @Query("SELECT DISTINCT monthYear FROM accounting_boxes ORDER BY monthYear DESC")
    fun getAllMonths(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBox(box: AccountingBoxEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoxes(boxes: List<AccountingBoxEntity>)

    @Query("DELETE FROM accounting_boxes WHERE id = :id")
    suspend fun deleteBox(id: String)

    @Query("SELECT * FROM workbooks WHERE monthYear = :monthYear LIMIT 1")
    fun getWorkbook(monthYear: String): Flow<WorkbookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWorkbook(workbook: WorkbookEntity)
}
