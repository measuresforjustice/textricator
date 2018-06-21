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

import io.mfj.textricator.form.RecordParser
import io.mfj.textricator.form.StateValue
import io.mfj.textricator.record.Record
import io.mfj.textricator.record.RecordType
import io.mfj.textricator.form.config.FormParseConfig
import io.mfj.textricator.form.config.State

import org.junit.Assert.*
import org.junit.Test

class RecordParserTest {

  val model = FormParseConfig(rootRecordType = "date", recordTypes = mutableMapOf(
      "date" to RecordType(label = "Date", children = mutableListOf("person"), valueTypes = mutableListOf("date")),
      "person" to RecordType(label = "Person", children = mutableListOf("case"),
          valueTypes = mutableListOf("name", "dob")),
      "case" to RecordType(label = "Case", children = mutableListOf("charge"),
          valueTypes = mutableListOf("caseId", "court")),
      "charge" to RecordType(label = "Charge", valueTypes = mutableListOf("chargeCode", "chargeDesc"))),
      states = mutableMapOf(
          "date" to State(startRecord = true, startRecordForEachValue = true),
          "name" to State(startRecord = true),
          "dob" to State(),
          "caseId" to State(startRecord = true),
          "court" to State(),
          "chargeCode" to State(startRecord = true),
          "chargeDesc" to State()))

  val rp = RecordParser(model)

  private fun sv( pageNumber:Int, stateId:String, vararg value:String ):StateValue = StateValue(
      pageNumber = pageNumber, stateId = stateId,
      state = model.states[stateId] ?: throw Exception("Missing state ${stateId}"), values = value.toList())

  @Test
  fun testStructure() {

    val stateValues:List<StateValue> = listOf(
        sv( 1, "date", "2018-01-01" ),
        sv( 1, "date", "2018-01-02" ),
        sv( 1, "name", "John Doe" ),
        sv( 1, "dob", "1980-01-01" ),
        sv( 1, "caseId", "CASE001" ),
        sv( 1, "court", "Kangaroo" ),
        sv( 1, "chargeCode", "AGGRAV ASSLT" ),
        sv( 1, "chargeDesc", "W DEADLY WEAPON" ),
        sv( 1, "chargeCode", "AGGRAV BATTERY" ),
        sv( 1, "chargeDesc", "BODILY HARM" ),
        sv( 1, "caseId", "CASE002" ),
        sv( 1, "court", "Kangaroo" ),
        sv( 1, "chargeCode", "MOVING VIOL" ),
        sv( 1, "chargeDesc", "30+" ),
        sv( 1, "chargeCode", "NONMOVING VIOL" ),
        sv( 1, "chargeDesc", "NO LICENSE" ),
        sv( 1, "name", "Jane Doe" ),
        sv( 1, "dob", "1990-01-01" ),
        sv( 1, "caseId", "CASE001" ),
        sv( 1, "court", "Kangaroo" ),
        sv( 1, "chargeCode", "VEH THEFT" ),
        sv( 1, "chargeDesc", "GRAND 3RD" ),
        sv( 1, "chargeCode", "NONMOVING VIOL" ),
        sv( 1, "chargeDesc", "NO LICESNSE" )
    )

    val records = rp.parse( stateValues.asSequence() ).toList()

    validateRecordsAgainstModel( records )
  }

  private fun validateRecordsAgainstModel( records:List<Record> ) {
    records.forEach { record ->
      assertEquals( "Root records must be of type \"${model.rootRecordType}\" -  found \"${record.typeId}\"",
          model.rootRecordType, record.typeId )

      validateRecordAgainstModel( record )
    }
  }

  private fun validateRecordAgainstModel( record:Record) {
    val childAllowedTypes = model.recordTypes[record.typeId]!!.children.toSet()

    record.children.keys.forEach { childType ->
      assertTrue( "Records of Type \"${record.typeId}\" may only contain children of types ${childAllowedTypes} - found \"${childType}\"",
          childAllowedTypes.contains( childType ) )
    }
    record.children.values.forEach { children ->
      children.forEach { child ->
        validateRecordAgainstModel( child )
      }
    }
  }


  @Test
  fun testAncestorThatIsAChildOf() {
    assertEquals( "person", rp.findAncestorThatIsAChildOf("person","date") )
    assertEquals( "person", rp.findAncestorThatIsAChildOf("case","date") )
    assertEquals( "person", rp.findAncestorThatIsAChildOf("charge","date") )
    assertEquals( "case", rp.findAncestorThatIsAChildOf("case","person") )
    assertEquals( "case", rp.findAncestorThatIsAChildOf("charge","person") )
    assertEquals( "charge", rp.findAncestorThatIsAChildOf("charge","case") )
  }

  /**
   * Make sure that if a state that is required for another state to start a new record
   * ([State.startRecordRequiredState])
   * has [State.include] = false
   * that the state is still still noticed for the purposes of [State.startRecordRequiredState].
   *
   * Previously, the [StateValue]s with [State.include]=false were filtered out too early and this did not work.
   */
  @Test
  fun testStartRecordIfSeenIgnoredState() {
    val model = FormParseConfig(rootRecordType = "person",
        recordTypes = mutableMapOf(
            "person" to RecordType(label = "Person", valueTypes = mutableListOf("name"))
        ),
        states = mutableMapOf(
            "name" to State(startRecord = true, startRecordRequiredState = "label"),
            "label" to State(include = false)
        )
    )
    val stateValues = listOf(
        StateValue(
            pageNumber = 1,
            values = listOf( "Fred" ),
            state = model.states["name"]!!,
            stateId = "name"
        ),
        StateValue(
            pageNumber = 1,
            values = listOf( "label" ),
            state = model.states["label"]!!,
            stateId = "label"
        ),
        StateValue(
            pageNumber = 1,
            values = listOf( "Sally" ),
            state = model.states["name"]!!,
            stateId = "name"
        )
    ).asSequence()

    val records = RecordParser(model).parse( stateValues ).toList()

    assertEquals( 2, records.size )
    assertEquals( "Fred", records[0].values["name"] )
    assertEquals( "Sally", records[1].values["name"] )
  }

}
