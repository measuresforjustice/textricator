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

import org.apache.commons.csv.*

import io.mfj.textricator.record.Record
import io.mfj.textricator.record.RecordModel
import io.mfj.textricator.record.RecordType

import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter

import kotlinx.coroutines.channels.ReceiveChannel

class CsvRecordOutput(private val config:RecordModel, output:OutputStream):RecordOutput {

  private val w = BufferedWriter(OutputStreamWriter(output))

  private val valueTypes = config.valueTypes

  val p = CSVPrinter(w, CSVFormat.DEFAULT.withRecordSeparator("\n"))

  var rowCount = 0

  override fun write( seq:Sequence<Record> ) {
    printHeader(p)
    seq.forEach { rec -> printRows(p,rec) }
  }

  override suspend fun write(channel:ReceiveChannel<Record>) {
    printHeader(p)
    for ( rec in channel ) {
      printRows(p,rec)
    }
  }

  override fun close() {
    p.flush()
    // do NOT close p, the caller is responsible for closing w, and that is all that p.close() does.
    //p.close()
    w.close()
  }

  private fun printHeader(p:CSVPrinter) {

    p.print("page")

    fun printType( recordType:RecordType) {
      recordType.valueTypes.forEach { valueTypeId ->
        val valueType = valueTypes[valueTypeId]
        if ( valueType?.include ?: true ) {
          val label = valueType?.label ?: valueTypeId
          p.print( label )
        }
      }
      recordType.children
          .map { config.recordTypes[it] ?: throw Exception("missing type ${it}") }
          .forEach { childRecordType ->
            printType( childRecordType )
          }
    }
    val rootRecordType = config.recordTypes[config.rootRecordType] ?: throw Exception("missing type ${config.rootRecordType}")
    printType(rootRecordType)

    p.println()
  }

  private fun printRows(p:CSVPrinter,root:Record) {

    val map:MutableMap<RecordType, Record> = mutableMapOf()

    fun pr(rec:Record) {
      val type = config.recordTypes[rec.typeId] ?: throw Exception( "Missing type ${rec.typeId}" )
      map[type] = rec

      if ( rec.isLeaf ) {
        printRow(p,map)
      } else {
        rec.children.values.forEach { it.forEach { pr(it) } }
      }

      map.remove(type)
    }

    pr(root)
  }

  private fun printRow( p:CSVPrinter, map:Map<RecordType, Record> ) {

    val cells:MutableList<Any> = mutableListOf()

    var pageNumber:Int? = null
    var pageNumberPriority:Int = -1

    fun printType( recordType:RecordType) {
      val rec = map[recordType]

      if ( ( rec != null ) && ( pageNumber == null || recordType.pagePriority > pageNumberPriority ) ) {
        pageNumber = rec.pageNumber
        pageNumberPriority = recordType.pagePriority
      }

      recordType.valueTypes.forEach { valueTypeId ->
        val valueType = valueTypes[valueTypeId]
        if ( valueType?.include ?: true ) {
          cells.add( rec?.values?.get(valueTypeId) ?: "" )
        }
      }
      recordType.children
          .map { config.recordTypes[it] ?: throw Exception("missing type ${it}") }
          .forEach { childRecordType ->
            printType( childRecordType )
          }
    }
    val rootRecordType = config.recordTypes[config.rootRecordType] ?: throw Exception("missing type ${config.rootRecordType}")

    printType(rootRecordType)

    p.print( pageNumber )
    p.printRecord( cells )

    if ( rowCount++ % 100 == 0 ) p.flush()
  }


}
