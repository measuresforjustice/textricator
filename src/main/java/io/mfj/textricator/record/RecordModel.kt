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

/**
 * Defines the state machine
 */
interface RecordModel {
  val rootRecordType: String
  val recordTypes: Map<String, RecordType>
  val valueTypes: Map<String, ValueType>
}

data class RecordType(
    var label: String, // user defined name
    var children: List<String> = emptyList(), // List of child RecordType's ids
    var valueTypes: List<String> = emptyList(), // List of the data and cell leaf states at this level.
    var pagePriority: Int = 0, // for output, use the page from the type with the highest priority
    var filter:String? = null
)

open class ValueType(
    val label:String? = null,

    /** If true, remove duplicate values. */
    val unrepeat:Boolean = false,

    /** Separator when combining values. Values are combined after unrepeating */
    val separator:String = " ",

    /** Replacements to make. Replacements are mae after values are unrepeated and combined. */
    val replacements:MutableList<PatternReplacement>? = null,

    /** If false, do not include in output.
     * Useful when the same data is repeated for each child record and you need it to mark new records. */
    val include:Boolean = true,

    /** Data type (used for [RecordType.filter]). */
    val type:String? = null // ExDataType

) {

  fun calcValue( values:List<String> ): String =
      values
          .unrepeat()
          .joinToString(separator)
          .replace()

  private fun List<String>.unrepeat(): List<String> {
    if ( ! unrepeat ) return this
    (1..size).forEach { i ->
      if ( isRepeated( this, i ) ) {
        return this.subList(0,i).unrepeat()
      }
    }
    return this
  }

  private fun isRepeated( list:List<String>, pos:Int ): Boolean {
    if ( list.size < pos*2 ) return false
    (0 until pos).forEach { i ->
      if ( list[i] != list[pos+i] ) {
        return false
      }
    }
    return true
  }

  private fun String.replace(): String {
    // if there's no replacements, return the original string
    if ( replacements == null) {
      return this
    } else {
      // otherwise check all possible regex patterns for a match
      replacements.forEach { r ->
        // if the string matches a pattern, replace it with the regex replacement and return
        if( this.matches(r.regexPattern) ) {
          return r.regexPattern.replace(this, r.replacement)
        }
      }
    }

    // if the string matched no patterns, return the original string
    return this
  }

}

fun ValueType?.calculateValue( values:List<String> ) =
    if ( this != null ) this.calcValue(values) else values.joinToString(" ")

data class PatternReplacement(val pattern:String, val replacement:String ) {
  // lazy property to make regex from pattern
  val regexPattern by lazy {
    Regex(pattern)
  }
}
