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

package io.mfj.textricator.examples

import io.mfj.textricator.Textricator
import io.mfj.textricator.form.config.FormParseConfigUtil
import io.mfj.textricator.table.config.TableParseConfigUtil

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.OutputStream

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Run the examples. They make pretty good tests.
 */
class ExamplesTest{
  private lateinit var name: String
  private lateinit var type: Type

  enum class Type { FORM, TABLE }

  companion object {

    /**
     * Examples.
     * Map of file name (without extension) to parse type.
     * For each of these, there must be a .pdf, a .yml, and a .csv in src/test/resources/io/mfj/textricator/examples/.
     */
    val examples = mapOf(
        "rap-sheet" to Type.FORM,
        "school-employee-list" to Type.FORM
    )

    @JvmStatic
    fun data() = examples.entries.map { (name,type) -> arrayOf( name, type ) }

    private fun compare( a:BufferedReader, b:BufferedReader ) = compare(
        a.lineSequence().iterator(), b.lineSequence().iterator())

    private fun compare( a:Iterator<String>, b:Iterator<String> ) {
      var line = 0
      while ( a.hasNext() ) {
        line++
        assertTrue( "Missing line @${line}" ) { b.hasNext() }
        val aline = a.next()
        val bline = b.next()
        assertEquals( aline, bline, "Incorrect line @${line}" )
      }
      line++
      assertFalse( "Extra line @${line}" ) { b.hasNext() }
    }
  }

  @ParameterizedTest
  @MethodSource("data")
  fun test(name: String, type: Type) {
    this.name = name
    this.type = type
    val outCsv = File.createTempFile( name, ".csv" )
    outCsv.deleteOnExit()
    try {
      // run textricator
      outCsv.outputStream().use { out ->
        ExamplesTest::class.java.getResourceAsStream( "${name}.yml" ).use { config ->
          ExamplesTest::class.java.getResourceAsStream( "${name}.pdf" ).use { pdf ->
            when ( type ) {
              Type.FORM -> testForm( config, pdf, out )
              Type.TABLE -> testTable( config, pdf, out )
            }
          }
        }
      }

      // make sure CSVs match
      outCsv.bufferedReader().use { b ->
        ExamplesTest::class.java.getResourceAsStream( "${name}.csv" ).bufferedReader().use { a ->
          compare(a, b)
        }
      }
    } finally {
      outCsv.delete()
    }

  }

  private fun testForm( configStream:InputStream, pdf:InputStream, out:OutputStream) {
    val config = FormParseConfigUtil.parseYaml( configStream )
    Textricator.parseForm(input = pdf, inputFormat = "pdf", output = out, outputFormat = "csv", config = config)
  }

  private fun testTable( configStream:InputStream, pdf:InputStream, out:OutputStream ) {
    val config = TableParseConfigUtil.parseYaml( configStream )
    Textricator.parseTable(input = pdf, inputFormat = "pdf", output = out, outputFormat = "csv", config = config)
  }

}