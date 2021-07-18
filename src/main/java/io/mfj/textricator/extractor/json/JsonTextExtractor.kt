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

package io.mfj.textricator.extractor.json

import io.mfj.textricator.extractor.TextExtractor
import io.mfj.textricator.text.Text

import java.io.InputStream
import java.util.*

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

class JsonTextExtractor(input:InputStream):TextExtractor {

  private val texts:Map<Int,List<Text>>
  private val pageCount:Int

  // load into memory up front
  init {
    val mapper = ObjectMapper(JsonFactory()).registerKotlinModule()

    val typeFactory = mapper.typeFactory

    val type =
        typeFactory.constructCollectionType( LinkedList::class.java,
                mapper.constructType(Text::class.java ) )

    val list:List<Text> = mapper.readValue(input,type)

    val map:MutableMap<Int,MutableList<Text>> = mutableMapOf()
    list.forEach { text ->
      map
          .getOrPut( text.pageNumber, { mutableListOf() } )
          .add( text )
    }

    this.texts = map
    this.pageCount = texts.keys.maxOrNull() ?: 0
  }

  override fun getPageCount():Int {
    return pageCount
  }

  override fun extract(pageNumber:Int):List<Text> {
    return texts[pageNumber] ?: emptyList()
  }

  override fun close() {}

}
