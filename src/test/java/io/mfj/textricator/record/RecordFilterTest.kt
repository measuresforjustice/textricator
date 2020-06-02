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

package io.mfj.textricator.record

import io.mfj.expr.ExDataType
import io.mfj.textricator.form.config.FormParseConfig

import org.junit.Assert.*
import org.junit.Test

class RecordFilterTest {

  @Test
  fun testNoFilter() {

    val model = FormParseConfig(rootRecordType = "a", recordTypes = mutableMapOf(
        "a" to RecordType(label = "A", children = mutableListOf("b", "c"), valueTypes = mutableListOf("a1", "a2")),
        "b" to RecordType(label = "B", children = mutableListOf(), valueTypes = mutableListOf("b1", "b2")),
        "c" to RecordType(label = "C", children = mutableListOf("d"), valueTypes = mutableListOf("c1", "c2")),
        "d" to RecordType(label = "D", valueTypes = mutableListOf("d1", "d2"))))

    val records =
        listOf(Record(1, "a", mutableMapOf("a1" to Value("Hello"), "a2" to Value("World")), mutableMapOf(
            "b" to mutableListOf(Record(1, "b", mutableMapOf("b1" to Value("1"), "b2" to Value("2")))),
            "c" to mutableListOf(Record(1, "c", mutableMapOf("c1" to Value("1"), "c2" to Value("2")),
                mutableMapOf("d" to mutableListOf(
                    Record(1, "d", mutableMapOf("d1" to Value("1"), "d2" to Value("2")))))))))
        )

    val filtered = RecordFilter(model).filter(records.asSequence()).toList()

    assertEquals( records, filtered )

  }

  @Test
  fun testTopFilter() {

    val model = FormParseConfig(rootRecordType = "a", recordTypes = mutableMapOf(
        "a" to RecordType(label = "A", filter = """ a1 = "x" """, children = mutableListOf("b", "c"),
            valueTypes = mutableListOf("a1", "a2")),
        "b" to RecordType(label = "B", children = mutableListOf(), valueTypes = mutableListOf("b1", "b2")),
        "c" to RecordType(label = "C", children = mutableListOf("d"), valueTypes = mutableListOf("c1", "c2")),
        "d" to RecordType(label = "D", valueTypes = mutableListOf("d1", "d2"))))

    val records =
        listOf(Record(1, "a", mutableMapOf("a1" to Value("Hello"), "a2" to Value("World")), mutableMapOf(
            "b" to mutableListOf(Record(1, "b", mutableMapOf("b1" to Value("1"), "b2" to Value("2")))),
            "c" to mutableListOf(Record(1, "c", mutableMapOf("c1" to Value("1"), "c2" to Value("2")),
                mutableMapOf("d" to mutableListOf(
                    Record(1, "d", mutableMapOf("d1" to Value("1"), "d2" to Value("2")))))))))
        )

    val filtered = RecordFilter(model).filter(records.asSequence()).toList()

    assertTrue( filtered.isEmpty() )

  }

  @Test
  fun testChildFilter() {

    val model = FormParseConfig(rootRecordType = "a", recordTypes = mutableMapOf(
        "a" to RecordType(label = "A", filter = """ a1 = "Hello" and a2 = "World" """,
            children = mutableListOf("b", "c"), valueTypes = mutableListOf("a1", "a2")),
        "b" to RecordType(label = "B", children = mutableListOf(), valueTypes = mutableListOf("b1", "b2")),
        "c" to RecordType(label = "C", filter = """ c1 = "2" """, children = mutableListOf("d"),
            valueTypes = mutableListOf("c1", "c2")),
        "d" to RecordType(label = "D", valueTypes = mutableListOf("d1", "d2"))))

    val records =
        listOf(Record(1, "a", mutableMapOf("a1" to Value("Hello"), "a2" to Value("World")), mutableMapOf(
            "b" to mutableListOf(Record(1, "b", mutableMapOf("b1" to Value("1"), "b2" to Value("2")))),
            "c" to mutableListOf(Record(1, "c", mutableMapOf("c1" to Value("1"), "c2" to Value("2")),
                mutableMapOf("d" to mutableListOf(
                    Record(1, "d", mutableMapOf("d1" to Value("1"), "d2" to Value("2")))))))))
        )

    val expected =
        listOf(Record(1, "a", mutableMapOf("a1" to Value("Hello"), "a2" to Value("World")), mutableMapOf(
            "b" to mutableListOf(Record(1, "b", mutableMapOf("b1" to Value("1"), "b2" to Value("2")))),
            "c" to mutableListOf()))
        )

    val filtered = RecordFilter(model).filter(records.asSequence()).toList()

    assertEquals( expected, filtered )

  }

  @Test
  fun testChildFilterSome() {

    val model = FormParseConfig(rootRecordType = "a", recordTypes = mutableMapOf(
        "a" to RecordType(label = "A", filter = """ a1 = "Hello" and a2 = "World" """,
            children = mutableListOf("b", "c"), valueTypes = mutableListOf("a1", "a2")),
        "b" to RecordType(label = "B", children = mutableListOf(), valueTypes = mutableListOf("b1", "b2")),
        "c" to RecordType(label = "C", filter = """ c1 = "2" """, children = mutableListOf("d"),
            valueTypes = mutableListOf("c1", "c2")),
        "d" to RecordType(label = "D", valueTypes = mutableListOf("d1", "d2"))))

    val records =
        listOf(Record(1, "a", mutableMapOf("a1" to Value("Hello"), "a2" to Value("World")), mutableMapOf(
            "b" to mutableListOf(Record(1, "b", mutableMapOf("b1" to Value("1"), "b2" to Value("2")))),
            "c" to mutableListOf(Record(1, "c", mutableMapOf("c1" to Value("1"), "c2" to Value("2")),
                mutableMapOf("d" to mutableListOf(
                    Record(1, "d", mutableMapOf("d1" to Value("1"), "d2" to Value("2")))))),
                Record(1, "c", mutableMapOf("c1" to Value("2"), "c2" to Value("3")), mutableMapOf(
                    "d" to mutableListOf(
                        Record(1, "d", mutableMapOf("d1" to Value("1"), "d2" to Value("2")))))))))
        )

    val expected =
        listOf(Record(1, "a", mutableMapOf("a1" to Value("Hello"), "a2" to Value("World")), mutableMapOf(
            "b" to mutableListOf(Record(1, "b", mutableMapOf("b1" to Value("1"), "b2" to Value("2")))),
            "c" to mutableListOf(Record(1, "c", mutableMapOf("c1" to Value("2"), "c2" to Value("3")),
                mutableMapOf("d" to mutableListOf(
                    Record(1, "d", mutableMapOf("d1" to Value("1"), "d2" to Value("2")))))))))
        )

    val filtered = RecordFilter(model).filter(records.asSequence()).toList()

    assertEquals( expected, filtered )

  }

  @Test
  fun testTypes() {

    val model = FormParseConfig(rootRecordType = "a",
        recordTypes = mutableMapOf("a" to RecordType(label = "A", filter = """
                  str = "a string"
                  and
                  3 < int < 5
                  and
                  1.1 < dbl < 1.3
                  and
                  none = "member type not set"
                  and
                  undef = "member not defined"
                   """, valueTypes = mutableListOf("str", "int", "dbl", "none", "undef"), children = mutableListOf())),
        valueTypes = mutableMapOf("str" to ValueType(type = ExDataType.STRING.name.toLowerCase()),
            "int" to ValueType(type = ExDataType.INTEGER.name.toLowerCase()),
            "dbl" to ValueType(type = ExDataType.DOUBLE.name.toLowerCase()), "none" to ValueType( /* type not set */)))

    val records =
        listOf(Record(1, "a",
            mutableMapOf("str" to Value("a string"), "int" to Value("4"), "dbl" to Value("1.2"), "none" to Value("member type not set"),
                "undef" to Value("member not defined"))), Record(2, "a",
            mutableMapOf("str" to Value("wrong"), "int" to Value("4"), "dbl" to Value("1.2"), "none" to Value("member type not set"),
                "undef" to Value("member not defined"))), Record(3, "a",
            mutableMapOf("str" to Value("a string"), "int" to Value("6"), "dbl" to Value("1.2"), "none" to Value("member type not set"),
                "undef" to Value("member not defined"))), Record(4, "a",
            mutableMapOf("str" to Value("a string"), "int" to Value("4"), "dbl" to Value("1.7"), "none" to Value("member type not set"),
                "undef" to Value("member not defined"))), Record(5, "a",
            mutableMapOf("str" to Value("a string"), "int" to Value("4"), "dbl" to Value("1.2"), "none" to Value("wrong"),
                "undef" to Value("member not defined"))), Record(6, "a",
            mutableMapOf("str" to Value("a string"), "int" to Value("4"), "dbl" to Value("1.2"), "none" to Value("member type not set"),
                "undef" to Value("wrong")))
        )

    val filtered = RecordFilter(model).filter(records.asSequence()).toList()

    assertEquals( 1, filtered.size )
    assertEquals( records[0], filtered[0] )

  }
}
