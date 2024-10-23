package io.mfj.textricator.extractor.text.output

import io.mfj.textricator.text.Text
import io.mfj.textricator.text.output.JsonTextOutput
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class JsonTextOutputTest {

    @Test
    fun write_outputsCorrectJson() {
        val outputStream = ByteArrayOutputStream()
        val jsonTextOutput = JsonTextOutput(outputStream)
        val texts = sequenceOf(
            Text(
                "content1",
                1,
                0F, 0F, 0F, 0F,
                "arial",
                16F,
                "black"), Text(
                "content2",
                    2,
                    1F, 1F, 1F, 1F,
                    "times",
                    8F))

        jsonTextOutput.write(texts)
        jsonTextOutput.close()

        val expectedJson = """
            [{
                "content" : "content1",
                "pageNumber" : 1,
                "ulx" : 0.0,
                "uly" : 0.0,
                "lrx" : 0.0,
                "lry" : 0.0,
                "font" : "arial",
                "fontSize" : 16.0,
                "color" : "black",
                "backgroundColor" : null,
                "link" : null
              },{
                "content" : "content2",
                "pageNumber" : 2,
                "ulx" : 1.0,
                "uly" : 1.0,
                "lrx" : 1.0,
                "lry" : 1.0,
                "font" : "times",
                "fontSize" : 8.0,
                "color" : null,
                "backgroundColor" : null,
                "link" : null
               }]
        """.trimIndent()
        assertEquals(expectedJson.replace(" ", ""), outputStream.toString().replace(" ", ""))
    }

    @Test
    fun write_handlesEmptySequence() {
        val outputStream = ByteArrayOutputStream()
        val jsonTextOutput = JsonTextOutput(outputStream)
        val texts = emptySequence<Text>()

        jsonTextOutput.write(texts)
        jsonTextOutput.close()

        val expectedJson = "[]"
        assertEquals(expectedJson, outputStream.toString().trim())
    }

    @Test
    fun write_handlesSingleText() {
        val outputStream = ByteArrayOutputStream()
        val jsonTextOutput = JsonTextOutput(outputStream)
        val texts = sequenceOf(Text("singleContent",3,
            1F, 1F, 1F, 1F,
            "times",
            8F))

        jsonTextOutput.write(texts)
        jsonTextOutput.close()

        val expectedJson = """
            [{
                "content" : "singleContent",
                "pageNumber" : 3,
                "ulx" : 1.0,
                "uly" : 1.0,
                "lrx" : 1.0,
                "lry" : 1.0,
                "font" : "times",
                "fontSize" : 8.0,
                "color" : null,
                "backgroundColor" : null,
                "link" : null
              }]
        """.trimIndent()
        assertEquals(expectedJson.replace(" ", ""), outputStream.toString().replace(" ", ""))
    }

    @Test
    fun write_doesNotCloseOutputStream() {
        val outputStream = ByteArrayOutputStream()
        val jsonTextOutput = JsonTextOutput(outputStream)
        val texts = sequenceOf(Text("content", 1,
            0F, 0F, 0F, 0F,
            "arial",
            16F,
            "black"))

        jsonTextOutput.write(texts)
        jsonTextOutput.close()

        assertDoesNotThrow { outputStream.write("still open".toByteArray()) }
    }
}