package com.cangjie.ledger.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)
    private val shortMonthFormatter = DateTimeFormatter.ofPattern("M月", Locale.CHINA)
    private val dateFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)

    fun todayEpochDay(): Long = LocalDate.now(zone).toEpochDay()
    fun fromEpochDay(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)
    fun toEpochDay(date: LocalDate): Long = date.toEpochDay()
    fun monthKey(date: LocalDate = LocalDate.now(zone)): String = YearMonth.from(date).toString()
    fun monthKey(epochDay: Long): String = YearMonth.from(fromEpochDay(epochDay)).toString()
    fun monthLabel(monthKey: String): String = YearMonth.parse(monthKey).format(monthFormatter)
    fun shortMonthLabel(monthKey: String): String = YearMonth.parse(monthKey).format(shortMonthFormatter)
    fun dateLabel(epochDay: Long): String = fromEpochDay(epochDay).format(dateFormatter)
    fun weekdayLabel(epochDay: Long): String {
        val day = fromEpochDay(epochDay).dayOfWeek
        return when (day) {
            DayOfWeek.MONDAY -> "周一"
            DayOfWeek.TUESDAY -> "周二"
            DayOfWeek.WEDNESDAY -> "周三"
            DayOfWeek.THURSDAY -> "周四"
            DayOfWeek.FRIDAY -> "周五"
            DayOfWeek.SATURDAY -> "周六"
            DayOfWeek.SUNDAY -> "周日"
        }
    }

    fun shiftMonth(monthKey: String, delta: Long): String =
        YearMonth.parse(monthKey).plusMonths(delta).toString()

    fun lastNMonths(monthKey: String, count: Int): List<String> {
        val end = YearMonth.parse(monthKey)
        return (count - 1 downTo 0).map { end.minusMonths(it.toLong()).toString() }
    }

    fun epochDayInMonth(monthKey: String, dayOfMonth: Int): Long {
        val month = YearMonth.parse(monthKey)
        val safeDay = dayOfMonth.coerceIn(1, month.lengthOfMonth())
        return month.atDay(safeDay).toEpochDay()
    }

    fun timestampLabel(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(zone).format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA),
        )

    fun currentYearMonth(): YearMonth = YearMonth.now(zone)
}

