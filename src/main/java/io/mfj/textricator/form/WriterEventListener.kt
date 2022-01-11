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

import java.io.Writer

import org.slf4j.LoggerFactory

class WriterEventListener(private val w:Writer): FormParseEventListener {

  private val log = LoggerFactory.getLogger(LoggingEventListener::class.java)

  override fun onText(text:Text) {
    write("============================")
    write("text: \"${text.content}\"")
    write("\tpageNumber: ${text.pageNumber} ul:[ ${text.ulx} , ${text.uly} ] lr: [ ${text.lrx} , ${text.lry} ]")
    write("\tfont: ${text.font} - ${text.fontSize}")
    write("\tbgcolor: ${text.backgroundColor}")
  }

  override fun onHeader(text:Text) {
    write("\tpart of header. skip")
  }

  override fun onFooter(text:Text) {
    write("\tpart of footer. skip")
  }

  override fun onLeftMargin(text:Text) {
    write("\tpart of left gutter. skip")
  }

  override fun onRightMargin(text:Text) {
    write("\tpart of right gutter. skip")
  }

  override fun onExclude(text: Text, condition: String) {
    write("\texcluded by condition \"${condition}\"")
  }

  override fun onCheckTransition(currentState:String, condition:String, nextState:String) {
    write("\tcheck transition \"${condition}\" (\"${currentState}\" -> \"${nextState}\")...")
  }

  override fun onCheckTransition(currentState:String, condition:String, nextState:String, match:Boolean, message:String?) {
    write("\t\t${match} ${if (message != null) " (${message})" else "" }")
  }

  override fun onNoPrevious(source:String) {
    write("\t\tno previous [${source}]")
  }

  override fun onCheckCondition(source:String, description:String, match:Boolean) {
    write("\t\tcheck condition [${source}] ${description} : ${match}" )
  }

  override fun onPageStateChange(page:Int, state:String) {
    write( "State = ${state} (page:${page} reset)" )
  }

  override fun onStateChange(page:Int, state:String) {
    write( "State = ${state} (page:${page})" )
  }

  override fun onVariableSet(currentState:String, name:String, value:String?) {
    write( "Variable ${name} = ${if ( value != null ) "\"${value}\"" else "null" }" )
  }

  override fun onFsmEnd() {
    write( "fsm end" )
  }

  override fun onRecordsEnd() {
    write( "records end" )
  }

  override fun onStateValue(sv:StateValue) {
    write( "parse StateValue: ${sv.stateId} : ${sv.values}" )
  }

  override fun onNewRecord(typeId:String) {
    write( "\tnew ${typeId} record" )
  }

  override fun onRecordAppend(typeId:String) {
    write( "\tadd to ${typeId} record" )
  }


  var writes:Long = 0

  private fun write( s:String ) {
    try {
      w.appendln(s)
      if ( writes++ % 100L == 0L ) w.flush()
    } catch ( e:Exception ) {
      log.error(e.message,e)
    }
  }

}
