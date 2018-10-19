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

package io.mfj.textricator

import java.util.*

object Version {

  val version:String
  val copyrightYear:String
  val sourceLocation:String

  init {
    val props = Properties().apply {
      Version::class.java.getResourceAsStream( "version.properties" ).use { input ->
        load( input )
      }
    }
    version = props.getProperty("version")
    copyrightYear = props.getProperty("copyright.year")
    sourceLocation = props.getProperty("source.location")
  }

}
