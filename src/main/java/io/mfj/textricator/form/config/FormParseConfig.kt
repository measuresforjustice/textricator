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

import io.mfj.textricator.record.RecordModel
import io.mfj.textricator.record.RecordType
import io.mfj.textricator.extractor.TextExtractorOptions
import io.mfj.textricator.record.ValueType

/**
 * Defines the state machine
 */
class FormParseConfig(
    var states: MutableMap<String, State> = mutableMapOf(),
    var stateDefaults: State? = null,
    var conditions: MutableMap<String, String> = mutableMapOf(),
    var initialState: String = "INITIAL_STATE",
    var newPageState: String? = null,
    var header:DefaultAndPages = DefaultAndPages(),
    var footer:DefaultAndPages = DefaultAndPages(),
    var left:DefaultAndPages = DefaultAndPages(),
    var right:DefaultAndPages = DefaultAndPages(),
    override var rootRecordType: String = "root",
    override var recordTypes: Map<String, RecordType> = emptyMap(),
    override var valueTypes: Map<String, ValueType> = emptyMap(),
    extractor:String? = null,
    pages:String? = null,
    maxRowDistance:Float = 0f,
    boxPrecision:Float =0f,
    boxIgnoreColors:MutableSet<String> = mutableSetOf()
): TextExtractorOptions(
    extractor = extractor,
    boxPrecision =  boxPrecision,
    boxIgnoreColors = boxIgnoreColors,
    maxRowDistance = maxRowDistance,
    pages = pages ), RecordModel
