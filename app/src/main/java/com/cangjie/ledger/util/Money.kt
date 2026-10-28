package com.cangjie.ledger.util

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale

object Money {
    private val mathContext = MathContext(16, RoundingMode.HALF_UP)

    fun parseExpression(input: String): BigDecimal {
        val parser = ExpressionParser(input)
        val result = parser.parse()
        require(result >= BigDecimal.ZERO) { "金额不能为负数" }
        require(result <= BigDecimal("999999999999.99")) { "金额过大" }
        return result
    }

    fun expressionToCents(input: String): Long {
        val value = parseExpression(input)
        require(value > BigDecimal.ZERO) { "金额必须大于 0" }
        return value.multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun format(cents: Long, withSymbol: Boolean = true): String {
        val value = BigDecimal(cents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        val body = String.format(Locale.CHINA, "%,.2f", value)
        return if (withSymbol) "¥$body" else body
    }

    fun plain(cents: Long): String =
        BigDecimal(cents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

    private class ExpressionParser(private val raw: String) {
        private val input = raw
            .replace('×', '*')
            .replace('÷', '/')
            .replace('（', '(')
            .replace('）', ')')
            .replace("，", "")
            .replace(",", "")
            .replace(" ", "")
        private var index = 0

        fun parse(): BigDecimal {
            require(input.isNotBlank()) { "请输入金额" }
            val result = parseExpression()
            require(index == input.length) { "算式格式不正确" }
            return result
        }

        private fun parseExpression(): BigDecimal {
            var value = parseTerm()
            while (peek() == '+' || peek() == '-') {
                val op = next()
                val rhs = parseTerm()
                value = if (op == '+') value.add(rhs, mathContext) else value.subtract(rhs, mathContext)
            }
            return value
        }

        private fun parseTerm(): BigDecimal {
            var value = parseFactor()
            while (peek() == '*' || peek() == '/') {
                val op = next()
                val rhs = parseFactor()
                if (op == '/') {
                    require(rhs.compareTo(BigDecimal.ZERO) != 0) { "不能除以 0" }
                    value = value.divide(rhs, mathContext)
                } else {
                    value = value.multiply(rhs, mathContext)
                }
            }
            return value
        }

        private fun parseFactor(): BigDecimal {
            if (peek() == '+') {
                next()
                return parseFactor()
            }
            if (peek() == '-') {
                next()
                return parseFactor().negate()
            }
            if (peek() == '(') {
                next()
                val value = parseExpression()
                require(next() == ')') { "缺少右括号" }
                return value
            }
            return parseNumber()
        }

        private fun parseNumber(): BigDecimal {
            val start = index
            var dots = 0
            while (index < input.length) {
                val c = input[index]
                if (c.isDigit()) {
                    index++
                } else if (c == '.' && dots == 0) {
                    dots++
                    index++
                } else {
                    break
                }
            }
            require(index > start && input.substring(start, index) != ".") { "请输入有效数字" }
            return input.substring(start, index).toBigDecimal()
        }

        private fun peek(): Char? = input.getOrNull(index)
        private fun next(): Char = input[index++]
    }
}

