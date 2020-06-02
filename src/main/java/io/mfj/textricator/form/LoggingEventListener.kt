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
import org.slf4j.LoggerFactory

object LoggingEventListener: FormParseEventListener {

  private val log = LoggerFactory.getLogger( LoggingEventListener::class.java )

  override fun onText(text:Text) {
    if ( log.isDebugEnabled ) {
      log.debug("============================")
      log.debug("text: \"${text.content}\"")
      log.debug("\tpageNumber: ${text.pageNumber} ul:[ ${text.ulx} , ${text.uly} ] lr: [ ${text.lrx} , ${text.lry} ]")
      log.debug("\tfont: ${text.font} - ${text.fontSize}")
      log.debug("\tbgcolor: ${text.backgroundColor}")
      log.debug("\tlink: ${text.link}")
    }
  }

  override fun onHeader(text:Text) {
    log.debug("\tpart of header. skip")
  }

  override fun onFooter(text:Text) {
    log.debug("\tpart of footer. skip")
  }

  override fun onLeftMargin(text:Text) {
    log.debug("\tpart of left gutter. skip")
  }

  override fun onRightMargin(text:Text) {
    log.debug("\tpart of rigth gutter. skip")
  }

  override fun onCheckTransition(currentState:String, condition:String, nextState:String) {
    log.debug("\tcheck transition \"${condition}\" (\"${currentState}\" -> \"${nextState}\")...")
  }

  override fun onNoPrevious(source:String) {
    log.debug("\t\tno previous [${source}]")
  }

  override fun onCheckTransition(currentState:String, condition:String, nextState:String, match:Boolean, message:String?) {
    log.debug("\t\t${match} ${if (message != null) " (${message})" else "" }")
  }

  override fun onCheckCondition(source:String, description:String, match:Boolean) {
    log.debug("\t\tcheck condition [${source}] ${description} : ${match}" )
  }

  override fun onPageStateChange(page:Int, state:String) {
    log.debug( "State = ${state} (page:${page} reset)" )
  }

  override fun onStateChange(page:Int, state:String) {
    log.debug( "State = ${state} (page:${page})" )
  }

  override fun onVariableSet(currentState:String, name:String, value:String?) {
    log.debug( "Variable ${name} = ${if ( value != null ) "\"${value}\"" else "null" }" )
  }

  override fun onFsmEnd() {
    log.debug( "fsm end" )
  }

  override fun onRecordsEnd() {
    log.debug( "records end" )
  }

  override fun onStateValue(sv:StateValue) {
    log.debug( "parse StateValue: ${sv.stateId} : ${sv.values}" )
  }

  override fun onNewRecord(typeId:String) {
    log.debug( "\tnew ${typeId} record" )
  }

  override fun onRecordAppend(typeId:String) {
    log.debug( "\tadd to ${typeId} record" )
  }

}
