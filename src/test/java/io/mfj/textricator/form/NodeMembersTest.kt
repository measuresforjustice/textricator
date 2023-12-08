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

package io.mfj.textricator.form

import io.mfj.textricator.form.config.FormParseConfig
import io.mfj.textricator.form.config.State
import io.mfj.textricator.record.PatternReplacement
import io.mfj.textricator.record.RecordType
import io.mfj.textricator.record.Value
import io.mfj.textricator.record.ValueType

import org.junit.Assert.*
import org.junit.Test

class NodeMembersTest {
  val model = FormParseConfig(rootRecordType = "name", recordTypes = mutableMapOf(
      "name" to RecordType(label = "name",
          valueTypes = mutableListOf("inmateName", "city", "state", "inmateDOB", "arrestDate"))),
      valueTypes = mutableMapOf("inmateName" to ValueType(label = "Inmate Name"),
          "city" to ValueType(label = "City", replacements = mutableListOf(PatternReplacement("(.*),.*", "$1"))),
          "state" to ValueType(label = "State", replacements = mutableListOf(PatternReplacement(".*,(.*)", "$1"))),
          "inmateDOB" to ValueType(label = "Inmate DOB"), "arrestDate" to ValueType(label = "Arrest Date")),
      states = mutableMapOf("inmateName" to State(startRecord = true),
          "cityState" to State(valueTypes = mutableListOf("city", "state")),
          "inmateDOB" to State(),
          "arrestDate" to State()))

  val rp = RecordParser(model)

  private fun sv(pageNumber: Int, stateId: String, vararg value: String) :StateValue = StateValue(
      source = "test",
      pageNumber = pageNumber, stateId = stateId,
      state = model.states[stateId] ?: throw Exception("Missing State: ${stateId}"), values = value.toList().map{ Value(it) })

  @Test
  fun testNodeMembers() {
    val stateValues: List<StateValue> = listOf(
        sv(1, "inmateName", "John Doe"),
        sv(1, "cityState", "New York,NY"),
        sv(1, "inmateDOB", "01/01/1985"),
        sv(1, "arrestDate", "02/15/2018"),
        sv(1, "inmateName", "Jane Doe"),
        sv(1, "cityState", "Rochester,NY"),
        sv(1, "inmateDOB", "12/12/1975"),
        sv(1, "arrestDate", "02/13/2018")
    )

    val records = rp.parse(stateValues.asSequence()).toList()

    assertEquals("New York", records[0].values["city"]?.text)
    assertEquals("NY", records[0].values["state"]?.text)
    assertEquals("Rochester", records[1].values["city"]?.text)
    assertEquals("NY", records[1].values["state"]?.text)

  }
}
