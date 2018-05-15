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
import io.mfj.textricator.form.config.FormParseConfigUtil

import java.io.ByteArrayOutputStream

import org.junit.*
import org.junit.Assert.*

class CsvOutputTest {

  @Test
  fun test() {
    val model = FormParseConfigUtil.parseYaml( """
rootRecordType: person
recordTypes:
  person:
    label: "Person"
    valueTypes:
      - name
      - dob
    children:
      - case
      - address
  case:
    label: "Case"
    valueTypes:
      - caseId
      - date
    children:
      - fee
  fee:
    label: "Fee"
    valueTypes:
      - desc
      - amount
  address:
    label: "Address"
    valueTypes:
      - city
      - state
valueTypes:
  name:
    label: "Name"
  dob:
    label: "Date of Birth"
  caseId:
    label: "Case ID"
  date:
    label: "Date"
  desc:
    label: "Fee Description"
  amount:
    label: "Fee Amount"
states:
  name: {}
  dob: {}
  caseId: {}
  date: {}
  desc: {}
  amount: {}
  city: {}
""" )

    val root = Record(1, "person", mutableMapOf("name" to "John Doe", "dob" to "1/1/1980"),
        mutableMapOf("case" to mutableListOf(
            Record(1, "case", mutableMapOf("caseId" to "1", "date" to "1/1/2018")),
            Record(1, "case", mutableMapOf("caseId" to "2", "date" to "3/3/2018"),
                mutableMapOf("fee" to mutableListOf(Record(1, "fee",
                    mutableMapOf("desc" to "court fee", "amount" to "99.95"))))),
            Record(1, "case", mutableMapOf("caseId" to "3", "date" to "7/1/2018"),
                mutableMapOf("fee" to mutableListOf(
                    Record(1, "fee", mutableMapOf("desc" to "court fee", "amount" to "10")),
                    Record(1, "fee",
                        mutableMapOf("desc" to "jail fee", "amount" to "5.99")))))), "address" to mutableListOf(
            Record(1, "address", mutableMapOf("city" to "Rochester", "state" to "NY")),
            Record(1, "address", mutableMapOf("city" to "Pittsburgh", "state" to "PA")))))

    val buffer = ByteArrayOutputStream()
    CsvRecordOutput(model,buffer).use { it.write(sequenceOf(root) ) }

    val expected = """
page,Name,Date of Birth,Case ID,Date,Fee Description,Fee Amount,city,state
1,John Doe,1/1/1980,1,1/1/2018,,,,
1,John Doe,1/1/1980,2,3/3/2018,court fee,99.95,,
1,John Doe,1/1/1980,3,7/1/2018,court fee,10,,
1,John Doe,1/1/1980,3,7/1/2018,jail fee,5.99,,
1,John Doe,1/1/1980,,,,,Rochester,NY
1,John Doe,1/1/1980,,,,,Pittsburgh,PA
"""

    assertEquals( expected.trim(), String(buffer.toByteArray()).trim())

  }

}
