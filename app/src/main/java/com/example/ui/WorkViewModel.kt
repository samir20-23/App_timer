package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

class WorkViewModel : ViewModel() {

    private val _appState = MutableStateFlow(AppState())
    val appState: StateFlow<AppState> = _appState

    private val _currentMonth = MutableStateFlow("")
    val currentMonth: StateFlow<String> = _currentMonth

    // Set of selected row IDs
    private val _selectedRows = MutableStateFlow<Set<String>>(emptySet())
    val selectedRows: StateFlow<Set<String>> = _selectedRows

    // 2-step single delete inline visual state (stores current row ID in confirm delete state)
    private val _confirmDeleteRowId = MutableStateFlow<String?>(null)
    val confirmDeleteRowId: StateFlow<String?> = _confirmDeleteRowId

    // Confetti event flow
    private val _showConfettiEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showConfettiEvent: SharedFlow<Unit> = _showConfettiEvent

    // State for backup reminder
    private val _showBackupReminder = MutableStateFlow(false)
    val showBackupReminder: StateFlow<Boolean> = _showBackupReminder

    // Selected PDF Month Range
    private val _pdfRangeOption = MutableStateFlow<String?>(null) // "current", "previous", "all"
    val pdfRangeOption: StateFlow<String?> = _pdfRangeOption

    // Keep track of active screen
    private val _activeScreen = MutableStateFlow(Screen.Main)
    val activeScreen: StateFlow<Screen> = _activeScreen

    enum class Screen {
        Main,
        PdfReport
    }

    fun init(context: Context) {
        val loaded = LocalStore.loadState(context)
        _appState.value = loaded

        val today = LocalDate.now()
        val todayMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        _currentMonth.value = todayMonth

        // Run auto-add today row
        runAutoAddToday(context)

        // Check if backup is needed (more than 5 days)
        checkBackupReminder(context)
    }

    private fun updateState(newState: AppState, context: Context) {
        _appState.value = newState
        LocalStore.saveState(context, newState)
    }

    private fun updateCurrentMonthData(mData: MonthData, context: Context) {
        val updatedMap = _appState.value.monthlyData + (_currentMonth.value to mData)
        updateState(_appState.value.copy(monthlyData = updatedMap), context)
    }

    // --- Navigation ---
    fun navigatePrevMonth(context: Context) {
        val parts = _currentMonth.value.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val prevDate = LocalDate.of(year, month, 1).minusMonths(1)
        val prevMonthStr = prevDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        selectMonth(prevMonthStr, context)
    }

    fun navigateNextMonth(context: Context) {
        val parts = _currentMonth.value.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val nextDate = LocalDate.of(year, month, 1).plusMonths(1)
        val nextMonthStr = nextDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        selectMonth(nextMonthStr, context)
    }

    fun navigateToday(context: Context, onTodayScrolled: () -> Unit) {
        val today = LocalDate.now()
        val todayMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        selectMonth(todayMonth, context)
        onTodayScrolled()
    }

    private fun selectMonth(monthStr: String, context: Context) {
        _currentMonth.value = monthStr
        // Deselect all upon navigation
        _selectedRows.value = emptySet()
        _confirmDeleteRowId.value = null

        // Initialize new month map entry if nonexistent
        var mData = _appState.value.monthlyData[monthStr]
        if (mData == null) {
            mData = MonthData(rows = emptyList(), deductions = emptyList(), isClosed = false)
            val updatedMap = _appState.value.monthlyData + (monthStr to mData)
            updateState(_appState.value.copy(monthlyData = updatedMap), context)
        }
    }

    fun isTodayMonth(): Boolean {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        return _currentMonth.value == todayStr
    }

    fun isFutureMonth(): Boolean {
        val todayMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        return _currentMonth.value > todayMonth
    }

    // --- Calculations ---
    fun getMonthlyFinances(): MonthlyFinances {
        val month = _currentMonth.value
        val mData = _appState.value.monthlyData[month] ?: MonthData()
        val rate = _appState.value.rate

        var totalHours = 0.0
        for (row in mData.rows) {
            totalHours += LocalStore.calcHours(row)
        }
        val totalPay = totalHours * rate

        var totalDeductions = 0.0
        for (ded in mData.deductions) {
            val amt = ded.amount.toDoubleOrNull() ?: 0.0
            totalDeductions += amt
        }

        val gBonus = mData.globalBonus?.toDoubleOrNull() ?: 0.0
        val gDeduction = mData.globalDeduction?.toDoubleOrNull() ?: 0.0

        val netPay = totalPay + gBonus - gDeduction - totalDeductions

        return MonthlyFinances(
            totalHours = Math.round(totalHours * 100.0) / 100.0,
            totalPay = Math.round(totalPay * 100.0) / 100.0,
            totalDeductions = Math.round(totalDeductions * 100.0) / 100.0,
            netPay = Math.round(netPay * 100.0) / 100.0
        )
    }

    data class MonthlyFinances(
        val totalHours: Double,
        val totalPay: Double,
        val totalDeductions: Double,
        val netPay: Double
    )

    // --- Add Rows ---
    fun runAutoAddToday(context: Context) {
        val state = _appState.value
        val today = LocalDate.now()
        val todayISO = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val todayMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))

        val monthData = state.monthlyData[todayMonth] ?: MonthData()
        val exists = monthData.rows.any { it.date == todayISO }

        if (!exists && !monthData.isClosed) {
            val newRow = LocalStore.buildRow(todayISO, state.defaults, state.defaultOff)
            val updatedRows = (monthData.rows + newRow).sortedBy { it.date }
            val updatedMonthData = monthData.copy(rows = updatedRows)
            val updatedMap = state.monthlyData + (todayMonth to updatedMonthData)
            updateState(state.copy(monthlyData = updatedMap), context)
        }
    }

    fun addSingleRow(context: Context) {
        val mData = _appState.value.monthlyData[_currentMonth.value] ?: MonthData()
        if (mData.isClosed) return

        val parts = _currentMonth.value.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val baseDate = LocalDate.of(year, month, 1)

        val lastDate = if (mData.rows.isEmpty()) {
            baseDate.minusDays(1)
        } else {
            try {
                LocalDate.parse(mData.rows.last().date)
            } catch (e: Exception) {
                baseDate.minusDays(1)
            }
        }

        val nextDate = lastDate.plusDays(1)
        val nextDateStr = nextDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        if (!nextDateStr.startsWith(_currentMonth.value)) {
            return // Month limit reached
        }

        val newRow = LocalStore.buildRow(nextDateStr, _appState.value.defaults, _appState.value.defaultOff)
        val updatedRows = (mData.rows + newRow).sortedBy { it.date }
        updateCurrentMonthData(mData.copy(rows = updatedRows), context)
    }

    fun handleBulkAdd(type: String, context: Context) {
        val mData = _appState.value.monthlyData[_currentMonth.value] ?: MonthData()
        if (mData.isClosed) return

        val parts = _currentMonth.value.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val baseDate = LocalDate.of(year, month, 1)

        val lastDate = if (mData.rows.isEmpty()) {
            baseDate.minusDays(1)
        } else {
            try {
                LocalDate.parse(mData.rows.last().date)
            } catch (e: Exception) {
                baseDate.minusDays(1)
            }
        }

        val count = when (type) {
            "full" -> {
                val lastDayVal = baseDate.lengthOfMonth()
                val lastDayDate = LocalDate.of(year, month, lastDayVal)
                val diff = java.time.temporal.ChronoUnit.DAYS.between(lastDate, lastDayDate).toInt()
                if (diff <= 0) 0 else diff
            }
            else -> type.toIntOrNull() ?: 0
        }

        val newRows = mutableListOf<WorkRow>()
        var currentLast = lastDate

        for (i in 0 until count) {
            val nextDate = currentLast.plusDays(1)
            val nextDateStr = nextDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            if (!nextDateStr.startsWith(_currentMonth.value)) {
                break
            }
            val newRow = LocalStore.buildRow(nextDateStr, _appState.value.defaults, _appState.value.defaultOff)
            newRows.add(newRow)
            currentLast = nextDate
        }

        if (newRows.isNotEmpty()) {
            val updatedRows = (mData.rows + newRows).sortedBy { it.date }
            updateCurrentMonthData(mData.copy(rows = updatedRows), context)
        }
    }

    // --- Editing Row Fields ---
    fun handleInputChange(rowId: String, fieldName: String, newValue: String, context: Context) {
        val mData = _appState.value.monthlyData[_currentMonth.value] ?: MonthData()
        if (mData.isClosed) return

        val updatedRows = mData.rows.map { row ->
            if (row.id == rowId) {
                when (fieldName) {
                    "start" -> row.copy(start = newValue)
                    "end" -> row.copy(end = newValue)
                    "break" -> row.copy(breakDuration = newValue)
                    "note" -> row.copy(note = newValue)
                    "date" -> {
                        try {
                            val parsed = LocalDate.parse(newValue)
                            val weekday = LocalStore.getDayOfWeekIndex(parsed)
                            val name = LocalStore.arabicDays[weekday]
                            if (weekday == 0) {
                                // Sunday is off and cleared
                                row.copy(date = newValue, dayName = name, start = "", end = "", breakDuration = "", off = true)
                            } else {
                                row.copy(date = newValue, dayName = name)
                            }
                        } catch (e: Exception) {
                            row.copy(date = newValue)
                        }
                    }
                    else -> row
                }
            } else {
                row
            }
        }.sortedBy { it.date }

        updateCurrentMonthData(mData.copy(rows = updatedRows), context)
    }

    fun handleOffToggled(rowId: String, off: Boolean, context: Context) {
        val mData = _appState.value.monthlyData[_currentMonth.value] ?: MonthData()
        if (mData.isClosed) return

        val updatedRows = mData.rows.map { row ->
            if (row.id == rowId) {
                val localDate = try { LocalDate.parse(row.date) } catch(e: Exception) { null }
                val weekday = localDate?.let { LocalStore.getDayOfWeekIndex(it) } ?: -1
                if (weekday == 0) {
                    // Sunday is always off
                    row.copy(off = true, start = "", end = "", breakDuration = "")
                } else {
                    if (off) {
                        row.copy(off = true, start = "", end = "", breakDuration = "")
                    } else {
                        // Restore defaults from state when unchecking off
                        val def = _appState.value.defaults
                        row.copy(off = false, start = def.start, end = def.end, breakDuration = def.brk)
                    }
                }
            } else {
                row
            }
        }
        updateCurrentMonthData(mData.copy(rows = updatedRows), context)
    }

    // --- Row Selection & Bulk Actions ---
    fun toggleSelectRow(rowId: String) {
        val current = _selectedRows.value
        if (current.contains(rowId)) {
            _selectedRows.value = current - rowId
        } else {
            _selectedRows.value = current + rowId
        }
    }

    fun selectAllRows() {
        val mData = _appState.value.monthlyData[_currentMonth.value] ?: MonthData()
        _selectedRows.value = mData.rows.map { it.id }.toSet()
    }

    fun deselectAllRows() {
        _selectedRows.value = emptySet()
    }

    fun bulkMarkOff(context: Context) {
        val mData = _appState.value.monthlyData[_currentMonth.value] ?: MonthData()
        if (mData.isClosed) return

        val ids = _selectedRows.value
        val updatedRows = mData.rows.map { row ->
            if (ids.contains(row.id)) {
                row.copy(off = true, start = "", end = "", breakDuration = "")
            } else {
                row
            }
        }
        updateCurrentMonthData(mData.copy(rows = updatedRows), context)
        deselectAllRows()
    }

    fun bulkDelete(context: Context) {
        val mData = _appState.value.monthlyData[_currentMonth.value] ?: MonthData()
        if (mData.isClosed) return

        val ids = _selectedRows.value
        val updatedRows = mData.rows.filterNot { ids.contains(it.id) }
        updateCurrentMonthData(mData.copy(rows = updatedRows), context)
        deselectAllRows()
    }

    // Special Bulk Action: When exactly 1 row is selected
    fun triggerNewCompanyStart(context: Context, onProfileDrawerOpenNeeded: () -> Unit) {
        val mData = _appState.value.monthlyData[_currentMonth.value] ?: MonthData()
        if (mData.isClosed) return

        val ids = _selectedRows.value
        if (ids.size != 1) return

        val targetId = ids.first()
        val targetRow = mData.rows.find { it.id == targetId } ?: return

        // 1. Mark isNewCompanyStart
        val updatedRows = mData.rows.map { row ->
            if (row.id == targetId) {
                row.copy(isNewCompanyStart = true)
            } else {
                row
            }
        }
        updateCurrentMonthData(mData.copy(rows = updatedRows), context)

        // 2. Clear current profile companyName & set startDate to this date
        val updatedProfile = _appState.value.profile.copy(
            startDate = targetRow.date,
            companyName = "" // So user can input new company name
        )
        updateState(_appState.value.copy(profile = updatedProfile), context)

        // 3. Clear selections & request drawer open
        deselectAllRows()
        onProfileDrawerOpenNeeded()
    }

    // --- Inline Single Delete ---
    fun requestDeleteRow(rowId: String?) {
        _confirmDeleteRowId.value = rowId
    }

    fun confirmDeleteRow(rowId: String, context: Context) {
        val mData = _appState.value.monthlyData[_currentMonth.value] ?: MonthData()
        if (mData.isClosed) return

        val updatedRows = mData.rows.filterNot { it.id == rowId }
        updateCurrentMonthData(mData.copy(rows = updatedRows), context)
        _confirmDeleteRowId.value = null
    }

    // --- Configurations & Settings Panels ---
    fun toggleMonthLock(context: Context) {
        val month = _currentMonth.value
        val mData = _appState.value.monthlyData[month] ?: MonthData()
        val updatedClose = !mData.isClosed
        updateCurrentMonthData(mData.copy(isClosed = updatedClose), context)
    }

    fun toggleDefaultOff(context: Context) {
        val toggle = !_appState.value.defaultOff
        updateState(_appState.value.copy(defaultOff = toggle), context)
    }

    fun updateDefaults(start: String, end: String, brk: String, context: Context) {
        val updatedDef = Defaults(start = start, end = end, brk = brk)
        updateState(_appState.value.copy(defaults = updatedDef), context)
    }

    fun updateRate(rate: Double, context: Context) {
        updateState(_appState.value.copy(rate = rate), context)
    }

    fun updateGlobalMonthlyValues(bonus: String, deduction: String, context: Context) {
        val mData = _appState.value.monthlyData[_currentMonth.value] ?: MonthData()
        val updated = mData.copy(
            globalBonus = bonus.takeIf { it.isNotEmpty() },
            globalDeduction = deduction.takeIf { it.isNotEmpty() }
        )
        updateCurrentMonthData(updated, context)
    }

    fun updateProfile(name: String, companyId: String, companyName: String, startDate: String, context: Context) {
        val updatedProfile = Profile(
            name = name,
            companyId = companyId,
            companyName = companyName,
            startDate = startDate
        )
        updateState(_appState.value.copy(profile = updatedProfile), context)
    }

    // --- Deductions / Loans Section (Per Month) ---
    fun addDeduction(context: Context) {
        val mData = _appState.value.monthlyData[_currentMonth.value] ?: MonthData()
        if (mData.isClosed) return

        val newDeduction = DeductionItem(
            id = UUID.randomUUID().toString(),
            amount = "",
            note = ""
        )
        val updatedDeds = mData.deductions + newDeduction
        updateCurrentMonthData(mData.copy(deductions = updatedDeds), context)
    }

    fun updateDeduction(dedId: String, amount: String, note: String, context: Context) {
        val mData = _appState.value.monthlyData[_currentMonth.value] ?: MonthData()
        if (mData.isClosed) return

        val updatedDeds = mData.deductions.map { ded ->
            if (ded.id == dedId) {
                ded.copy(amount = amount, note = note)
            } else {
                ded
            }
        }
        updateCurrentMonthData(mData.copy(deductions = updatedDeds), context)
    }

    fun deleteDeduction(dedId: String, context: Context) {
        val mData = _appState.value.monthlyData[_currentMonth.value] ?: MonthData()
        if (mData.isClosed) return

        val updatedDeds = mData.deductions.filterNot { ded -> ded.id == dedId }
        updateCurrentMonthData(mData.copy(deductions = updatedDeds), context)
    }

    // --- Tasks System (Kanban) ---
    fun addTask(text: String, context: Context) {
        if (text.isBlank()) return
        val newTask = TaskItem(
            id = UUID.randomUUID().toString(),
            text = text,
            hasPrice = false,
            price = "",
            status = "todo"
        )
        val updatedNotes = _appState.value.notes + newTask
        updateState(_appState.value.copy(notes = updatedNotes), context)
    }

    fun toggleTaskPriceEnabled(taskId: String, enabled: Boolean, context: Context) {
        val updatedNotes = _appState.value.notes.map { note ->
            if (note.id == taskId) {
                note.copy(hasPrice = enabled, price = if (enabled) note.price else "")
            } else {
                note
            }
        }
        updateState(_appState.value.copy(notes = updatedNotes), context)
    }

    fun updateTaskPrice(taskId: String, priceStr: String, context: Context) {
        val updatedNotes = _appState.value.notes.map { note ->
            if (note.id == taskId) {
                note.copy(price = priceStr)
            } else {
                note
            }
        }
        updateState(_appState.value.copy(notes = updatedNotes), context)
    }

    fun updateTaskStatus(taskId: String, newStatus: String, context: Context) {
        val oldTask = _appState.value.notes.find { it.id == taskId }
        val updatedNotes = _appState.value.notes.map { note ->
            if (note.id == taskId) {
                note.copy(status = newStatus)
            } else {
                note
            }
        }
        updateState(_appState.value.copy(notes = updatedNotes), context)

        // Confetti trigger if moving from todo -> done
        if (oldTask?.status == "todo" && newStatus == "done") {
            _showConfettiEvent.tryEmit(Unit)
        }
    }

    fun reorderTasks(newOrderedTasks: List<TaskItem>, context: Context) {
        // Re-construct global list prioritizing the drag reorder sequence
        updateState(_appState.value.copy(notes = newOrderedTasks), context)
    }

    fun deleteTask(taskId: String, context: Context) {
        val updatedNotes = _appState.value.notes.filterNot { it.id == taskId }
        updateState(_appState.value.copy(notes = updatedNotes), context)
    }

    // --- Clean Wipe All Data ---
    fun cleanWipeData(context: Context) {
        val fresh = LocalStore.clearAllData(context)
        _appState.value = fresh
        _selectedRows.value = emptySet()
        _confirmDeleteRowId.value = null

        val today = LocalDate.now()
        val todayMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        _currentMonth.value = todayMonth
    }

    // --- JSON Export / Import Backup Flows ---
    fun getExportJson(): String {
        return LocalStore.exportStateToJson(_appState.value)
    }

    fun updateBackupTimestamp(context: Context) {
        val current = System.currentTimeMillis()
        LocalStore.saveLastBackupTime(context, current)
        _showBackupReminder.value = false
    }

    fun importBackupJson(jsonString: String, context: Context): Boolean {
        val imported = LocalStore.importStateFromJson(jsonString)
        return if (imported != null) {
            updateState(imported, context)
            val todayMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
            _currentMonth.value = todayMonth
            _selectedRows.value = emptySet()
            _confirmDeleteRowId.value = null
            true
        } else {
            false
        }
    }

    private fun checkBackupReminder(context: Context) {
        val lastTime = LocalStore.getLastBackupTime(context)
        if (lastTime == 0L) {
            // First run, set first backup time so we won't show it immediately
            LocalStore.saveLastBackupTime(context, System.currentTimeMillis())
        } else {
            val fiveDaysMs = 5 * 24 * 60 * 60 * 1000L
            if (System.currentTimeMillis() - lastTime > fiveDaysMs) {
                _showBackupReminder.value = true
            }
        }
    }

    fun dismissBackupReminder(context: Context) {
        _showBackupReminder.value = false
        // Postpone for another 5 days
        LocalStore.saveLastBackupTime(context, System.currentTimeMillis())
    }

    // --- PDF Print Option System ---
    fun selectPdfRange(rangeOption: String?) {
        _pdfRangeOption.value = rangeOption
        if (rangeOption != null) {
            _activeScreen.value = Screen.PdfReport
        } else {
            _activeScreen.value = Screen.Main
        }
    }

    fun closePdfScreen() {
        _pdfRangeOption.value = null
        _activeScreen.value = Screen.Main
    }

    fun getPdfRows(): List<WorkRow> {
        val currentMonthISO = _currentMonth.value
        val today = LocalDate.now()
        val state = _appState.value

        return when (_pdfRangeOption.value) {
            "current" -> {
                val todayMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                val mData = state.monthlyData[todayMonth] ?: MonthData()
                mData.rows
            }
            "previous" -> {
                val prevMonth = today.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"))
                val mData = state.monthlyData[prevMonth] ?: MonthData()
                mData.rows
            }
            "all" -> {
                // Return all rows of all months combined, sorted by date asc
                state.monthlyData.values.flatMap { it.rows }.sortedBy { it.date }
            }
            else -> emptyList()
        }
    }

    fun getPdfReportTitle(): String {
        val user = _appState.value.profile.name.trim()
        val titleUser = if (user.isNotEmpty()) user else "Work Log Report"
        val option = _pdfRangeOption.value
        val today = LocalDate.now()

        return when (option) {
            "current" -> {
                val mStr = today.format(DateTimeFormatter.ofPattern("MM-yyyy"))
                "$titleUser - Work Logs $mStr"
            }
            "previous" -> {
                val mStr = today.minusMonths(1).format(DateTimeFormatter.ofPattern("MM-yyyy"))
                "$titleUser - Work Logs $mStr"
            }
            else -> {
                "$titleUser - Full History"
            }
        }
    }

    fun getPdfPeriodLabel(): String {
        val today = LocalDate.now()
        val option = _pdfRangeOption.value
        return when (option) {
            "current" -> {
                today.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            }
            "previous" -> {
                today.minusMonths(1).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            }
            "all" -> {
                "Full History (All)"
            }
            else -> "N/A"
        }
    }
}
