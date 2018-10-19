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

typealias PageFilter = (Int)->Boolean

val ALL_PAGES:PageFilter = { _:Int -> true }

/**
 * Parse pages text to PageFilter.
 *
 * @param pages Pages: e.g.: 1,3-5,100-103,400
 */
fun String?.toPageFilter():PageFilter {
  return if (this != null && isNotBlank()) {
    val ranges:List<(Int)->Boolean> = split(",").map { s ->
      if (s.contains("-")) {
        val a = s.split("-")
        val min = a[0].toInt()
        val max = intOrEnd(a[1])
        ;
        { page:Int ->
          (min..max).contains(page)
        }
      } else {
        val p = s.toInt();
        { page:Int ->
          (page == p)
        }
      }
    };
    { page:Int ->
      ranges.any { it(page) }
    }
  } else {
    ALL_PAGES
  }
}

private fun intOrEnd( s:String? ): Int = if ( s == null || s.isBlank() ) { Int.MAX_VALUE } else { s.toInt() }

fun PageFilter.and( pages:PageFilter):PageFilter = { i -> this(i) && pages(i) }

fun PageFilter.and(s:String?):PageFilter = this.and( s.toPageFilter() )
