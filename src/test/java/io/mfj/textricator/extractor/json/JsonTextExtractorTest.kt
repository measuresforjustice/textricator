package io.mfj.textricator.extractor.json

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class JsonTextExtractorTest {

    @Test
    fun getPageCount_returnsCorrectPageCount() {
        val json = """
            [
                {"pageNumber": 1,  
                 "font": "Arial",
                 "content": "Text 1"},
                {"pageNumber": 2, 
                 "font": "Arial",
                 "content": "Text 2"},
                {"pageNumber": 2, 
                 "font": "Arial",
                 "content": "Text 3"}
            ]
        """.trimIndent()
        val inputStream = ByteArrayInputStream(json.toByteArray())
        val extractor = JsonTextExtractor(inputStream)

        assertEquals(2, extractor.getPageCount())
    }

    @Test
    fun extract_returnsTextsForGivenPage() {
        val json = """
            [
                {"pageNumber": 1, 
                 "font": "Arial",
                 "content": "Text 1"},
                {"pageNumber": 2, 
                 "font": "Arial",
                 "content": "Text 2"},
                {"pageNumber": 2, 
                 "font": "Arial",
                 "content": "Text 3"}
            ]
        """.trimIndent()
        val inputStream = ByteArrayInputStream(json.toByteArray())
        val extractor = JsonTextExtractor(inputStream)

        val texts = extractor.extract(2)
        assertEquals(2, texts.size)
        assertEquals("Text 2", texts[0].content)
        assertEquals("Text 3", texts[1].content)
    }

    @Test
    fun extract_returnsEmptyListForNonExistentPage() {
        val json = """
            [
                {"pageNumber": 1, 
                "font": "Arial",
                "content": "Text 1"},
                {"pageNumber": 2, 
                 "font": "Times",
                 "content": "Text 2"}
            ]
        """.trimIndent()
        val inputStream = ByteArrayInputStream(json.toByteArray())
        val extractor = JsonTextExtractor(inputStream)

        val texts = extractor.extract(3)
        assertTrue(texts.isEmpty())
    }

    @Test
    fun getPageCount_returnsZeroForEmptyInput() {
        val json = "[]"
        val inputStream = ByteArrayInputStream(json.toByteArray())
        val extractor = JsonTextExtractor(inputStream)

        assertEquals(0, extractor.getPageCount())
    }

    @Test
    fun extract_returnsEmptyListForEmptyInput() {
        val json = "[]"
        val inputStream = ByteArrayInputStream(json.toByteArray())
        val extractor = JsonTextExtractor(inputStream)

        val texts = extractor.extract(1)
        assertTrue(texts.isEmpty())
    }
}