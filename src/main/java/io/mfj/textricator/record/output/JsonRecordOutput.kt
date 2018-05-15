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

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import io.mfj.textricator.record.Record
import io.mfj.textricator.record.RecordModel

import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer

class JsonRecordOutput(config:RecordModel,output:OutputStream):RecordOutput {

  private val w = BufferedWriter(OutputStreamWriter(output))

  private val writer = ObjectMapper()
      .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET,false) // otherwise closes w
      .writer().withDefaultPrettyPrinter()

  override fun write( seq:Sequence<Record> ) {

    w.append( "[" )
    var first = true

    seq.forEach { rec ->

      if ( first ) {
        first = false
      } else {
        w.append( "," )
      }

      writeRec( w, rec )

    }

    w.append( "]" )
  }

  private fun writeRec( w:Writer, rec:Record) {
    writer.writeValue(w,rec)
  }

  override fun close() {
    w.close()
  }

}
