package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AccountingRepository
import com.example.engine.*
import com.example.voice.VoiceAction
import com.example.voice.VoiceAssistantManager
import com.example.voice.VoiceState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    WORKSPACE,
    SCREENSHOT_GALLERY,
    MONTHLY_ARCHIVE
}

data class AuditIssue(
    val type: IssueType,
    val personName: String,
    val message: String,
    val isResolved: Boolean = false
)

enum class IssueType {
    MISSING_DATE,
    CALCULATION_DISCREPANCY,
    DUPLICATE_NAME,
    EMPTY_AMOUNT
}

data class UiState(
    val currentScreen: AppScreen = AppScreen.WORKSPACE,
    val workbook: Workbook = Workbook(),
    val selectedCell: Pair<Int, Int> = Pair(1, 1),
    val formulaText: String = "",
    val activeBoxId: String? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isDesignStudioOpen: Boolean = false,
    val isCalculatorOpen: Boolean = false,
    val calcExpression: String = "",
    val calcResult: String = "0",
    val calcHistory: List<String> = emptyList(),
    val isVoiceDialogOpen: Boolean = false,
    val generatedScreenshots: List<GeneratedScreenshot> = emptyList(),
    val selectedScreenshotNames: Set<String> = emptySet(),
    val isGeneratingScreenshots: Boolean = false,
    val validationIssues: List<AuditIssue> = emptyList(),
    val isValidationDialogOpen: Boolean = false,
    val autosaveStatus: String = "All changes saved locally",
    val availableMonths: List<String> = listOf("October 2026", "September 2026", "August 2026")
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AccountingRepository
    val voiceManager: VoiceAssistantManager

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val voiceState: StateFlow<VoiceState>

    private val undoStack = mutableListOf<Workbook>()
    private val redoStack = mutableListOf<Workbook>()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AccountingRepository(db.accountingDao())
        voiceManager = VoiceAssistantManager(application)
        voiceState = voiceManager.voiceState

        voiceManager.onActionDetected = { action, originalText ->
            handleVoiceAction(action, originalText)
        }

        // Initialize default workbook based on user's real accounting workflow (Charbi Account)
        loadInitialWorkbook()
    }

    private fun loadInitialWorkbook() {
        viewModelScope.launch {
            val initialMonth = "October 2026"
            repository.getWorkbook(initialMonth).collectLatest { savedWorkbook ->
                if (savedWorkbook != null && savedWorkbook.sheets.isNotEmpty()) {
                    _uiState.update { it.copy(workbook = savedWorkbook) }
                } else {
                    // Seed initial realistic accounting workbook
                    val seeded = createDefaultAccountingWorkbook(initialMonth)
                    _uiState.update { it.copy(workbook = seeded) }
                    repository.saveWorkbook(seeded)
                }
            }
        }
    }

    private fun createDefaultAccountingWorkbook(monthYear: String): Workbook {
        // Initial accounts as seen in reference screenshots
        val salmanBox = AccountingBox(
            personName = "Salman",
            monthYear = monthYear,
            startRow = 2,
            startCol = 0,
            headerBgColor = 0xFF0D1B2AL,
            entries = listOf(
                AccountingEntry(date = "01 Oct", description = "Initial maal entry", charbi = 1200.0, jama = 500.0, baki = 700.0),
                AccountingEntry(date = "05 Oct", description = "Cash paid via GPay", charbi = 0.0, jama = 400.0, baki = 300.0),
                AccountingEntry(date = "08 Oct", description = "Charbi supply 25kg", charbi = 1850.0, jama = 1000.0, baki = 1150.0)
            )
        )

        val naveedBox = AccountingBox(
            personName = "Naveed Quraishi",
            monthYear = monthYear,
            startRow = 9,
            startCol = 0,
            headerBgColor = 0xFF1E1B4BL,
            entries = listOf(
                AccountingEntry(date = "02 Oct", description = "Mandi supply lot 1", charbi = 3400.0, jama = 2000.0, baki = 1400.0),
                AccountingEntry(date = "06 Oct", description = "Advance jama", charbi = 0.0, jama = 1400.0, baki = 0.0),
                AccountingEntry(date = "09 Oct", description = "Charbi dispatch 50kg", charbi = 4250.0, jama = 1500.0, baki = 2750.0)
            )
        )

        val imranBox = AccountingBox(
            personName = "Imran",
            monthYear = monthYear,
            startRow = 16,
            startCol = 0,
            headerBgColor = 0xFF064E3BL,
            entries = listOf(
                AccountingEntry(date = "03 Oct", description = "Weekly bill payment", charbi = 950.0, jama = 950.0, baki = 0.0),
                AccountingEntry(date = "07 Oct", description = "Special charbi cut", charbi = 1600.0, jama = 500.0, baki = 1100.0)
            )
        )

        val charbiSheet = Worksheet(
            name = "Charbi Account",
            boxes = listOf(salmanBox, naveedBox, imranBox)
        )

        val sheet4 = Worksheet(name = "Sheet4", boxes = emptyList())
        val sheet5 = Worksheet(name = "Sheet5", boxes = emptyList())

        return Workbook(
            name = "charbi account.xlsx",
            monthYear = monthYear,
            activeSheetIndex = 0,
            sheets = listOf(charbiSheet, sheet4, sheet5)
        )
    }

    private fun pushUndo() {
        undoStack.add(_uiState.value.workbook)
        if (undoStack.size > 30) undoStack.removeAt(0)
        redoStack.clear()
        _uiState.update { it.copy(canUndo = true, canRedo = false) }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(_uiState.value.workbook)
            _uiState.update {
                it.copy(
                    workbook = previous,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = true,
                    autosaveStatus = "Undone changes"
                )
            }
            saveCurrentWorkbook()
            voiceManager.speak("Undo ho gaya")
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(_uiState.value.workbook)
            _uiState.update {
                it.copy(
                    workbook = next,
                    canUndo = true,
                    canRedo = redoStack.isNotEmpty(),
                    autosaveStatus = "Redone changes"
                )
            }
            saveCurrentWorkbook()
            voiceManager.speak("Redo ho gaya")
        }
    }

    fun selectCell(row: Int, col: Int) {
        val currentSheet = getActiveSheet()
        val cell = currentSheet.cells[Pair(row, col)]
        val raw = cell?.rawValue ?: ""
        val box = findBoxContainingCell(row, col)

        _uiState.update {
            it.copy(
                selectedCell = Pair(row, col),
                formulaText = raw,
                activeBoxId = box?.id
            )
        }
    }

    fun updateFormulaText(text: String) {
        _uiState.update { it.copy(formulaText = text) }
    }

    fun commitCurrentCell(value: String) {
        pushUndo()
        val (row, col) = _uiState.value.selectedCell
        val evaluated = FormulaEngine.evaluate(value, getActiveSheet().cells)

        updateCellInActiveSheet(row, col) { existing ->
            (existing ?: SpreadsheetCell(row = row, col = col)).copy(
                rawValue = evaluated,
                formula = if (value.startsWith("=")) value else null
            )
        }
        _uiState.update { it.copy(formulaText = evaluated) }
        saveCurrentWorkbook()
    }

    private fun updateCellInActiveSheet(row: Int, col: Int, transform: (SpreadsheetCell?) -> SpreadsheetCell) {
        val currentWb = _uiState.value.workbook
        val currentSheet = getActiveSheet()
        val updatedCells = currentSheet.cells.toMutableMap()
        val updatedCell = transform(updatedCells[Pair(row, col)])
        updatedCells[Pair(row, col)] = updatedCell

        val updatedSheet = currentSheet.copy(cells = updatedCells)
        val updatedSheets = currentWb.sheets.toMutableList()
        updatedSheets[currentWb.activeSheetIndex] = updatedSheet

        _uiState.update { it.copy(workbook = currentWb.copy(sheets = updatedSheets)) }
    }

    fun switchSheet(index: Int) {
        val wb = _uiState.value.workbook
        if (index in wb.sheets.indices) {
            _uiState.update { it.copy(workbook = wb.copy(activeSheetIndex = index)) }
        }
    }

    fun addNewSheet(name: String = "Sheet${_uiState.value.workbook.sheets.size + 1}") {
        pushUndo()
        val wb = _uiState.value.workbook
        val newSheet = Worksheet(name = name)
        val updatedSheets = wb.sheets + newSheet
        _uiState.update {
            it.copy(workbook = wb.copy(sheets = updatedSheets, activeSheetIndex = updatedSheets.lastIndex))
        }
        saveCurrentWorkbook()
    }

    // --- Dynamic People Boxes Operations ---

    fun createAccountingBox(personName: String) {
        pushUndo()
        val currentSheet = getActiveSheet()
        // Determine start row based on existing boxes
        val lastRow = currentSheet.boxes.maxOfOrNull { it.startRow + it.entries.size + 3 } ?: 1
        val newBox = AccountingBox(
            personName = personName,
            monthYear = _uiState.value.workbook.monthYear,
            startRow = lastRow,
            startCol = 0,
            headerBgColor = 0xFF0D1B2AL,
            entries = listOf(
                AccountingEntry(
                    date = SimpleDateFormat("dd MMM", Locale.US).format(Date()),
                    description = "Opening balance",
                    charbi = 0.0,
                    jama = 0.0,
                    baki = 0.0
                )
            )
        )

        val updatedBoxes = currentSheet.boxes + newBox
        updateActiveSheetBoxes(updatedBoxes)
        _uiState.update { it.copy(activeBoxId = newBox.id) }
        saveCurrentWorkbook()
        voiceManager.speak("${personName} ka box ban gaya hai")
    }

    fun addEntryToBox(
        boxId: String,
        date: String,
        desc: String,
        charbi: Double,
        jama: Double
    ) {
        pushUndo()
        val currentSheet = getActiveSheet()
        val box = currentSheet.boxes.find { it.id == boxId } ?: return
        val prevBaki = if (box.entries.isNotEmpty()) box.entries.last().baki else 0.0
        val newBaki = prevBaki + charbi - jama

        val newEntry = AccountingEntry(
            date = date.ifBlank { SimpleDateFormat("dd MMM", Locale.US).format(Date()) },
            description = desc,
            charbi = charbi,
            jama = jama,
            baki = newBaki
        )

        val updatedEntries = box.entries + newEntry
        val updatedBox = box.copy(entries = updatedEntries)
        val updatedBoxes = currentSheet.boxes.map { if (it.id == boxId) updatedBox else it }
        updateActiveSheetBoxes(updatedBoxes)
        saveCurrentWorkbook()
        voiceManager.speak("Entry save ho gayi. Naya baki hai ₹${formatAmount(newBaki)}")
    }

    private fun updateActiveSheetBoxes(boxes: List<AccountingBox>) {
        val wb = _uiState.value.workbook
        val sheet = getActiveSheet()
        val updatedSheet = sheet.copy(boxes = boxes)
        val updatedSheets = wb.sheets.toMutableList()
        updatedSheets[wb.activeSheetIndex] = updatedSheet
        _uiState.update { it.copy(workbook = wb.copy(sheets = updatedSheets)) }
    }

    // --- Voice Action Handler ---

    fun handleVoiceAction(action: VoiceAction, rawTranscript: String) {
        when (action) {
            is VoiceAction.CreateBox -> {
                createAccountingBox(action.personName)
            }
            is VoiceAction.InsertDate -> {
                val box = getActiveOrFirstBox()
                if (box != null) {
                    addEntryToBox(box.id, action.date, "Daily entry", 0.0, 0.0)
                } else {
                    commitCurrentCell(action.date)
                }
                voiceManager.speak("Date ${action.date} darj kar di")
            }
            is VoiceAction.InsertAmount -> {
                val box = getActiveOrFirstBox()
                if (box != null) {
                    val isCharbi = action.field.equals("Charbi", ignoreCase = true)
                    val charbiAmt = if (isCharbi) action.amount else 0.0
                    val jamaAmt = if (!isCharbi) action.amount else 0.0
                    addEntryToBox(
                        box.id,
                        SimpleDateFormat("dd MMM", Locale.US).format(Date()),
                        if (isCharbi) "Charbi maal" else "Jama cash",
                        charbiAmt,
                        jamaAmt
                    )
                } else {
                    commitCurrentCell(action.amount.toString())
                }
                voiceManager.speak("${action.field} mein ${formatAmount(action.amount)} likh diya hai")
            }
            is VoiceAction.AddToAmount -> {
                val box = getActiveOrFirstBox()
                if (box != null) {
                    addEntryToBox(
                        box.id,
                        SimpleDateFormat("dd MMM", Locale.US).format(Date()),
                        "Additional maal",
                        action.amount,
                        0.0
                    )
                    voiceManager.speak("${formatAmount(action.amount)} jod diya hai")
                } else {
                    val currentVal = _uiState.value.formulaText.toDoubleOrNull() ?: 0.0
                    val total = currentVal + action.amount
                    commitCurrentCell(total.toString())
                    voiceManager.speak("${formatAmount(action.amount)} jod diya. Total: ${formatAmount(total)}")
                }
            }
            is VoiceAction.SetDescription -> {
                val (r, c) = _uiState.value.selectedCell
                commitCurrentCell(action.description)
                voiceManager.speak("Description likh di hai")
            }
            is VoiceAction.FormatCell -> {
                applyFormatting(action.property, action.value)
                voiceManager.speak("Formatting update kar di hai")
            }
            is VoiceAction.QueryTotal -> {
                val currentSheet = getActiveSheet()
                if (action.targetPerson != null) {
                    val box = currentSheet.boxes.find { it.personName.contains(action.targetPerson, ignoreCase = true) }
                    if (box != null) {
                        val reply = "${box.personName} ka total charbi ₹${formatAmount(box.totalCharbi)}, jama ₹${formatAmount(box.totalJama)}, aur baki balance ₹${formatAmount(box.currentBalance)} hai."
                        voiceManager.speak(reply)
                    } else {
                        voiceManager.speak("${action.targetPerson} ka koi record nahi mila")
                    }
                } else {
                    val totalCharbi = currentSheet.boxes.sumOf { it.totalCharbi }
                    val totalJama = currentSheet.boxes.sumOf { it.totalJama }
                    val totalBaki = currentSheet.boxes.sumOf { it.currentBalance }
                    val reply = "Is mahine ka kul charbi ₹${formatAmount(totalCharbi)}, jama ₹${formatAmount(totalJama)}, aur kul baki ₹${formatAmount(totalBaki)} hai."
                    voiceManager.speak(reply)
                }
            }
            is VoiceAction.OpenMonth -> {
                switchMonth(action.monthName)
                voiceManager.speak("${action.monthName} ka hisaab khol diya hai")
            }
            is VoiceAction.Undo -> {
                undo()
            }
            is VoiceAction.Redo -> {
                redo()
            }
            is VoiceAction.CalculatorOp -> {
                handleCalculatorVoice(action.operation, action.amount)
            }
            is VoiceAction.CompleteMonthHisaab -> {
                openScreenshotGallery()
                voiceManager.speak("Mahine ka poora hisaab tayyar hai. Screenshot gallery kholi gayi hai.")
            }
            is VoiceAction.FinalizeMonthValidation -> {
                runMonthEndValidation()
                voiceManager.speak("Mahine ke hisaab ki verification shuru kar di gayi hai.")
            }
            is VoiceAction.Ambiguous -> {
                voiceManager.speak(action.question)
            }
            is VoiceAction.Unknown -> {
                // If it looks like a number, add to calculator or cell
                val num = action.rawText.toDoubleOrNull()
                if (num != null) {
                    commitCurrentCell(num.toString())
                }
            }
        }
    }

    // --- Formatting Tools ---

    fun applyFormatting(property: String, value: Any) {
        pushUndo()
        val (row, col) = _uiState.value.selectedCell
        updateCellInActiveSheet(row, col) { existing ->
            val cell = existing ?: SpreadsheetCell(row = row, col = col)
            when (property) {
                "bold" -> cell.copy(isBold = !(cell.isBold))
                "italic" -> cell.copy(isItalic = !(cell.isItalic))
                "font_size" -> cell.copy(fontSize = (value as? String)?.toIntOrNull() ?: 14)
                "header_color" -> {
                    val colorHex = when (value) {
                        "violet" -> 0xFF7C3AEDL
                        "blue" -> 0xFF0D1B2AL
                        "emerald" -> 0xFF059669L
                        else -> 0xFF0F172AL
                    }
                    cell.copy(backgroundColor = colorHex, textColor = 0xFFFFFFFFL)
                }
                "background" -> {
                    val bgHex = when (value) {
                        "soft_blue" -> 0xFFE0F2FEL
                        "soft_green" -> 0xFFD1FAE5L
                        "soft_yellow" -> 0xFFFEF3C7L
                        "soft_violet" -> 0xFFEDE9FEL
                        else -> 0xFFFFFFFFL
                    }
                    cell.copy(backgroundColor = bgHex)
                }
                "border" -> cell.copy(borderStyle = BorderStyle.THIN)
                else -> cell
            }
        }
        saveCurrentWorkbook()
    }

    // --- Side Calculator Operations ---

    fun handleCalcInput(key: String) {
        val currentExpr = _uiState.value.calcExpression
        when (key) {
            "C" -> _uiState.update { it.copy(calcExpression = "", calcResult = "0") }
            "=" -> evaluateCalculator()
            "DEL" -> {
                if (currentExpr.isNotEmpty()) {
                    _uiState.update { it.copy(calcExpression = currentExpr.dropLast(1)) }
                }
            }
            "INSERT_TO_CELL" -> {
                val res = _uiState.value.calcResult
                commitCurrentCell(res)
                voiceManager.speak("Result ₹$res cell mein daal diya")
            }
            else -> {
                val updated = currentExpr + key
                _uiState.update { it.copy(calcExpression = updated) }
            }
        }
    }

    private fun evaluateCalculator() {
        val expr = _uiState.value.calcExpression
        if (expr.isBlank()) return
        val res = SimpleExpressionParser.evaluate(expr)
        val formatted = if (res % 1.0 == 0.0) res.toLong().toString() else String.format(Locale.US, "%.2f", res)
        val historyEntry = "$expr = $formatted"
        _uiState.update {
            it.copy(
                calcResult = formatted,
                calcHistory = listOf(historyEntry) + it.calcHistory.take(15)
            )
        }
    }

    private fun handleCalculatorVoice(operation: String, amount: Double) {
        when (operation) {
            "insert_to_cell" -> {
                val res = _uiState.value.calcResult
                commitCurrentCell(res)
                voiceManager.speak("Result cell mein daal diya")
            }
            "minus" -> {
                val expr = "${_uiState.value.calcResult} - $amount"
                _uiState.update { it.copy(calcExpression = expr) }
                evaluateCalculator()
                voiceManager.speak("$amount ghata diya. Naya total: ${_uiState.value.calcResult}")
            }
            "multiply" -> {
                val expr = "${_uiState.value.calcResult} * $amount"
                _uiState.update { it.copy(calcExpression = expr) }
                evaluateCalculator()
                voiceManager.speak("Naya total: ${_uiState.value.calcResult}")
            }
            else -> {
                val expr = if (_uiState.value.calcExpression.isBlank()) amount.toString() else "${_uiState.value.calcResult} + $amount"
                _uiState.update { it.copy(calcExpression = expr) }
                evaluateCalculator()
                voiceManager.speak("$amount jod diya. Total: ${_uiState.value.calcResult}")
            }
        }
    }

    // --- Monthly Screenshot Gallery ---

    fun openScreenshotGallery() {
        _uiState.update { it.copy(currentScreen = AppScreen.SCREENSHOT_GALLERY, isGeneratingScreenshots = true) }
        viewModelScope.launch {
            val context = getApplication<Application>()
            val currentSheet = getActiveSheet()
            val monthYear = _uiState.value.workbook.monthYear

            val generated = mutableListOf<GeneratedScreenshot>()
            for (box in currentSheet.boxes) {
                val screenshot = ScreenshotGenerator.generateBoxScreenshot(context, box, monthYear)
                generated.add(screenshot)
            }

            _uiState.update {
                it.copy(
                    generatedScreenshots = generated,
                    selectedScreenshotNames = generated.map { g -> g.personName }.toSet(),
                    isGeneratingScreenshots = false
                )
            }
        }
    }

    fun toggleScreenshotSelection(personName: String) {
        val current = _uiState.value.selectedScreenshotNames.toMutableSet()
        if (current.contains(personName)) current.remove(personName) else current.add(personName)
        _uiState.update { it.copy(selectedScreenshotNames = current) }
    }

    fun selectAllScreenshots() {
        val all = _uiState.value.generatedScreenshots.map { it.personName }.toSet()
        _uiState.update { it.copy(selectedScreenshotNames = all) }
    }

    fun deselectAllScreenshots() {
        _uiState.update { it.copy(selectedScreenshotNames = emptySet()) }
    }

    fun shareSelectedScreenshots(context: Context) {
        val selected = _uiState.value.generatedScreenshots.filter {
            _uiState.value.selectedScreenshotNames.contains(it.personName)
        }
        if (selected.isEmpty()) return

        if (selected.size == 1) {
            val item = selected.first()
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, item.uri)
                putExtra(Intent.EXTRA_SUBJECT, "EXELESWAJJU PRO - ${item.personName} Hisaab (${item.monthYear})")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "EXELESWAJJU PRO: Account Statement for ${item.personName} (${item.monthYear}).\nTotal Charbi: ₹${formatAmount(item.totalCharbi)}\nTotal Jama: ₹${formatAmount(item.totalJama)}\nNet Balance: ₹${formatAmount(item.currentBalance)}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share ${item.personName} Hisaab via..."))
        } else {
            val uris = ArrayList<Uri>(selected.map { it.uri })
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/png"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_SUBJECT, "EXELESWAJJU PRO - Monthly Statements (${_uiState.value.workbook.monthYear})")
                putExtra(Intent.EXTRA_TEXT, "EXELESWAJJU PRO: ${selected.size} Account Statements attached for ${_uiState.value.workbook.monthYear}.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Monthly Statements via..."))
        }
    }

    // --- Month-End Validation ---

    fun runMonthEndValidation() {
        val currentSheet = getActiveSheet()
        val issues = mutableListOf<AuditIssue>()

        // 1. Check duplicate person names
        val nameCounts = currentSheet.boxes.groupingBy { it.personName.trim().lowercase(Locale.ROOT) }.eachCount()
        for ((name, count) in nameCounts) {
            if (count > 1) {
                issues.add(
                    AuditIssue(
                        type = IssueType.DUPLICATE_NAME,
                        personName = name.capitalizeWords(),
                        message = "Multiple boxes found with name '$name'. Please consolidate."
                    )
                )
            }
        }

        // 2. Check each box for missing dates or math discrepancies
        for (box in currentSheet.boxes) {
            var running = 0.0
            for ((idx, entry) in box.entries.withIndex()) {
                if (entry.date.isBlank()) {
                    issues.add(
                        AuditIssue(
                            type = IssueType.MISSING_DATE,
                            personName = box.personName,
                            message = "Row ${idx + 1} has an empty date."
                        )
                    )
                }
                val expectedBaki = running + entry.charbi - entry.jama
                if (Math.abs(entry.baki - expectedBaki) > 0.01 && entry.baki != 0.0) {
                    issues.add(
                        AuditIssue(
                            type = IssueType.CALCULATION_DISCREPANCY,
                            personName = box.personName,
                            message = "Row ${idx + 1} balance discrepancy: recorded ₹${entry.baki}, calculated ₹$expectedBaki."
                        )
                    )
                }
                running = expectedBaki
            }
        }

        _uiState.update {
            it.copy(
                validationIssues = issues,
                isValidationDialogOpen = true
            )
        }
    }

    fun dismissValidationDialog() {
        _uiState.update { it.copy(isValidationDialogOpen = false) }
    }

    // --- Navigation & UI Toggles ---

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun toggleDesignStudio() {
        _uiState.update { it.copy(isDesignStudioOpen = !it.isDesignStudioOpen) }
    }

    fun toggleCalculator() {
        _uiState.update { it.copy(isCalculatorOpen = !it.isCalculatorOpen) }
    }

    fun toggleVoiceDialog(open: Boolean) {
        _uiState.update { it.copy(isVoiceDialogOpen = open) }
        if (open) {
            voiceManager.startListening()
        } else {
            voiceManager.stopListening()
        }
    }

    fun switchMonth(newMonthYear: String) {
        viewModelScope.launch {
            repository.getWorkbook(newMonthYear).collectLatest { saved ->
                if (saved != null) {
                    _uiState.update { it.copy(workbook = saved) }
                } else {
                    val newWb = Workbook(monthYear = newMonthYear, name = "charbi account.xlsx")
                    _uiState.update { it.copy(workbook = newWb) }
                    repository.saveWorkbook(newWb)
                }
            }
        }
    }

    fun exportToExcel(context: Context) {
        val (_, uri) = ExcelExporter.exportWorkbookToCsv(context, _uiState.value.workbook)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "EXELESWAJJU PRO Workbook Export - ${_uiState.value.workbook.monthYear}")
            putExtra(Intent.EXTRA_TEXT, "Here is the exported Excel/CSV workbook for ${_uiState.value.workbook.monthYear}.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export Workbook via..."))
    }

    private fun saveCurrentWorkbook() {
        viewModelScope.launch {
            val wb = _uiState.value.workbook.copy(lastSavedMillis = System.currentTimeMillis())
            repository.saveWorkbook(wb)
            val time = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
            _uiState.update { it.copy(autosaveStatus = "Autosaved at $time") }
        }
    }

    private fun getActiveSheet(): Worksheet {
        val wb = _uiState.value.workbook
        return wb.sheets.getOrNull(wb.activeSheetIndex) ?: Worksheet(name = "Charbi Account")
    }

    private fun getActiveOrFirstBox(): AccountingBox? {
        val currentSheet = getActiveSheet()
        val activeId = _uiState.value.activeBoxId
        return currentSheet.boxes.find { it.id == activeId } ?: currentSheet.boxes.firstOrNull()
    }

    private fun findBoxContainingCell(row: Int, col: Int): AccountingBox? {
        return getActiveSheet().boxes.find { box ->
            row >= box.startRow && row <= box.startRow + box.entries.size + 2 &&
            col >= box.startCol && col <= box.startCol + box.columns.size
        }
    }

    private fun formatAmount(amt: Double): String {
        return if (amt % 1.0 == 0.0) String.format(Locale.US, "%,d", amt.toLong()) else String.format(Locale.US, "%,.2f", amt)
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
