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

package io.mfj.textricator.form

import io.mfj.textricator.text.Text

interface FsmEventListener {

  fun onText( text:Text)

  fun onHeader( text:Text)
  fun onFooter( text:Text)
  fun onLeftMargin( text:Text)
  fun onRightMargin( text:Text)
  fun onExclude( text:Text, condition:String )

  fun onCheckTransition( currentState:String, condition:String, nextState:String )
  fun onCheckTransition( currentState:String, condition:String, nextState:String, match:Boolean, message:String? )

  fun onNoPrevious( source:String )
  fun onCheckCondition( source:String, description:String, match:Boolean )

  fun onPageStateChange( page:Int, state:String )

  fun onStateChange( page:Int, state:String )

  fun onVariableSet( currentState:String, name:String, value:String? )

  fun onFsmEnd()

}
