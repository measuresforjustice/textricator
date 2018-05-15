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

package io.mfj.textricator.table.config

import io.mfj.textricator.record.RecordModel
import io.mfj.textricator.record.RecordType
import io.mfj.textricator.extractor.TextExtractorOptions
import io.mfj.textricator.record.ValueType
import io.mfj.textricator.table.TableParser

// Value types are dynamically generated from [cols].

class TableParseConfig(
    var top:Float? = null,
    var bottom:Float? = null,
    var right:Float? = null,
    var cols:Map<String, Float>,
    var pageConfig:Map<Int, ConfigPage>? = null,
    val types:Map<String,ValueType> = emptyMap(),
    val filter:String? = null,
    extractor:String? = null,
    maxRowDistance: Float,
    pageFilter:String? = null ):
    TextExtractorOptions(
        extractor = extractor,
        maxRowDistance = maxRowDistance,
        pages = pageFilter ), RecordModel
{

  override val recordTypes:Map<String, RecordType>
    get() = mapOf( TableParser.ROOT_TYPE to
        RecordType(
            label = TableParser.ROOT_TYPE,
            valueTypes = cols.keys.toList(),
            filter = filter
        ) )

  override val rootRecordType:String
    get() = TableParser.ROOT_TYPE

  override val valueTypes:Map<String, ValueType>
    get() = cols
        .mapValues { (colName,_) ->
          types[colName] ?: ValueType(colName)
        }

  fun getTop(page:Int):Float? {
    return pageConfig?.get(page)?.top ?: top
  }

  fun getBottom(page:Int):Float? {
    return pageConfig?.get(page)?.bottom ?: bottom
  }

  fun getRight(page:Int):Float? {
    return pageConfig?.get(page)?.right ?: right
  }
}

data class ConfigPage(
    var top:Float? = null,
    var bottom:Float? = null,
    var right:Float? = null,
    var skip:Boolean = false)

