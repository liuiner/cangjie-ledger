package com.cangjie.ledger.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtilsTest {
    @Test
    fun clampsMonthlyRuleToLastDay() {
        assertEquals(
            LocalDate.of(2026, 2, 28).toEpochDay(),
            DateUtils.epochDayInMonth("2026-02", 31),
        )
    }

    @Test
    fun handlesFebruaryInLeapYear() {
        assertEquals(
            LocalDate.of(2028, 2, 29).toEpochDay(),
            DateUtils.epochDayInMonth("2028-02", 30),
        )
    }
}

