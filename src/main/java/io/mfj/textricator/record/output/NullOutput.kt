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

package io.mfj.textricator.record.output

import io.mfj.textricator.record.Record

import kotlinx.coroutines.channels.ReceiveChannel

/**
 * Run through the records but do not do anything with them.
 */
object NullOutput:RecordOutput {
  override fun write(seq:Sequence<Record>) {
    seq.forEach {}
  }

  override suspend fun write(channel:ReceiveChannel<Record>) {
    for ( rec in channel ) {
    }
  }

  override fun close() {}
}
