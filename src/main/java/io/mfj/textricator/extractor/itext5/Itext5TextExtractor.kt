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

package io.mfj.textricator.extractor.itext5

import io.mfj.textricator.extractor.TextExtractor
import io.mfj.textricator.text.Text

import com.itextpdf.text.BaseColor
import com.itextpdf.text.pdf.*
import com.itextpdf.text.pdf.parser.ContentOperator
import com.itextpdf.text.pdf.parser.FilteredTextRenderListener
import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy
import com.itextpdf.text.pdf.parser.PdfContentStreamProcessor
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.itextpdf.text.pdf.parser.Vector

import java.io.InputStream

import kotlin.math.min
import kotlin.math.max

import org.slf4j.LoggerFactory

/**
 * Class to extract text from a PDF.
 *
 * Create an instance and call [extract] for each page.
 *
 * @constructor Create an instance for the supplied PDF.
 */
class Itext5TextExtractor(input:InputStream, boxPrecision:Float?, boxIgnoreColors:Set<String>? ):
    TextExtractor {

  private val reader = PdfReader(input)

  private val boxPrecision = boxPrecision?: 0f

  private val boxtricator = Boxtricator(reader, boxIgnoreColors ?: emptySet())

  /** Number of pages in the PDF */
  private val pageCount:Int = reader.numberOfPages
  private val pageRange = (1..pageCount)

  override fun getPageCount():Int {
    return pageCount
  }

  companion object {
    private val log = LoggerFactory.getLogger(Itext5TextExtractor::class.java)

    internal fun color( baseColor:BaseColor? ): String {
      if ( baseColor == null ) return "default"
      return "#%02x%02x%02x".format( baseColor.red, baseColor.green, baseColor.blue )
    }

    data class Link( val url:String, val lrx:Float, val lry:Float, val ulx:Float, val uly:Float)
  }

  override fun close() {
    reader.close()
  }

  /**
   * Get the size, in points, of the specified page, as a width/height pair.
   *
   * @param pageNumber Page number
   */
  private fun getPageSize(pageNumber:Int):Size {
    val rect = reader.getPageSize(pageNumber)
    return Size(rect.width, rect.height)
  }

  /**
   * Extract text from the PDF, calling the callback for each text block.
   *
   * @param pageNumber Page to extract text from
   *
   */
  override fun extract(pageNumber:Int):List<Text> {

    if ( ! pageRange.contains( pageNumber ) ) {
      throw IllegalArgumentException( "Invalid page number: $pageNumber. Valid pages are $pageRange" )
    }

    val pageHeight = getPageSize(pageNumber).height

    // in iText, if y is positive, it is from the bottom, if y is negative, it is from the top.
    // We calculate from the top.
    fun calcY(y:Float):Float {
      return if (y >= 0) {
        pageHeight - y
      } else {
        y * -1
      }
    }

    val boxes:List<Box> = boxtricator.getBoxes(pageNumber,pageHeight)

    val links = reader.getLinks(pageNumber)
        .mapNotNull { annotation ->
          val a = annotation.parameters[PdfName.A]
          if ( a is PdfDictionary ) {
            val uriPdfString = a[PdfName.URI] as PdfString
            val uri = uriPdfString.toUnicodeString()
            val rect = annotation.rect.map { o -> ( o as PdfNumber ).floatValue() }
            Link(
                url = uri,
                ulx = rect[0],
                uly = calcY(rect[3]),
                lrx = rect[2],
                lry = calcY(rect[1])
            )
          } else {
            // Found a file where it is a com.itextpdf.text.pdf.PRIndirectReference.
            // Did not need to get links from that document, so did not figure out what to do.
            null
          }
        }

    fun Buffer.toText():Text {
      val content = content.toString().trim()

      val link = links
          .firstOrNull { link ->
            ulx >= link.ulx
                && uly >= link.uly
                && lrx <= link.lrx
                && lry <= link.lry
          }
          ?.url

      return Text(
          content = content,
          pageNumber = pageNumber,
          ulx = ulx,
          uly = uly,
          lrx = lrx,
          lry = lry,
          font = font,
          fontSize = fontSize,
          color= fontColor,
          backgroundColor = getBackground(boxes, this, content),
          link = link )
    }

    val texts:MutableList<Text> = mutableListOf()

    val strategy = FilteredTextRenderListener(LocationTextExtractionStrategy())

    var buffer:Buffer? = null



    // start a new text segment
    fun start(x:Float, y:Float, font:String, fontSize:Float, fontColor:String?) {
      if (buffer != null) throw Exception("Forgot to call flush(). Text: $buffer")
      buffer = Buffer(pageNumber, x, y, x, y, font, fontSize, fontColor)
    }

    // append text to an existing segment
    // font and fontSize are ignored unless somebody forgot to call start()
    fun append(x:Float, y:Float, text:String, font:String, fontSize:Float, fontColor:String? ) {
      if ( buffer == null ) {
        // ' or " without Tj - I have seen this with some PDFs modified with qoppa's PDFStudio.
        log.warn("Forgot to call start()")
        start(x,y,font,fontSize,fontColor)
      }
      buffer!!.content.append(text)
      buffer!!.lrx = x
      buffer!!.lry = y
    }

    // Call the callback with the buffer content.
    fun flush() {
      if (buffer != null) {
        texts.add( buffer!!.toText() )
        buffer = null
      }
    }

    // Define operators to capture text from the content stream

    // capture strings - start a new segment
    val stringOperator = ContentOperator { processor:PdfContentStreamProcessor, operator:PdfLiteral?, operands:ArrayList<PdfObject> ->
      val string = operands[0] as PdfString
      val matrix = processor.textMatrix
      val x = matrix[6]
      val y = calcY( matrix[7] )
      val bytes = string.bytes
      val gs = processor.gs()
      val text = gs.font.decode(bytes, 0, bytes.size)
      val fontSize = getFontSize(processor)
      val width = gs.font.getWidthPoint(text, fontSize)
      val fontColor = color(gs.fillColor)

      flush()

      log.debug("{${pageNumber}} string [ ${x}, $y ] $text")
      start(x, y, gs.font.postscriptFontName, fontSize, fontColor)
      append(x + width, y, text, gs.font.postscriptFontName, fontSize, fontColor )
    }

    // capture continuation of previous string - append to existing segment
    val stringOperatorContinue = ContentOperator { processor:PdfContentStreamProcessor, operator:PdfLiteral?, operands:ArrayList<PdfObject> ->
      val string = operands[0] as PdfString
      val matrix = processor.textMatrix
      val x = matrix[6]
      val y = calcY( matrix[7] )
      val bytes = string.bytes
      val gs = processor.gs()
      val text = gs.font.decode(bytes, 0, bytes.size)
      val fontSize = getFontSize(processor)
      val width = gs.font.getWidthPoint(text, fontSize)
      val fontColor = color(gs.fillColor)

      // continuation from stringOperator
      log.debug("{${pageNumber}} continue [ ${x}, $y ] $text")
      append(x + width, y, " $text", gs.font.postscriptFontName, fontSize, fontColor)
    }

    // capture pdfarray - this is all one segment
    val arrayOperator = ContentOperator { processor:PdfContentStreamProcessor, operator:PdfLiteral?, operands:ArrayList<PdfObject> ->
      val array = operands[0] as PdfArray
      val matrix = processor.textMatrix
      val x = matrix[6]
      val y = calcY( matrix[7] )
      val gs = processor.gs()
      val fontSize = getFontSize(processor)

      // combine all PdfStrings in the PdfArray to one String
      val text = array.asSequence().filter { it is PdfString }.map { it as PdfString }.map { pdfString ->
        // PdfString.toUnicodeString() does not work in all cases.
        val bytes = pdfString.bytes
        gs.font.decode(bytes, 0, bytes.size)
      }.joinToString(separator = "")
      val width = gs.font.getWidthPoint(text, fontSize)
      val fontColor = color(gs.fillColor)

      flush()

      log.debug("{${pageNumber}} array [ $x , $y ] $text")
      start(x, y, gs.font.postscriptFontName, fontSize, fontColor)
      append(x + width, y, text, gs.font.postscriptFontName, fontSize, fontColor)
      flush()
    }

    // This is setting up what PdfContentStreamProcesses does internally.
    val twOperator = SetTextWordSpacing()
    val tcOperator = SetTextCharacterSpacing()
    val tdOperator = TextMoveStartNextLine()
    val tstarOperator = TextMoveNextLine(tdOperator)
    val tickOperator = MoveNextLineAndShowText(tstarOperator, stringOperatorContinue)
    val quoteOperator = MoveNextLineAndShowTextWithSpacing(twOperator, tcOperator,
        tickOperator)

    // This results in calls to the content operators defined above. We do not care about its return value.
    PdfTextExtractor.getTextFromPage(reader, pageNumber, strategy,
        mapOf("Tj" to stringOperator, "TJ" to arrayOperator, "'" to tickOperator, "\"" to quoteOperator))

    flush()

    return texts
        .sortedWith( compareBy( {it.uly}, {it.ulx} ) )
  }

  private fun getBackground(all:List<Box>?,buffer:Buffer,content:String): String? {
    if ( all == null ) return null
    val boxes = all.filter { box -> isBufferInBox( buffer, box ) }
    val box:Box? = boxes.lastOrNull()
    return box?.color
  }

  private fun isBufferInBox( buffer:Buffer, box:Box): Boolean {
    // shrink the buffer by the precision, but do not shrink width or height to negative.
    return box.ulx <= min( buffer.ulx + boxPrecision, buffer.lrx )
        && box.uly <= min( buffer.uly + boxPrecision, buffer.lry )
        && box.lrx >= max( buffer.lrx - boxPrecision, buffer.ulx )
        && box.lry >= max( buffer.lry - boxPrecision, buffer.uly )
  }

  private fun getFontSize( processor:PdfContentStreamProcessor ):Float {
    val gs = processor.gs()
    val effMatrix = processor.textMatrix.multiply( gs.ctm )
    val fontSize = Vector(0f,gs.fontSize,0f).cross(effMatrix)[1]
    return fontSize
  }

}
