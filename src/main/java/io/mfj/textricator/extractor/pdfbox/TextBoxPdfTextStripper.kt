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

import java.io.IOException
import java.util.regex.Pattern

import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition

internal class TextBoxPdfTextStripper : PDFTextStripper() {

  companion object {
    const val NON_BREAKING_SPACE:String = "\u00A0"
    val NON_PRINTABLE:Pattern = Pattern.compile(".*[\\u0000-\\u0019]+.*")
  }

  internal val wordList: MutableMap<Int, MutableList<Text>> = mutableMapOf()

  /**
   * used to group words decoded at the same time.
   */
  private var chunkIndex:Long = 0

  @Throws(IOException::class)
  override fun writeString(text:String, textPositions:List<TextPosition> ) {
    chunkIndex++
    val sb = StringBuilder()
    // add initial position

    // look for huge space gaps:
    var wordStart:Int = -1
    var current:TextPosition = textPositions.get(0)
    var previous: TextPosition
    var maxHeight:Float = 0f

    (0..textPositions.size-1).forEach { i ->
      previous = current

      current = textPositions.get(i)

      val separation:Float = current.getX() - (previous.getX() + previous.getWidth())
      val sameFont:Boolean = current.getFont().equals(previous.getFont())
          && current.getFontSize() == previous.getFontSize()


      if (current.getUnicode().endsWith(" ") ||
          current.getUnicode().endsWith(
              NON_BREAKING_SPACE) ||
          (wordStart != -1 && (separation >= previous.getWidthOfSpace()))) {
        addWord(sb, wordStart, i, textPositions, maxHeight)
        maxHeight = 0f
        wordStart = -1
      } else if ((i > 0 && separation < -(previous.getWidth()) / 2) || !sameFont) {
        // split in cases where words overlay (eg in excel print outs! // TODO : this should be a configuable param
        // have to exclude i = 0 as we set previous to the first char
        addWord(sb, wordStart, i, textPositions, maxHeight)
        maxHeight = 0f
        wordStart = i
        sb.append(current.getUnicode())
      } else {
        if (wordStart == -1) {
          wordStart = i
        }
        sb.append(current.getUnicode())
      }
      val gs = graphicsState
      val color = gs.strokingColor.toRGB()
      val nscolor = gs.nonStrokingColor.toRGB()
      if ( sb.toString() == "MOHAMMED" ) {
        println( gs )
      }
      maxHeight = Math.max(maxHeight, current.getHeight())

    }

    addWord(sb, wordStart, textPositions.size, textPositions, maxHeight)
  }

  private fun expandNonPrintableUnicode(string:String) :String {
    if (NON_PRINTABLE.matcher(string).matches()) {
      val sb = StringBuilder()
      (0..string.length-1).forEach { i ->
        val cp = string.codePointAt(i)
        if (cp < 0x20) {
          sb.append(String.format("\\x%02x", cp))
        } else {
          sb.append(string[i])
        }
      }
      return sb.toString()
    } else {
      return string
    }
  }

  private fun addWord(sb: StringBuilder, startIndex:Int, endIndex:Int,
      textPositions: List<TextPosition>, maxHeight:Float) {
    if (sb.length > 0) {
      val first = textPositions.get(startIndex)
      val previous = textPositions.get(endIndex - 1)

      val expanded = expandNonPrintableUnicode(sb.toString())

      val textBox = Text(content = expanded, pageNumber = currentPageNo, ulx = first.x,
          uly = first.y - first.height, lrx = previous.x + previous.width, lry = first.y, font = first.font.name,
          fontSize = first.fontSize, color = null, // TODO
          backgroundColor = null)

      wordList.putIfAbsent(textBox.pageNumber, mutableListOf())
      val pageList = wordList.get(textBox.pageNumber)!!
      pageList.add(textBox)
      sb.setLength(0)
    }
  }


}
