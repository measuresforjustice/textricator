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

import io.mfj.expr.*

/**
 * Filters records based on [RecordType.filter].
 *
 * @param config FormParseConfig
 */
class RecordFilter( private val config:RecordModel) {

  companion object {
    private val DEFAULT_TYPE:ExDataType = ExDataType.STRING
  }

  fun filter( seq:Sequence<Record> ): Sequence<Record> =
      seq.mapNotNull { record ->
        filter( record )
      }

  /** Map of value type id to ExDataType */
  private val valueTypeMap = config.valueTypes.map { (id,member) ->
    id to ( member.type?.let { ExDataType.valueOf( it.uppercase() ) } ?: DEFAULT_TYPE)
  }.toMap()

  /** Get the ExDataType for the specified member. */
  private fun getExDataType( valueTypeId:String ) = valueTypeMap[valueTypeId] ?: DEFAULT_TYPE

  /** Map of record type ID to the Expr to filter it, or null if no filter. */
  private val recordTypeToExpr:Map<String,Expr?> =
      config.recordTypes.entries
          .associate { (recordTypeId, recordType) ->
            recordTypeId to buildExpr(recordType)
          }

  /**
   * Build Expr for the specified record type. Null if no filter.
   */
  private fun buildExpr( type:RecordType): Expr? {
    val filter = type.filter
    return if ( filter != null ) {
      val vtp:VarTypeProvider = object:VarTypeProvider {
        override fun contains(varName:String):Boolean = type.valueTypes.contains(varName)
        override fun get(varName:String):ExDataType =
            if ( type.valueTypes.contains(varName) ) {
              getExDataType( varName )
            } else {
              throw IllegalArgumentException( "No such var \"${varName}\"" )
            }
        override fun getKnownVars():Map<String, ExDataType> =
            type.valueTypes.associateWith { getExDataType(it) }
      }
      ExprParser.parseToExpr( filter, vtp )
    } else {
      null
    }
  }

  /**
   * Filter the supplied record.
   * If the record's type's filter passes, return the record with the children also filtered.
   * If the record's type's filter does not pass, return null.
   */
  private fun filter( record:Record): Record? {
    return if ( evalFilter( record ) ) {
      filterChildren( record )
      record
    } else {
      null
    }
  }

  /**
   * Filter the children of the supplied record.
   */
  private fun filterChildren( record:Record) {
    record.children.values.forEach { children ->
      children.retainAll { child ->
        evalFilter( child )
      }
      children.forEach { child ->
        filterChildren( child )
      }
    }
  }

  /**
   * Evaluate the filter for the specified record.
   */
  private fun evalFilter( record:Record): Boolean =
      recordTypeToExpr[record.typeId]
          ?.value(
              object:VarProvider {
                override fun contains(varName:String):Boolean = record.values.contains( varName )
                override fun get(varName:String):Any? {
                  val exDataType = getExDataType(varName)
                  val s = record.values[varName]?.text // TODO filter based on other attributes
                  return ExConvert.convertStr( s, exDataType )
                }
                override fun getKnownVars():Set<String> = record.values.keys
              }
          )
          ?: true // accept if no filter

}
