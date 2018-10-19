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


internal class Table(private val numberOfColumns:Int, private val maxRowDistance:Float) {

  private val xtable:XTable = mutableMapOf()

  private fun createXRow():XRow =
      (0..numberOfColumns)
          .map { createXCell() }
          .toTypedArray()

  private fun createXCell():XCell = mutableMapOf()

  private fun createXValues():XValues = mutableListOf()


  /** add an object to the model */
  fun addToCell(x:Float, y:Float, columnIndex:Int, text:String) {
    val xrow = xtable.getOrPut( y, { createXRow() } )
    val xcell = xrow[columnIndex]
    val xvalues = xcell.getOrPut( x, { createXValues() } )
    xvalues.add( text )
  }

  fun getRows(): Sequence<Row> {

    return xtable
        .toSortedMap()
        .group()
        .asSequence()
        .map { xrow -> xrow.toRow() }
  }

  /**
   * Group rows that are within [maxRowDistance].
   *
   * This modifies the receiver!
   */
  private fun Map<Float, XRow>.group(): List<XRow> {

    val grouped:MutableList<XRow> = mutableListOf()

    var buffer:Pair<Float, XRow>? = null

    for ( ( y, row ) in entries ) {
      if ( buffer == null ) {
        // first row
        buffer = Pair(y,row)
      } else if ( buffer.first + maxRowDistance < y ) {
        // new row
        grouped.add( buffer.second )
        buffer = Pair(y,row)
      } else {
        // combine row with buffer.second
        row.forEachIndexed { index, cell ->

          // cell is a map of x-coords to value
          // if 2 identical x-coords, space-separate them
          val bufferCell = buffer.second[index]

          cell.entries.forEach { (x,value) ->
            bufferCell.getOrPut( x, { createXValues() } )
                .addAll( value )
          }
        }
      }
    }

    if ( buffer != null ) {
      grouped.add( buffer.second )
    }

    return grouped
  }

  private fun XRow.toRow():Row = map { xcell -> xcell.toCell() }

  // sort by x-coord, then combine the xvalues (which is already sorted by y-value)
  private fun XCell.toCell():Cell = toSortedMap().values.flatten().toList()

}

///// internal data structure

// map y-coord -> row
private typealias XTable = MutableMap<Float, XRow>

// cell index -> cell
private typealias XRow = Array<XCell>

// x-coord -> values
private typealias XCell = MutableMap<Float, XValues>

private typealias XValues = MutableList<String>
