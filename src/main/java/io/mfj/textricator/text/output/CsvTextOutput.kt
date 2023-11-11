/*
This file is part of Textricator.
Copyright 2018 Measures for Justice Institute.

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU Affero General Public License version 3 as published by the
Free Software Foundation.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along
with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package io.mfj.textricator.text.output

import io.mfj.textricator.text.Text

import java.io.OutputStream

import org.apache.commons.csv.*

import org.slf4j.LoggerFactory
import java.io.OutputStreamWriter

class CsvTextOutput(output:OutputStream):TextOutput {

  private val w = OutputStreamWriter(output)
  private val p = CSVPrinter(w, CSV_FORMAT)

  companion object {
    private val log = LoggerFactory.getLogger(CsvTextOutput::class.java)
    val CSV_FORMAT = CSVFormat.DEFAULT.withRecordSeparator("\n")
  }

  override fun close() {
    p.close()
    w.close()
  }

  override fun write(seq:Sequence<Text>) {
    writeHeader()
    writeTexts(seq)
  }

  private fun writeTexts(seq:Sequence<Text>) {
    seq.forEach { text ->
      write( text )
    }
  }

  private fun writeHeader() {
    p.printRecord(
        "page",
        "ulx",
        "uly",
        "lrx",
        "lry",
        "width",
        "height",
        "content",
        "font",
        "fontSize",
        "fontColor",
        "bgcolor",
        "link"
    )
  }

  private fun write(text:Text) {
    // If you change this, change CsvTextExtractor.parseRec(CSVRecord) to match.
    p.printRecord(
        text.pageNumber,
        text.ulx,
        text.uly,
        text.lrx,
        text.lry,
        text.width,
        text.height,
        text.content,
        text.font,
        text.fontSize,
        text.color,
        text.backgroundColor,
        text.link
    )
  }

}
