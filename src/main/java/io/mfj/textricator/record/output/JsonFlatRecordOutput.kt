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

package io.mfj.textricator.record.output

import io.mfj.textricator.record.Record
import io.mfj.textricator.record.RecordModel
import io.mfj.textricator.record.RecordType
import io.mfj.textricator.record.ValueType

import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter

import java.util.concurrent.atomic.AtomicBoolean

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.channels.ReceiveChannel

class JsonFlatRecordOutput(private val config:RecordModel, output:OutputStream):
    RecordOutput {

  private val w = BufferedWriter(OutputStreamWriter(output))

  private val valueTypes = config.valueTypes

  private val writer = ObjectMapper()
      .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET,false) // otherwise closes w
      .writer().withDefaultPrettyPrinter()

  var printHeader = true

  override fun write( seq:Sequence<Record> ) {

    w.append( "[" )

    val notFirst = AtomicBoolean(false)

    if ( printHeader ) {
      printHeader()
      notFirst.set(true)
    }

    seq.forEach { rec ->

      printRows(rec,notFirst)

    }

    w.append( "]" )
  }

  override suspend fun write(channel:ReceiveChannel<Record>) {
    w.append( "[" )

    val notFirst = AtomicBoolean(false)

    if ( printHeader ) {
      printHeader()
      notFirst.set(true)
    }

		for ( rec in channel ) {

      printRows(rec,notFirst)

    }

    w.append( "]" )
  }

  override fun close() {
    w.close()
  }

  private fun printHeader() {

    val row:MutableMap<String,String> = mutableMapOf()
    row["page"] = "page"

    fun print( recordType:RecordType) {
      recordType.valueTypes.forEach { valueTypeId ->
        val valueType:ValueType? = valueTypes[valueTypeId]
        if ( valueType?.include ?: true ) {
          val label = valueType?.label ?: valueTypeId
          row[valueTypeId] = label
        }
      }
      recordType.children
          .map { config.recordTypes[it] ?: throw Exception("missing type ${it}") }
          .forEach { childRecordType ->
            print( childRecordType )
          }
    }
    val rootRecordType = config.recordTypes[config.rootRecordType] ?: throw Exception("missing type ${config.rootRecordType}")
    print(rootRecordType)

    writer.writeValue(w,row)
  }

  private fun printRows(root:Record,notFirst:AtomicBoolean) {

    val map:MutableMap<RecordType, Record> = mutableMapOf()

    fun pr(rec:Record) {
      val recType = config.recordTypes[rec.typeId] ?: throw Exception( "Missing type ${rec.typeId}" )
      map[recType] = rec

      if ( rec.isLeaf ) {
        if ( notFirst.getAndSet(true) ) w.write(",")
        printRow(map)
      } else {
        rec.children.values.forEach { it.forEach { pr(it) } }
      }

      map.remove(recType)
    }

    pr(root)
  }

  private fun printRow( map:Map<RecordType, Record> ) {

    val row:MutableMap<String,String> = mutableMapOf()

    var pageNumber:Int? = null
    var pageNumberPriority:Int = -1

    fun printType( type:RecordType) {
      val rec = map[type]

      if ( pageNumber == null && rec != null ) pageNumber = rec.pageNumber
      if ( ( rec != null ) && ( pageNumber == null || type.pagePriority > pageNumberPriority ) ) {
        pageNumber = rec.pageNumber
        pageNumberPriority = type.pagePriority
      }

      type.valueTypes.forEach { valueTypeId ->
        val valueType = valueTypes[valueTypeId]
        if ( valueType?.include ?: true ) {
          val attribute = valueType?.attribute
          val value = rec?.getValue(valueTypeId,attribute) ?: ""
          row[valueTypeId] = value
        }
      }
      type.children
          .map { config.recordTypes[it] ?: throw Exception("missing type ${it}") }
          .forEach { childRecordType ->
            printType( childRecordType )
          }
    }
    val rootRecordType = config.recordTypes[config.rootRecordType] ?: throw Exception("missing type ${config.rootRecordType}")

    printType(rootRecordType)

    row["page"] = pageNumber.toString()

    writer.writeValue(w,row)
  }


}
