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

import com.itextpdf.text.pdf.*
import com.itextpdf.text.pdf.parser.*

// This is copies of a bunch of stuff that is private in IText's PdfContentStreamProcessor and GraphicsState
// DO NOT USE THIS!!! (except in Itext5TextExtractortor.kt)

/**
 * A content operator implementation (Td).
 */
class TextMoveStartNextLine:ContentOperator {
  override fun invoke(processor:PdfContentStreamProcessor, operator:PdfLiteral?, operands:ArrayList<PdfObject>) {
    val tx = (operands.get(0) as PdfNumber).floatValue()
    val ty = (operands.get(1) as PdfNumber).floatValue()

    val translationMatrix = Matrix(tx, ty)
    processor.textMatrix = translationMatrix.multiply(processor.textLineMatrix)
    processor.textLineMatrix = processor.textMatrix
  }

}

/**
 * A content operator implementation (T*).
 */
class TextMoveNextLine(private val moveStartNextLine:TextMoveStartNextLine):ContentOperator {
  override fun invoke(processor:PdfContentStreamProcessor, operator:PdfLiteral?, operands:ArrayList<PdfObject>) {
    val tdoperands:ArrayList<PdfObject> = ArrayList<PdfObject>(2)
    tdoperands.add(0, PdfNumber(0))
    tdoperands.add(1, PdfNumber(-processor.gs().leading))
    moveStartNextLine.invoke(processor, null, tdoperands)
  }
}

/**
 * A content operator implementation (').
 */
class MoveNextLineAndShowText(private val textMoveNextLine:TextMoveNextLine, private val showText:ContentOperator):
    ContentOperator {
  override fun invoke(processor:PdfContentStreamProcessor, operator:PdfLiteral?, operands:ArrayList<PdfObject>) {
    textMoveNextLine.invoke(processor, null, ArrayList<PdfObject>(0))
    showText.invoke(processor, null, operands)
  }
}

/**
 * A content operator implementation (Tw).
 */
class SetTextWordSpacing:ContentOperator {
  override fun invoke(processor:PdfContentStreamProcessor, operator:PdfLiteral?, operands:ArrayList<PdfObject>) {
    val wordSpace = operands.get(0) as PdfNumber
    processor.gs().setWordSpacing(wordSpace.floatValue())
  }
}

/**
 * A content operator implementation (Tc).
 */
class SetTextCharacterSpacing:ContentOperator {
  override fun invoke(processor:PdfContentStreamProcessor, operator:PdfLiteral?, operands:ArrayList<PdfObject>) {
    val charSpace = operands.get(0) as PdfNumber
    processor.gs().setCharacterSpacing(charSpace.floatValue())
  }
}

/**
 * A content operator implementation (").
 */
class MoveNextLineAndShowTextWithSpacing(private val setTextWordSpacing:SetTextWordSpacing,
    private val setTextCharacterSpacing:SetTextCharacterSpacing,
    private val moveNextLineAndShowText:MoveNextLineAndShowText):ContentOperator {

  override fun invoke(processor:PdfContentStreamProcessor, operator:PdfLiteral?, operands:ArrayList<PdfObject>) {

    val aw:PdfNumber = operands.get(0) as PdfNumber
    val ac:PdfNumber = operands.get(1) as PdfNumber
    val string:PdfString = operands.get(2) as PdfString

    val twOperands:ArrayList<PdfObject> = ArrayList<PdfObject>(1)
    twOperands.add(0, aw)
    setTextWordSpacing.invoke(processor, null, twOperands)

    val tcOperands:ArrayList<PdfObject> = ArrayList<PdfObject>(1)
    tcOperands.add(0, ac)
    setTextCharacterSpacing.invoke(processor, null, tcOperands)

    val tickOperands:ArrayList<PdfObject> = ArrayList<PdfObject>(1)
    tickOperands.add(0, string)
    moveNextLineAndShowText.invoke(processor, null, tickOperands)
  }
}

/**
 * private/protected things we need to access
 */
var PdfContentStreamProcessor.textMatrix:Matrix
  get() = javaClass.getDeclaredField("textMatrix").let {
    it.isAccessible = true
    it.get(this) as Matrix
  }
  set(textMatrix) = javaClass.getDeclaredField("textMatrix").let {
    it.isAccessible = true
    it.set(this, textMatrix)
  }

/// accessors for private itext stuff

var PdfContentStreamProcessor.textLineMatrix:Matrix
  get() = javaClass.getDeclaredField("textLineMatrix").let {
    it.isAccessible = true
    it.get(this) as Matrix
  }
  set(textLineMatrix) = javaClass.getDeclaredField("textLineMatrix").let {
    it.isAccessible = true
    it.set(this, textLineMatrix)
  }

fun GraphicsState.setWordSpacing(wordSpacing:Float) {
  javaClass.getDeclaredField("wordSpacing").let {
    it.isAccessible = true
    it.set(this, wordSpacing)
  }
}

fun GraphicsState.setCharacterSpacing(wordSpacing:Float) {
  javaClass.getDeclaredField("characterSpacing").let {
    it.isAccessible = true
    it.set(this, wordSpacing)
  }
}

val PathPaintingRenderInfo.gs:GraphicsState
  get() = javaClass.getDeclaredField("gs").let {
    it.isAccessible = true
    it.get(this) as GraphicsState
  }
