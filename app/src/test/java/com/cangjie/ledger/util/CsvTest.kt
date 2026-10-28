package com.cangjie.ledger.util

import java.io.StringReader
import java.io.StringWriter
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTest {
    @Test
    fun handlesQuotesCommasAndNewlines() {
        val source = "date,note\r\n2026-10-28,\"午饭, \"\"加蛋\"\"\"\r\n"
        val rows = Csv.readRows(StringReader(source))
        assertEquals(listOf("date", "note"), rows[0].values)
        assertEquals(listOf("2026-10-28", "午饭, \"加蛋\""), rows[1].values)
    }

    @Test
    fun writesEscapedRows() {
        val writer = StringWriter()
        Csv.writeRow(writer, listOf("a", "b,c", "d\"e"))
        assertEquals("a,\"b,c\",\"d\"\"e\"\r\n", writer.toString())
    }
}
