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

package io.mfj.textricator.extractor.itext7
import io.mfj.textricator.extractor.TextExtractor
import io.mfj.textricator.text.Text

import java.io.InputStream

import com.itextpdf.kernel.colors.*
import com.itextpdf.kernel.geom.Vector
import com.itextpdf.kernel.pdf.*
import com.itextpdf.kernel.pdf.canvas.parser.EventType
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy

/**
 * Extract text using iText 7.
 *
 * This gets accurate locations, and does not attempt to group characters based on proximity or structure.
 */
class Itext7TextExtractor(input:InputStream):TextExtractor {

  private val reader = PdfReader(input)
  private val doc = PdfDocument(reader)

  /** Number of pages in the PDF */
  private val pageCount:Int = doc.numberOfPages

  override fun getPageCount():Int {
    return pageCount
  }

  override fun close() {
    reader.close()
  }


  /**
   * Extract text from the PDF, calling the callback for each text block.
   *
   * @param pageNumber Page to extract text from
   *
   */
  override fun extract(pageNumber:Int):List<Text> {

    val page = doc.getPage(pageNumber)

    val pageHeight = page.pageSize.height

    val links:List<Link> = page.annotations
        .filter { anno ->
          anno.subtype == PdfName.Link
        }
        .map { anno ->
          val aObj = anno.pdfObject[PdfName.A] as PdfDictionary
          val uriObj = (aObj[PdfName.URI] ?: aObj[PdfName.URL]) as PdfString
          val uri = uriObj.value
          // get the bounding box
          val rect = (anno.pdfObject[PdfName.Rect] as PdfArray).map { (it as PdfNumber).floatValue() }
          Link(
              url = uri,
              ulx = rect[0],
              uly = calcY(pageHeight, rect[3]),
              lrx = rect[2],
              lry = calcY(pageHeight, rect[1])
          )
        }

    val strategy = Strategy(pageNumber, pageHeight, links)

    PdfTextExtractor.getTextFromPage(page,strategy)

    return strategy.texts
        .sortedWith( compareBy( {it.uly}, {it.ulx} ) )
  }

  private class Strategy( private val pageNumber:Int, private val pageHeight:Float, private val links:List<Link>):
    LocationTextExtractionStrategy() {
    val texts = mutableListOf<Text>()

    override fun eventOccurred(data:IEventData?, type:EventType?) {
      if ( type == EventType.RENDER_TEXT ) {
        val ri = data as TextRenderInfo
        val content = ri.text

        val matrix = ri.textMatrix
        val font = ri.font.fontProgram.fontNames.fontName

        val ulx = matrix[6]
        val lry = calcY( pageHeight, matrix[7] )
        val width = ri.font.getWidth(content,ri.fontSize)
        val height = ri.font.getAscent(content, ri.fontSize)
        val uly = lry - height
        val lrx = ulx + width

        val color = ri.fillColor?.getHexColor()

        val link = links
            .firstOrNull { link ->
              ulx >= link.ulx
                  && uly >= link.uly
                  && lrx <= link.lrx
                  && lry <= link.lry
            }
            ?.url

        val effMatrix = ri.textMatrix.multiply( ri.graphicsState.ctm )
        val fontSize = Vector(0f,ri.fontSize,0f).cross(effMatrix)[1]

        val text = Text(content = content, backgroundColor = null, pageNumber = pageNumber,
            fontSize = fontSize, font = font, color = color,
            ulx = ulx, uly = uly, lrx = lrx, lry = lry,
            link = link )

        texts.add( text )
      }
      super.eventOccurred(data, type)
    }

    private fun Color.getHexColor(): String? =
        when ( this ) {
          is DeviceRgb -> getRgb( this )
          is DeviceCmyk -> getRgb( Color.convertCmykToRgb( this ) )
          else -> null
        }

    private fun getRgb( color:DeviceRgb ): String? {
      val value = color.colorValue
      val r = ( value[0] * 255 ).toInt()
      val g = ( value[1] * 255 ).toInt()
      val b = ( value[2] * 255 ).toInt()
      return "#%02x%02x%02x".format( r, g, b )
    }

  }

  companion object {
    data class Link(val url:String, val lrx:Float, val lry:Float, val ulx:Float, val uly:Float)

    // in iText, if y is positive, it is from the bottom, if y is negative, it is from the top.
    // We calculate from the top.
    fun calcY(pageHeight:Float, y:Float):Float {
      return if (y >= 0) {
        pageHeight - y
      } else {
        y * -1
      }
    }

  }

}
