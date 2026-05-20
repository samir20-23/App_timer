package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Defaults(
    val start: String = "06:30",
    val end: String = "15:30",
    val brk: String = "-30"
)

@JsonClass(generateAdapter = true)
data class Profile(
    val name: String = "",
    val companyId: String = "",
    val companyName: String = "",
    val startDate: String = ""
)

@JsonClass(generateAdapter = true)
data class TaskItem(
    val id: String,
    val text: String,
    val hasPrice: Boolean = false,
    val price: String = "",
    val status: String = "todo" // "todo" or "done"
)

@JsonClass(generateAdapter = true)
data class DeductionItem(
    val id: String,
    val amount: String,
    val note: String
)

@JsonClass(generateAdapter = true)
data class WorkRow(
    val id: String,
    val date: String, // "yyyy-MM-dd"
    val dayName: String, // Arabic day name
    val start: String,
    val end: String,
    @Json(name = "break") val breakDuration: String, // Maps to "break" in legacy JSON structure
    val note: String = "",
    val off: Boolean = false,
    val isNewCompanyStart: Boolean = false
)

@JsonClass(generateAdapter = true)
data class MonthData(
    val rows: List<WorkRow> = emptyList(),
    val deductions: List<DeductionItem> = emptyList(),
    val isClosed: Boolean = false,
    val globalBonus: String? = null,
    val globalDeduction: String? = null,
    // Legacy support flags for backward compatibility checks
    val minusAmount: Double? = null,
    val minusNote: String? = null
)

@JsonClass(generateAdapter = true)
data class AppState(
    val monthlyData: Map<String, MonthData> = emptyMap(),
    val rate: Double = 16.15,
    val notes: List<TaskItem> = emptyList(),
    val defaults: Defaults = Defaults(),
    val profile: Profile = Profile(),
    val defaultOff: Boolean = false
)
