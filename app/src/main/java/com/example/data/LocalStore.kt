package com.example.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

object LocalStore {
    private const val PREFS_NAME = "workdevforce_prefs"
    private const val DATA_KEY = "worklog_app_data_monthly_v2"
    private const val BACKUP_TIME_KEY = "worklog_last_backup_date"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val appStateAdapter = moshi.adapter(AppState::class.java)

    val arabicDays = listOf(
        "الأحد",       // Sunday (0)
        "الاثنين",     // Monday (1)
        "الثلاثاء",    // Tuesday (2)
        "الأربعاء",   // Wednesday (3)
        "الخميس",     // Thursday (4)
        "الجمعة",      // Friday (5)
        "السبت"       // Saturday (6)
    )

    fun getDayOfWeekIndex(localDate: LocalDate): Int {
        return when (localDate.dayOfWeek) {
            java.time.DayOfWeek.SUNDAY -> 0
            java.time.DayOfWeek.MONDAY -> 1
            java.time.DayOfWeek.TUESDAY -> 2
            java.time.DayOfWeek.WEDNESDAY -> 3
            java.time.DayOfWeek.THURSDAY -> 4
            java.time.DayOfWeek.FRIDAY -> 5
            java.time.DayOfWeek.SATURDAY -> 6
        }
    }

    fun parseTimeToMinutes(timeStr: String): Int? {
        val trimmed = timeStr.trim()
        val regex = Regex("^(\\d{1,2}):(\\d{2})$")
        val matchResult = regex.find(trimmed) ?: return null
        val hours = matchResult.groupValues[1].toIntOrNull() ?: return null
        val minutes = matchResult.groupValues[2].toIntOrNull() ?: return null
        return hours * 60 + minutes
    }

    fun calcHours(row: WorkRow): Double {
        val localDate = try {
            LocalDate.parse(row.date)
        } catch (e: Exception) {
            return 0.0
        }
        val weekday = getDayOfWeekIndex(localDate)
        if (row.off || weekday == 0) {
            return 0.0
        }

        val startMinutes = parseTimeToMinutes(row.start) ?: return 0.0
        val endMinutes = parseTimeToMinutes(row.end) ?: return 0.0

        var diff = endMinutes - startMinutes
        if (diff <= 0) {
            diff += 1440 // Supports overnight shifts (add 24 hours)
        }

        val cleanBreakStr = row.breakDuration.replace("-", "").trim()
        val breakMinutes = cleanBreakStr.toIntOrNull() ?: 0

        val hours = (diff - breakMinutes) / 60.0
        return if (hours > 0) {
            Math.round(hours * 100.0) / 100.0
        } else {
            0.0
        }
    }

    fun buildRow(dateISO: String, defaults: Defaults, isDefaultOff: Boolean): WorkRow {
        val localDate = LocalDate.parse(dateISO)
        val weekday = getDayOfWeekIndex(localDate)
        val isOff = isDefaultOff || (weekday == 0)

        return WorkRow(
            id = UUID.randomUUID().toString(),
            date = dateISO,
            dayName = arabicDays[weekday],
            start = if (isOff) "" else (defaults.start.takeIf { it.isNotEmpty() } ?: "06:30"),
            end = if (isOff) "" else (defaults.end.takeIf { it.isNotEmpty() } ?: "15:30"),
            breakDuration = if (isOff) "" else (defaults.brk.takeIf { it.isNotEmpty() } ?: "-30"),
            note = "",
            off = isOff
        )
    }

    fun loadState(context: Context): AppState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(DATA_KEY, null)
        if (jsonString.isNullOrBlank()) {
            return buildInitialState(context)
        }

        return try {
            val rawState = appStateAdapter.fromJson(jsonString) ?: buildInitialState(context)
            migrateState(rawState)
        } catch (e: Exception) {
            Log.e("LocalStore", "Error deserializing state", e)
            buildInitialState(context)
        }
    }

    fun saveState(context: Context, state: AppState) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val jsonString = appStateAdapter.toJson(state)
            prefs.edit().putString(DATA_KEY, jsonString).apply()
        } catch (e: Exception) {
            Log.e("LocalStore", "Error serializing state", e)
        }
    }

    fun getLastBackupTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(BACKUP_TIME_KEY, 0L)
    }

    fun saveLastBackupTime(context: Context, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(BACKUP_TIME_KEY, timestamp).apply()
    }

    fun clearAllData(context: Context): AppState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(DATA_KEY).apply()
        return buildInitialState(context)
    }

    private fun buildInitialState(context: Context): AppState {
        val today = LocalDate.now()
        val todayMonthString = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val todayISO = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val initialDefaults = Defaults()
        val initialRow = buildRow(todayISO, initialDefaults, false)

        val initialMonthData = MonthData(
            rows = listOf(initialRow),
            deductions = emptyList(),
            isClosed = false
        )

        val state = AppState(
            monthlyData = mapOf(todayMonthString to initialMonthData),
            rate = 16.15,
            notes = emptyList(),
            defaults = initialDefaults,
            profile = Profile(),
            defaultOff = false
        )
        // Auto-save the initial state
        saveState(context, state)
        return state
    }

    private fun migrateState(state: AppState): AppState {
        var updatedMonthlyData = state.monthlyData.toMutableMap()
        var changed = false

        for ((month, mData) in state.monthlyData) {
            var updatedRows = mData.rows.toMutableList()
            var rowsChanged = false

            // Backward Compatibility: ensure every months' task or legacy issues are resolved
            // Old format to new deductions list conversion:
            var updatedDeductions = mData.deductions.toMutableList()
            var mDataChanged = false

            if (mData.minusAmount != null && updatedDeductions.isEmpty()) {
                updatedDeductions.add(
                    DeductionItem(
                        id = UUID.randomUUID().toString(),
                        amount = mData.minusAmount.toString(),
                        note = mData.minusNote ?: ""
                    )
                )
                mDataChanged = true
            }

            val rawMData = if (mDataChanged) {
                mData.copy(
                    deductions = updatedDeductions,
                    minusAmount = null,
                    minusNote = null
                )
            } else {
                mData
            }

            if (mDataChanged || rawMData.deductions != mData.deductions) {
                updatedMonthlyData[month] = rawMData
                changed = true
            }
        }

        // Backward Compatibility for notes array mapping
        val updatedNotes = state.notes.map { noteItem ->
            // In case of conversion or null checking
            noteItem
        }

        val resultState = if (changed || updatedNotes != state.notes) {
            state.copy(
                monthlyData = updatedMonthlyData,
                notes = updatedNotes
            )
        } else {
            state
        }
        return resultState
    }

    fun exportStateToJson(state: AppState): String {
        return appStateAdapter.toJson(state)
    }

    fun importStateFromJson(jsonString: String): AppState? {
        return try {
            val state = appStateAdapter.fromJson(jsonString)
            if (state?.monthlyData != null) { // Simple validation: must contain monthlyData
                state
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("LocalStore", "Failed to deserialize imported JSON", e)
            null
        }
    }
}
