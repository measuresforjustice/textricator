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

class PatternReplacementTest {
  val model = FormParseConfig(rootRecordType = "name", recordTypes = mutableMapOf(
      "name" to RecordType(label = "name",
          valueTypes = mutableListOf("inmateName", "inmateAge", "inmateRace", "arrestDateTime"))),
      valueTypes = mutableMapOf("inmateName" to ValueType(label = "Inmate Name"),
          "inmateAge" to ValueType(label = "Inmate Age",
              replacements = mutableListOf(PatternReplacement("30", "thirty"), PatternReplacement("40", "forty"))),
          "inmateRace" to ValueType(label = "Inmate Race",
              replacements = mutableListOf(PatternReplacement("W", "white"), PatternReplacement("W", "black"))),
          "arrestDateTime" to ValueType(label = "Arrest Date Time", replacements = mutableListOf(
              PatternReplacement(pattern = "(.*)\\ 12/30/1899\\ (.*)", replacement = "$1 $2")))),
      states = mutableMapOf("inmateName" to State(startRecord = true),
          "inmateAge" to State(), "inmateRace" to State(),
          "arrestDateTime" to State()))

  val rp = RecordParser(model)

  private fun sv(pageNumber: Int, stateId: String, vararg value: String) :StateValue = StateValue(
    source = "test",
    pageNumber = pageNumber, stateId = stateId,
      state = model.states[stateId] ?: throw Exception("Missing State: ${stateId}"), values = value.toList().map{ Value(it) })

  @Test
  fun testPatternReplacement() {
    val stateValues: List<StateValue> = listOf(
        sv(1, "inmateName", "John Doe"),
        sv(1, "inmateAge", "30"),
        sv(1, "inmateRace", "W"),
        sv(1, "arrestDateTime", "02/15/2018 12/30/1899 03:33:00 AM"),
        sv(1, "inmateName", "Jane Doe"),
        sv(1, "inmateAge", "40"),
        sv(1, "inmateRace", "W"),
        sv(1, "arrestDateTime", "02/13/2018 05:55:00 PM")
    )

    val records = rp.parse(stateValues.asSequence()).toList()

    // test that removing 12/30/1899 from arrestDateTime works
    assertEquals("02/15/2018 03:33:00 AM", records[0].values["arrestDateTime"]?.text)

    // test that nothing is removed from arrestDateTime when 12/30/1899 isn't there
    assertEquals("02/13/2018 05:55:00 PM", records[1].values["arrestDateTime"]?.text)

    // test that correct replacement is used when there are multiple patterns as options
    assertEquals("thirty", records[0].values["inmateAge"]?.text)
    assertEquals("forty", records[1].values["inmateAge"]?.text)

    // test that first replacement is used when there are multiple matching patterns
    assertEquals("white", records[0].values["inmateRace"]?.text)
    assertEquals("white", records[1].values["inmateRace"]?.text)
  }
}
