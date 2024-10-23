//package io.mfj.textricator.extractor.table
//
//import io.mfj.textricator.table.TableParser
//import io.mfj.textricator.table.config.TableParseConfig
//import io.mfj.textricator.text.Page
//import io.mfj.textricator.text.Text
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Assertions.assertTrue
//import org.junit.jupiter.api.Test
//
//class TableParserTest {
//    @Test
//    fun parsesSinglePageWithSingleRow() {
//        val config = TableParseConfig(/* initialize with appropriate values */)
//        val parser = TableParser(config)
//        val page = Page(1, listOf(Text(0.0f, 0.0f, 1.0f, 1.0f, "content")))
//        val pages = sequenceOf(page)
//
//        val records = parser.parse(pages).toList()
//
//        assertEquals(1, records.size)
//        assertEquals("content", records[0].values["columnName"]?.toString())
//    }
//
//    @Test
//    fun parsesMultiplePagesWithMultipleRows() {
//        val config = TableParseConfig(/* initialize with appropriate values */)
//        val parser = TableParser(config)
//        val page1 = Page(1, listOf(Text(0.0f, 0.0f, 1.0f, 1.0f, "content1")))
//        val page2 = Page(2, listOf(Text(0.0f, 0.0f, 1.0f, 1.0f, "content2")))
//        val pages = sequenceOf(page1, page2)
//
//        val records = parser.parse(pages).toList()
//
//        assertEquals(2, records.size)
//        assertEquals("content1", records[0].values["columnName"]?.toString())
//        assertEquals("content2", records[1].values["columnName"]?.toString())
//    }
//
//    @Test
//    fun handlesEmptyPagesSequence() {
//        val config = TableParseConfig(/* initialize with appropriate values */)
//        val parser = TableParser(config)
//        val pages = emptySequence<Page>()
//
//        val records = parser.parse(pages).toList()
//
//        assertTrue(records.isEmpty())
//    }
//
//    @Test
//    fun handlesTextOutsideConfiguredColumns() {
//        val config = TableParseConfig(/* initialize with appropriate values */)
//        val parser = TableParser(config)
//        val page = Page(1, listOf(Text(10.0f, 0.0f, 11.0f, 1.0f, "content")))
//        val pages = sequenceOf(page)
//
//        val records = parser.parse(pages).toList()
//
//        assertTrue(records.isEmpty())
//    }
//
//    @Test
//    fun handlesTextOutsideConfiguredRows() {
//        val config = TableParseConfig(/* initialize with appropriate values */)
//        val parser = TableParser(config)
//        val page = Page(1, listOf(Text(0.0f, 10.0f, 1.0f, 11.0f, "content")))
//        val pages = sequenceOf(page)
//
//        val records = parser.parse(pages).toList()
//
//        assertTrue(records.isEmpty())
//    }
//
//    @Test
//    fun mergesTextWithinMaxRowDistance() {
//        val config = TableParseConfig(/* initialize with appropriate values */)
//        val parser = TableParser(config)
//        val page = Page(1, listOf(
//            Text(0.0f, 0.0f, 1.0f, 1.0f, "content1"),
//            Text(0.0f, 0.5f, 1.0f, 1.5f, "content2")
//        ))
//        val pages = sequenceOf(page)
//
//        val records = parser.parse(pages).toList()
//
//        assertEquals(1, records.size)
//        assertEquals("content1 content2", records[0].values["columnName"]?.toString())
//    }
//}