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

package io.mfj.textricator.extractor

import java.io.InputStream
import java.util.*

interface TextExtractorFactory {

  fun create( input:InputStream, options:TextExtractorOptions):TextExtractor

  companion object {

    /** path to properties files in classpath that has name=fqcn. */
    private val FACTORY_PROPERTIES = "io/mfj/textricator/extractor/textExtractor.properties"

    /** Map of name->factory class, loaded from all io/mfj/unbox/factory.properties on classpath. */
    val classMap:Map<String,Class<TextExtractorFactory>> by lazy {
      TextExtractorFactory::class.java.classLoader.getResources(
          FACTORY_PROPERTIES)
          .asSequence()
          .map { url ->
            Properties().apply { url.openStream().use { load( it ) } }
                .entries
                .map { (k,v) ->
                  val name = k as String
                  val fqcn = v as String
                  val factoryClass:Class<TextExtractorFactory> = getFactoryClassFromFqcn(
                      fqcn)
                  Pair( name, factoryClass )
                }
          }
          .flatten()
          .toMap()
    }

    val extractorNames:Collection<String> by lazy { classMap.keys }

    /** Get the factory class for the specified FQCN. */
    private fun getFactoryClassFromFqcn( fqcn:String ): Class<TextExtractorFactory> =
        Class.forName(fqcn).let { class_ ->
          if ( TextExtractorFactory::class.java.isAssignableFrom( class_ ) ) {
            @Suppress("UNCHECKED_CAST")
            class_ as Class<TextExtractorFactory>
          } else {
            throw Exception( "\"${fqcn}\" does not implement \"${TextExtractorFactory::class.java.name}\"" )
          }
        }

    fun getFactory( name:String ):TextExtractorFactory {
      val factoryClass = classMap[name] ?: throw Exception( "No factory \"${name}\"" )
      val factory = factoryClass.newInstance()
      return factory
    }

  }

}
