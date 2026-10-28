package com.cangjie.ledger.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test
    fun evaluatesArithmeticInOrder() {
        assertEquals(1_450L, Money.expressionToCents("12.5+8*0.5-2"))
    }

    @Test
    fun respectsParentheses() {
        assertEquals(1_400L, Money.expressionToCents("(12+16)/2"))
    }

    @Test
    fun roundsToCents() {
        assertEquals(333L, Money.expressionToCents("10/3"))
    }
}

