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

package io.mfj.textricator.text

class RowGrouper( private val maxRowDistance:Float ) {

  private class GroupByRowState {
    var buffer:MutableList<Text> = mutableListOf()
    var lastPage:Int = 1
  }

  fun group( source:Sequence<Text> ): Sequence<Text> {

    val groupByRowState = GroupByRowState()

    return source
        .plus( null as Text? ) // sentinal
        .map { text -> groupByRow( groupByRowState, text ) }
        .filter { it != null }.map { it!! }
        .flatten()
  }

  private fun groupByRow( s:GroupByRowState, text:Text? ): Sequence<Text>? {

    // collect until find a pageNumber break or gap of $maxRowDistance, then flush

    if ( text == null ) {
      // sentintal
      // flush and done.
      return s.buffer.sortedBy( Text::ulx ).asSequence()
    }

    val ret:MutableList<Text> = mutableListOf()

    if ( text.pageNumber != s.lastPage ) {
      // new pageNumber
      ret.addAll( s.buffer.sortedBy( Text::ulx ) )
      s.buffer = mutableListOf()
    }

    if ( s.buffer.isEmpty() ) {
    } else {
      val last = s.buffer.last()
      if ( text.uly - last.uly > maxRowDistance ) {
        ret.addAll( s.buffer.sortedBy( Text::ulx ).asSequence() )
        s.buffer = mutableListOf()
      }
    }

    s.buffer.add( text )
    s.lastPage = text.pageNumber

    return if ( ret.isNotEmpty() ) ret.asSequence() else null
  }

}

fun Sequence<Text>.groupRows(maxRowDistance:Float?): Sequence<Text> =
    if ( maxRowDistance != null ) {
      RowGrouper(maxRowDistance).group(this)
    } else {
      this
    }

fun Sequence<Page>.groupRowsPaged(maxRowDistance:Float?): Sequence<Page> =
    if ( maxRowDistance != null ) {
      this.map { page ->
        Page(page.pageNumber, page.texts.asSequence().groupRows(maxRowDistance).toList())
      }
    } else {
      this
    }
