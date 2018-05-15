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

package io.mfj.textricator.text.output

import io.mfj.textricator.text.Text

import java.io.OutputStream

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper

import org.slf4j.LoggerFactory

class JsonTextOutput(private val output:OutputStream):TextOutput {

  private val writer = ObjectMapper()
      .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET,false)
      .writer().withDefaultPrettyPrinter()

  companion object {
    private val log = LoggerFactory.getLogger(JsonTextOutput::class.java)
  }

  override fun close() {}

  override fun write(seq:Sequence<Text>) {

    write("[")
    var first = true
    seq.forEach { text ->
      if ( first ) first = false else output.write(",".toByteArray())
      writer.writeValue(output,text)
    }
    write("]")
  }

  private fun write(s:String) {
    output.write( s.toByteArray() )
  }

}
