package io.mfj.textricator.extractor.text.output

import io.mfj.textricator.text.Text
import io.mfj.textricator.text.output.CsvTextOutput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import kotlin.test.assertFailsWith

class CsvTextOutputTest {
    @Test
    fun writesValidTextSequenceToCsv() {
        val outCsv = File.createTempFile("valid-text-sequence", ".csv")
        outCsv.deleteOnExit()
        try {
            outCsv.outputStream().use { out ->
                val texts = sequenceOf(
                    Text("content 1", 1, 0F, 1F, 1F, 1F, "arial", 8F, null, null, null),
                    Text("content 2", 2, 0F, 1F, 1F, 1F, "arial", 8F, null, null, null)
                )
                val csvOutput = CsvTextOutput(out)
                csvOutput.write(texts)
                csvOutput.close()
                val expectedCsv = """
                page,ulx,uly,lrx,lry,width,height,content,font,fontSize,fontColor,bgcolor,link
                1,0.0,1.0,1.0,1.0,1.0,0.0,content 1,arial,8.0,,,
                2,0.0,1.0,1.0,1.0,1.0,0.0,content 2,arial,8.0,,,
            """.trimIndent()
                assertEquals(expectedCsv, outCsv.readText().trim())
            }
        } finally {
            outCsv.delete()
        }
    }

    @Test
    fun handlesEmptyTextSequence() {
        val outCsv = File.createTempFile("empty-text-sequence", ".csv")
        outCsv.deleteOnExit()
        try {
            outCsv.outputStream().use { out ->
                val texts = emptySequence<Text>()
                val csvOutput = CsvTextOutput(out)
                csvOutput.write(texts)
                csvOutput.close()
                val expectedCsv = "page,ulx,uly,lrx,lry,width,height,content,font,fontSize,fontColor,bgcolor,link"
                assertEquals(expectedCsv, outCsv.readText().trim())
            }
        } finally {
            outCsv.delete()
        }
    }

    @Test
    fun handlesTextWithNullFields() {
        val outCsv = File.createTempFile("text-with-null-fields", ".csv")
        outCsv.deleteOnExit()
        try {
            outCsv.outputStream().use { out ->
                val texts = sequenceOf(
                    Text("Page 1", 1, 0F, 1F, 1F, 1F, "times", 2F, null, null, null)
                )
                val csvOutput = CsvTextOutput(out)
                csvOutput.write(texts)
                csvOutput.close()
                val expectedCsv = """
                page,ulx,uly,lrx,lry,width,height,content,font,fontSize,fontColor,bgcolor,link
                1,0.0,1.0,1.0,1.0,1.0,0.0,Page 1,times,2.0,,,
            """.trimIndent()
                assertEquals(expectedCsv, outCsv.readText().trim())
            }
        } finally {
            outCsv.delete()
        }
    }

    @Test
    fun doesCloseOutputStream() {
        val outCsv = File.createTempFile("output-stream-not-closed", ".csv")
        outCsv.deleteOnExit()
        try {
            outCsv.outputStream().use { out ->
                val texts = sequenceOf(
                    Text("Page 1", 1, 0F, 1F, 1F, 1F, "times", 2F, null, null, null))
                val csvOutput = CsvTextOutput(out)
                csvOutput.write(texts)
                csvOutput.close()
                assertFailsWith<IOException> { out.write("still open".toByteArray()) }
            }
        } finally {
            outCsv.delete()
        }
    }
}