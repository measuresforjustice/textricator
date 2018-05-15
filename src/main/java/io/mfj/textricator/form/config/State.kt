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

package io.mfj.textricator.form.config

import java.util.*

/**
 * A state in the state machine
 */

data class State(
    val include:Boolean = true,
    val transitions: MutableList<Transition> = ArrayList(),
    val startRecord: Boolean = false,
    val startRecordRequiredState: String? = null,
    val startRecordForEachValue:Boolean = false,
    val valueTypes: MutableList<String>? = null,
    val combineLimit: Float? = null,
    val setVariables: MutableList<VariableSet> = mutableListOf()
)
