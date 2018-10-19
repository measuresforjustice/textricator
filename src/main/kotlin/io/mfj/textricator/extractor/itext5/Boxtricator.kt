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

import com.itextpdf.text.BaseColor

import com.itextpdf.text.pdf.*
import com.itextpdf.text.pdf.parser.*
import io.mfj.textricator.extractor.itext5.Itext5TextExtractor.Companion.color

import org.slf4j.LoggerFactory

// adapted from
// https://github.com/mkl-public/testarea-itext5/blob/master/src/test/java/mkl/testarea/itext5/extract/ExtractPaths.java
internal class Boxtricator(private val reader:PdfReader,
    private val ignoreBoxColors:Set<String> ) {

  companion object {
    private val log = LoggerFactory.getLogger( Boxtricator::class.java )
  }

  fun getBoxes( pageNumber:Int, pageHeight:Float):List<Box> {

    val boxes:MutableList<Box> = mutableListOf()

    val parser = PdfReaderContentParser(reader)
    parser.processContent(pageNumber,
        MyExtRenderListener(boxes, pageHeight, ignoreBoxColors))
    return if ( boxes.isNotEmpty() ) boxes else emptyList()
  }

  private class MyExtRenderListener(val boxes:MutableList<Box>, val pageHeight:Float,
      val ignoreColors:Set<String> ): ExtRenderListener {
    private val pathInfos:MutableList<PathConstructionRenderInfo?> = mutableListOf()
    override fun beginTextBlock() {}
    override fun renderText(renderInfo:TextRenderInfo?) {}
    override fun endTextBlock() {}
    override fun renderImage(renderInfo:ImageRenderInfo?) {}
    override fun modifyPath(renderInfo:PathConstructionRenderInfo?) {
      pathInfos.add(renderInfo)
    }
    override fun renderPath(renderInfo:PathPaintingRenderInfo):Path? {
      val graphicsState:GraphicsState = renderInfo.gs

      val ctm = graphicsState.ctm

      val fill = (renderInfo.getOperation() and PathPaintingRenderInfo.FILL) != 0

      if ( fill ) {
        val fillColor = graphicsState.fillColor
        val color = color(fillColor)

        if ( ! ignoreColors.contains( color ) ) {

          log.debug("\tthe path:")

          pathInfos.forEach { pathConstructionRenderInfo ->
            when ( pathConstructionRenderInfo?.operation ) {
              PathConstructionRenderInfo.MOVETO -> {
                log.debug("move to {} ", transform(ctm, pathConstructionRenderInfo.segmentData))
              }
              PathConstructionRenderInfo.CLOSE -> {
                log.debug("\tclose {} ", transform(ctm, pathConstructionRenderInfo.segmentData))

              }
              PathConstructionRenderInfo.CURVE_123 -> {
                log.debug("\tcurve123 {} ", transform(ctm, pathConstructionRenderInfo.segmentData))

              }
              PathConstructionRenderInfo.CURVE_13 -> {
                log.debug("\tcurve13 {} ", transform(ctm, pathConstructionRenderInfo.segmentData))

              }
              PathConstructionRenderInfo.CURVE_23 -> {
                log.debug("\tcurve23 {} ", transform(ctm, pathConstructionRenderInfo.segmentData))

              }
              PathConstructionRenderInfo.LINETO -> {
                log.debug("\tline to {} ", transform(ctm, pathConstructionRenderInfo.segmentData))

              }
              PathConstructionRenderInfo.RECT -> {
                val matrix = transform(ctm, expandRectangleCoordinates(pathConstructionRenderInfo.segmentData))!!
                log.debug("rectangle {} ", matrix)

                val box = Box(ulx = matrix[0 /* or 6 */], uly = calcY(matrix[5 /* or 7 */]),
                    lrx = matrix[4 /* or 2 */], lry = calcY(matrix[1 /* or 3 */]), color = color(fillColor))

                log.debug( "box: ${box}" )
                boxes.add( box )
              }
              /*
              else -> {
                throw Exception( "\t\tunhandled ${pathConstructionRenderInfo?.operation}" )
              }
              */
            }
          }
        }
      }

      pathInfos.clear()
      return null
    }

    override fun clipPath(rule:Int) {}

    private fun transform(ctm:Matrix, coordinates:List<Float>? ):List<Float>? {
      if ( coordinates == null ) return null
      val result:MutableList<Float> = mutableListOf()
      var i = 0
      while ( i < coordinates.size-1) {
        var vector = Vector(coordinates.get(i), coordinates.get(i + 1), 1f)
        vector = vector.cross(ctm)
        result.add(vector.get(Vector.I1))
        result.add(vector.get(Vector.I2))
        i+=2
      }
      return result
    }

    private fun expandRectangleCoordinates(rectangle:List<Float> ):List<Float> {
      if (rectangle.size < 4) return emptyList()

      return listOf(
          rectangle.get(0), // x (left)
          rectangle.get(1), // y (bottom) (from bottom)
          rectangle.get(0) + rectangle.get(2), // x (right)
          rectangle.get(1), // y (bottom) (from bottom)
          rectangle.get(0) + rectangle.get(2), // x (right)
          rectangle.get(1) + rectangle.get(3), // y (top) (from bottom)
          rectangle.get(0), // x (left)
          rectangle.get(1) + rectangle.get(3) // y (top) (from bottom)
      )
    }

    private fun BaseColor?.format(): String = if (this == null) "DEFAULT" else "${red},${green},${blue}"

    // in iText, if y is positive, it is from the bottom, if y is negative, it is from the top.
    // We calculate from the top.
    fun calcY(y:Float):Float {
      return if (y >= 0) {
        pageHeight - y
      } else {
        y * -1
      }
    }
  }

}
