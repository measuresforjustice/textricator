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

package io.mfj.textricator.extractor.csv

import io.mfj.textricator.extractor.TextExtractor
import io.mfj.textricator.text.Text
import io.mfj.textricator.text.output.CsvTextOutput

import java.io.InputStream

import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord

/**
 * Parse the CSV output from [CsvTextOutput].
 */
class CsvTextExtractor(input:InputStream):TextExtractor {

  // load into memory up front
  // Do not assume that pages are in order in the CSV.
  private val pages:MutableMap<Int,MutableList<Text>> = mutableMapOf<Int,MutableList<Text>>()
      .apply {
        CSVParser(input.bufferedReader(), CsvTextOutput.CSV_FORMAT).use { csvp ->
          csvp.asSequence()
              .drop(1) // header
              .map(::parseRec)
              .forEach { text ->
                getOrPut(text.pageNumber) { mutableListOf() }.add(text)
              }
        }
      }

  private val pageCount = pages.keys.sorted().lastOrNull() ?: 0

  // This matches CsvTextOutput.write(Text)
  private fun parseRec(rec:CSVRecord): Text =
      Text(
          pageNumber = rec[0].toInt(),
          ulx = rec[1].toFloat(),
          uly = rec[2].toFloat(),
          lrx = rec[3].toFloat(),
          lry = rec[4].toFloat(),
          // width (rec[5]) is calculated from ulx and lrx
          // height (rec[6]) is calculated from uly and lry
          content = rec[7],
          font = rec[8],
          fontSize = rec[9].toFloat(),
          color = rec[10],
          backgroundColor = rec[11],
          link = rec[12]
      )

  override fun getPageCount():Int = pageCount

  override fun extract(pageNumber:Int):List<Text> = pages[pageNumber] ?: emptyList()

  override fun close() {}

}
