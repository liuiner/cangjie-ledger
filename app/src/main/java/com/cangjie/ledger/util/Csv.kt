package com.cangjie.ledger.util

import java.io.BufferedReader
import java.io.Reader
import java.io.Writer

object Csv {
    fun escape(value: String): String {
        if (value.none { it == ',' || it == '"' || it == '\n' || it == '\r' }) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }

    fun writeRow(writer: Writer, values: List<String>) {
        writer.append(values.joinToString(",") { escape(it) })
        writer.append("\r\n")
    }

    fun readRows(reader: Reader): List<CsvRow> {
        val rows = mutableListOf<CsvRow>()
        val buffered = reader.buffered()
        var physicalLine = 0
        while (true) {
            val parsed = readRecord(buffered, physicalLine)
            if (parsed == null) break
            physicalLine = parsed.lineNumber
            if (parsed.values.any { it.isNotBlank() }) rows += parsed
        }
        return rows
    }

    private fun readRecord(reader: BufferedReader, startLine: Int): CsvRow? {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var line = startLine

        while (true) {
            val read = reader.read()
            if (read == -1) {
                if (inQuotes || current.isNotEmpty() || values.isNotEmpty()) {
                    values += current.toString()
                    return CsvRow(values, line + 1)
                }
                return null
            }
            val c = read.toChar()
            if (inQuotes) {
                if (c == '"') {
                    reader.mark(1)
                    val next = reader.read()
                    if (next == '"'.code) {
                        current.append('"')
                    } else {
                        inQuotes = false
                        if (next != -1) reader.reset()
                    }
                } else {
                    current.append(c)
                    if (c == '\n') line++
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        values += current.toString()
                        current.clear()
                    }
                    '\n' -> {
                        values += current.toString().removeSuffix("\r")
                        return CsvRow(values, line + 1)
                    }
                    '\r' -> Unit
                    else -> current.append(c)
                }
            }
        }
    }
}

data class CsvRow(
    val values: List<String>,
    val lineNumber: Int,
)

