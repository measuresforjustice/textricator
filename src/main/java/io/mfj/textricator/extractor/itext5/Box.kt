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

import java.beans.Transient

/** A box drawn on the page. */
internal data class Box(
    val ulx:Float,
    val uly:Float,
    val lrx:Float,
    val lry:Float,
    val color:String?
) {
  val width:Float
    @Transient
    get() = lrx - ulx;
  val height:Float
    @Transient
    get() = uly - lry;
}
