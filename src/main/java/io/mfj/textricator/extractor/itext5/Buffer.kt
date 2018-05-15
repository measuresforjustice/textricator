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

/** Buffer for accumulating values when parsing. */
internal data class Buffer (
    val pageNumber:Int,
    val ulx:Float,
    val uly:Float,
    var lrx:Float,
    var lry:Float,
    val font:String,
    val fontSize:Float,
    val fontColor:String?,
    val content:StringBuffer = StringBuffer())
