/*
This file is part of Textricator.
Copyright 2021 Stephen Byrne.

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

import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.xml.stream.XMLOutputFactory

import kotlinx.coroutines.channels.ReceiveChannel

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper

class XmlRecordOutput(config:RecordModel,output:OutputStream):RecordOutput {

  private val w = BufferedWriter(OutputStreamWriter(output))

  // First create Stax components we need
  private val xmlOutputFactory = XMLOutputFactory.newFactory()
  private val sw = xmlOutputFactory.createXMLStreamWriter(w)
  // then Jackson components
  private val mapper = XmlMapper()
      .apply {
        enable(SerializationFeature.INDENT_OUTPUT)
        setSerializationInclusion(JsonInclude.Include.NON_EMPTY) // omit empty elements e.g.: lots of <link />
      }

  init {
    sw.writeStartDocument();
    sw.writeStartElement("Records");
  }

  override fun write( seq:Sequence<Record> ) {
    seq.forEach { rec->
      mapper.writeValue(sw, rec);
    }
  }

  override suspend fun write(channel:ReceiveChannel<Record>) {
    for ( rec in channel ) {
      mapper.writeValue(sw,rec)
    }
  }

  override fun close() {
    try {
      sw.writeEndElement();
      sw.writeEndDocument();
      sw.close()
    } finally {
      w.close()
    }
  }

}
