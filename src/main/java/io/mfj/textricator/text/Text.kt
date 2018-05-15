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

package io.mfj.textricator.text

import java.beans.Transient

/**
 * Extricated text.
 *
 * Positions and dimensions are in points.
 * x=0 is the left edge.
 * y=0 is the top edge.
 *
 * @property content The text.
 * @property pageNumber Page Number
 * @property ulx upper-left x-coordinate
 * @property uly upper-left y-coordinate
 * @property lrx lower-right x-coordinate
 * @property lry lower-right y-coordinate
 * @property font Font name
 * @property fontSize Font size, in points
 * @property color Text color
 * @property background Background color
 *
 * @property width Width
 * @property height Height
 */
data class Text(
    val content:String,
    val pageNumber:Int,
    val ulx:Float,
    val uly:Float,
    val lrx:Float,
    val lry:Float,
    val font:String,
    val fontSize:Float,
    val color:String?,
    val backgroundColor:String? ) {

  val width:Float
    @Transient
    get() = lrx - ulx;

  val height:Float
    @Transient
    get() = uly - lry;
}
