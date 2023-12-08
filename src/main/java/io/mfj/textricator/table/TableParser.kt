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

package io.mfj.textricator.table

import io.mfj.textricator.table.config.TableParseConfig
import io.mfj.textricator.record.Record
import io.mfj.textricator.record.Value
import io.mfj.textricator.text.Page
import io.mfj.textricator.text.PageFilter
import io.mfj.textricator.text.Text
import io.mfj.textricator.text.toPageFilter

import org.slf4j.LoggerFactory

/**
 * Extract tabular data from a PDF where there is text in each "cell" that may overflow into the next cell.
 */
class TableParser( private val config:TableParseConfig) {

  companion object {
    private val log = LoggerFactory.getLogger( TableParser::class.java )

    internal val ROOT_TYPE = "row"
  }

  fun parse(pages:Sequence<Page>): Sequence<Record> {

    // just the start x-index of each column, sorted
    val cols = config.cols.values.sorted().toTypedArray()

    val pageFilter:PageFilter = config.pages.toPageFilter()

    return pages
        .filter { page -> pageFilter(page.pageNumber) }
        .flatMap { page ->

      val pageNumber = page.pageNumber

      // position filter
      val top:Float = config.getTop(pageNumber) ?: 0f
      val bottom:Float = config.getBottom(pageNumber) ?: Float.MAX_VALUE
      val pageLeft:Float = cols[0]
      val pageRight:Float = config.getRight(pageNumber) ?: Float.MAX_VALUE
      val positionFilter = { text:Text ->
        val y = text.uly
        val x = text.ulx
        ( y >= top ) && ( y <= bottom ) && ( x >= pageLeft ) && ( x <= pageRight )
      }

      // find the column based on x-value
      fun findCol( x:Float ):Int? {
        cols.forEachIndexed { colIndex, left ->
          val right = if ( colIndex+1 < cols.size ) {
            cols[colIndex+1]
          } else {
            pageRight
          }
          if ( x >= left && x < right ) {
            return colIndex
          }
        }
        return null
      }

      // table to add text to
      val table = Table(config.cols.size, config.maxRowDistance)

      // Run the extraction
      page.texts
          .filter ( positionFilter )
          .forEach { text ->
            findCol(text.ulx)?.let { colIndex ->
              log.debug( "p${pageNumber} y${text.uly} c${colIndex} ${text.content}" )
              table.addToCell(text.ulx, text.uly, colIndex, text.content)
            }
          }

      log.debug("processed page ${pageNumber}")

      table.getRows()
          .map { row ->
            createRecord( pageNumber, row )
          }

    }

  }

  private val valueTypes = config.valueTypes

  private fun createRecord( pageNumber:Int, row:Row):Record {

    // Create a single record per row.
    // It has no children, just values.

    val record = Record(source=null, pageNumber = pageNumber, typeId = ROOT_TYPE)

    config.cols.keys
        .forEachIndexed { i, colName ->
          val cell = row[i].map { text -> Value(text) }
          val value = valueTypes[colName]!!.calcValue( cell )
          record.values[colName] = value
        }

    return record
  }

}

internal typealias Row = List<Cell>
internal typealias Cell = List<String>
