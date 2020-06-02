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

package io.mfj.textricator.record

import java.beans.Transient

// cannot be a data class, because used as a map key in RecordParser.
class Record(
    val pageNumber:Int,
    val typeId:String,
    val values:MutableMap<String,Value> = mutableMapOf(),
    val children:MutableMap<String,MutableList<Record>> = mutableMapOf()
) {

  val isLeaf:Boolean
    @Transient
    get() {
      children.values.forEach { childList ->
        if ( childList.isNotEmpty() ) {
          return false
        }
      }
      return true
    }

  fun getValue( valueTypeId:String, attribute:String? ): String? = values[valueTypeId]?.getValue(attribute)

  override fun hashCode():Int = pageNumber + typeId.hashCode() + 17 * values.size + 27 * children.size

  override fun equals(other:Any?):Boolean = ( other != null ) && ( other is Record) &&
      pageNumber == other.pageNumber && typeId == other.typeId &&
      values.size == other.values.size && children.size == other.children.size &&
      values == other.values && children == other.children

}
