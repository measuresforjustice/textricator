/*
This file is part of Textricator.
Copyright 2018 Measures for Justice Institute.
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
import io.mfj.textricator.form.config.FormParseConfigUtil
import io.mfj.textricator.record.Value

import java.io.ByteArrayOutputStream

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class XmlOutputTest {

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

    val root = Record(1, "person", mutableMapOf("name" to Value("John Doe"), "dob" to Value("1/1/1980")),
        mutableMapOf("case" to mutableListOf(
            Record(1, "case", mutableMapOf("caseId" to Value("1"), "date" to Value("1/1/2018"))),
            Record(1, "case", mutableMapOf("caseId" to Value("2"), "date" to Value("3/3/2018")),
                mutableMapOf("fee" to mutableListOf(Record(1, "fee",
                    mutableMapOf("desc" to Value("court fee"), "amount" to Value("99.95")))))),
            Record(1, "case", mutableMapOf("caseId" to Value("3"), "date" to Value("7/1/2018")),
                mutableMapOf("fee" to mutableListOf(
                    Record(1, "fee", mutableMapOf("desc" to Value("court fee"), "amount" to Value("10"))),
                    Record(1, "fee",
                        mutableMapOf("desc" to Value("jail fee"), "amount" to Value("5.99"))))))), "address" to mutableListOf(
            Record(1, "address", mutableMapOf("city" to Value("Rochester"), "state" to Value("NY"))),
            Record(1, "address", mutableMapOf("city" to Value("Pittsburgh"), "state" to Value("PA"))))))

    val buffer = ByteArrayOutputStream()
    XmlRecordOutput(model,buffer).use { it.write(sequenceOf(root) ) }
    val str = String(buffer.toByteArray()).trim()
    val normalized = normalize(str)

    val expected = normalize( """
      <Records>
        <Record>
          <pageNumber>1</pageNumber>
          <typeId>person</typeId>
          <values>
            <name>
              <text>John Doe</text>
            </name>
            <dob>
              <text>1/1/1980</text>
            </dob>
          </values>
          <children>
            <case>
              <pageNumber>1</pageNumber>
              <typeId>case</typeId>
              <values>
                <caseId>
                  <text>1</text>
                </caseId>
                <date>
                  <text>1/1/2018</text>
                </date>
              </values>
            </case>
            <case>
              <pageNumber>1</pageNumber>
              <typeId>case</typeId>
              <values>
                <caseId>
                  <text>2</text>
                </caseId>
                <date>
                  <text>3/3/2018</text>
                </date>
              </values>
              <children>
                <fee>
                  <pageNumber>1</pageNumber>
                  <typeId>fee</typeId>
                  <values>
                    <desc>
                      <text>court fee</text>
                    </desc>
                    <amount>
                      <text>99.95</text>
                    </amount>
                  </values>
                </fee>
              </children>
            </case>
            <case>
              <pageNumber>1</pageNumber>
              <typeId>case</typeId>
              <values>
                <caseId>
                  <text>3</text>
                </caseId>
                <date>
                  <text>7/1/2018</text>
                </date>
              </values>
              <children>
                <fee>
                  <pageNumber>1</pageNumber>
                  <typeId>fee</typeId>
                  <values>
                    <desc>
                      <text>court fee</text>
                    </desc>
                    <amount>
                      <text>10</text>
                    </amount>
                  </values>
                </fee>
                <fee>
                  <pageNumber>1</pageNumber>
                  <typeId>fee</typeId>
                  <values>
                    <desc>
                      <text>jail fee</text>
                    </desc>
                    <amount>
                      <text>5.99</text>
                    </amount>
                  </values>
                </fee>
              </children>
            </case>
            <address>
              <pageNumber>1</pageNumber>
              <typeId>address</typeId>
              <values>
                <city>
                  <text>Rochester</text>
                </city>
                <state>
                  <text>NY</text>
                </state>
              </values>
            </address>
            <address>
              <pageNumber>1</pageNumber>
              <typeId>address</typeId>
              <values>
                <city>
                  <text>Pittsburgh</text>
                </city>
                <state>
                  <text>PA</text>
                </state>
              </values>
            </address>
          </children>
        </Record>
      </Records>
    """ )

    assertEquals( expected, normalized )

  }

  /**
   * Normalize the XML so we can compare 2 documents.
   */
  private fun normalize( input:String ):String {
    val mapper = XmlMapper()
    val tree = mapper.readTree(input)
    val normalized = mapper.writeValueAsString(tree)
    return normalized
  }

}
