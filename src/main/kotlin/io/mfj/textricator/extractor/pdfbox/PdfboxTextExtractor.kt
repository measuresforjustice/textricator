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

package io.mfj.textricator.extractor.pdfbox

import io.mfj.textricator.text.Text
import io.mfj.textricator.extractor.TextExtractor

import java.io.InputStream

import org.apache.pdfbox.pdmodel.PDDocument

class PdfboxTextExtractor(input:InputStream):TextExtractor {

  private val doc = PDDocument.load(input)

  private val stripper = TextBoxPdfTextStripper()

  override fun extract(pageNumber:Int):List<Text> {

    stripper.startPage = pageNumber
    stripper.endPage = pageNumber
    stripper.getText(doc)

    return stripper.wordList[pageNumber]?.sortedWith( compareBy( Text::uly, Text::ulx ) ) ?: emptyList()
  }

  override fun getPageCount():Int = doc.numberOfPages

  override fun close() {
    doc.close()
  }

}
