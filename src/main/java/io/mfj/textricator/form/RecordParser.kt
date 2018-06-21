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

import io.mfj.textricator.record.Record
import io.mfj.textricator.record.calculateValue
import io.mfj.textricator.form.config.FormParseConfig
import io.mfj.textricator.form.config.State

import java.util.*

/**
 * Parses a sequence of StateValue
 * (probably the output of [FsmParser])
 * to a sequence of Record according to the config.
 *
 * @param config FormParseConfig
 */
class RecordParser( private val config:FormParseConfig, private val eventListener:RecordParserEventListener? =null ) {

  fun parse( seq:Sequence<StateValue> ): Sequence<Record> {
    val parseState = ParseState()

    return seq
        .map { stateTexts -> split(stateTexts) }.flatten()
        .map { stateValue -> splitValueTypes(stateValue) }.flatten()
        .plus( null as StateValue? ) // sentinel
        .mapNotNull { stateTexts ->
          parse( parseState, stateTexts )
        }
  }

  /**
   * If [State.startRecordForEachValue], split the StateValue
   * so parse can create new records for each
   */
  private fun split( st:StateValue): List<StateValue> =
      if ( st.state.startRecordForEachValue ) {
        st.values.map { value ->
          StateValue(st.pageNumber, st.stateId, st.state, listOf(value))
        }
      } else {
        listOf( st )
      }

  /**
   * If a State goes to multiple value types, split it
   * into one for each value type.
   */
  private fun splitValueTypes(st:StateValue): List<StateValue> =
      st.state.valueTypes
          ?.mapIndexed { index, valueTypeId ->
            val state = st.state.copy( valueTypes = mutableListOf( valueTypeId ) )
            StateValue(st.pageNumber, st.stateId, state, st.values, (index > 0))
          }
          ?: listOf( st )

  private class ParseState {
    val stateStack: Stack<Pair<String,Boolean>> = Stack() // <stateId,isNewRecord>
    var root: Record? = null

    // map of record to its member valueTypes.
    // We build this up as a list of strings, then collapse it to a single string before returning.
    // see extension function Record.setValueTypes()
    // Using IdentityHashMap is important - we need to distinguish between equal empty Records.
    val buffer: MutableMap<Record,MutableMap<String,MutableList<String>>> = IdentityHashMap()
  }

  private fun parse( s:ParseState, st:StateValue? ): Record? {

    if ( st == null ) {
      //log.debug( "end of states" )
      eventListener?.onRecordsEnd()
      // sentinel
      return s.root?.setValues(s)
    }

    //log.debug( "parse ${st.state} - ${st.valueTypes}" )
    eventListener?.onStateValue( st )

    // figure out if we should start a new record
    val newRecord = ( ! st.splitContinuation ) && isNewRecord( s, st.stateId )

    if ( newRecord ) {
      s.stateStack.popThrough { it.second && it.first == st.stateId }
    }
    s.stateStack.push( Pair( st.stateId, newRecord ) )

    // return value, if a new record is started return the current root
    var ret:Record? = null

    // have to check this *after* the call to s.stateStack.push() so this state goes in the stack, even if not used.
    if ( st.state.include ) {

      // there is only one because of splitValueTypes()
      val valueTypeId:String = st.state.valueTypes?.get(0) ?: st.stateId

      // find the type the contains this state
      val recordTypeId:String = findRecordTypeId( valueTypeId )

      // if new
      val rec:Record = if ( newRecord ) {

        // if the type is root
        val rec:Record = if ( recordTypeId == config.rootRecordType) {

          // return the old one and start a new root
          ret = s.root?.setValues(s)
          s.buffer.clear()
          s.root = Record(st.pageNumber, recordTypeId)
          s.root!!

        } else {
          // find the parent record
          val parent = findParentRecord( s, st.pageNumber, recordTypeId )

          // start a new record
          val rec = Record(st.pageNumber, recordTypeId)
          parent.children.getOrPut( recordTypeId, { mutableListOf() } ).add( rec )
          rec
        }

        //log.debug( "new ${rec.typeId} record" )
        eventListener?.onNewRecord(rec.typeId)
        rec

      } else {
        if ( s.root == null ) {
          // not explicitly starting a new record, but no record exists.
          // create a new root to add to.
          // TODO is this okay?
          s.root = Record(st.pageNumber, config.rootRecordType)
        }

        // find the root
        val rec = findRecord( s.root!!, recordTypeId )
        //log.debug( "add to ${rec.typeId} record" )
        eventListener?.onRecordAppend( rec.typeId )
        rec
      }

      // add data
      s.buffer
          .getOrPut( rec, {mutableMapOf()} )
          .getOrPut( valueTypeId, {mutableListOf()} )
          .addAll( st.values )
    }

    return ret
  }

  private val valueTypes = config.valueTypes

  /**
   * Set the valueTypes of the members from the buffer.
   */
  private fun Record.setValues( s:ParseState):Record {
    val recBuffer = s.buffer.get(this)
    if ( recBuffer != null ) {
      recBuffer.forEach { valueTypeId, values ->
        val value = valueTypes[valueTypeId].calculateValue(values)
        this.values[valueTypeId] = value
      }
    }
    this.children.forEach { childRecordTypeId, children ->
      children.forEach { child ->
        child.setValues(s)
      }
    }
    return this
  }


  private fun isNewRecord( s:ParseState, stateId:String ): Boolean {

    // Decide if a new record should be started

    // It is more complex than just "does this state start a new record", because of this case:
    // Imagine PDF that has records that look like this:

    // Jonathan Zachary			  Defendant pled to something or other blah blah blah says he didn't mean to blah blah
    // Johnson							  hurt anybody blah blah blah blah blah blah blah blah blah police brutality blah blah
    //                        blah waste of taxpayer money blah blah only 2 ounces blah blah blah blah blah blah
    // Case Type: F						blah.
    //
    // John Doe								This is the next record blah blah blah

    // "Johnson" on the 2nd line is part of the name, so we transition from the "notes" (the explanation at the right)
    // state to the "name" state, but we should not start a new record as you would transitioning from "notes" for
    // "so windy" to "name" for "John Doe"
    // The way we handle this is to require entering a specific state (i.e. case_type) before starting a new record.

    // To do this, we keep a stack of states (a pair of a string (the state) and a boolean for if it is a new record)

    // "Jonathon Zackary" - push "name"/true
    // "Defendant pled...." - push "notes"/false
    // "Johnson" - push "name"/false
    // "hurt anybody..." - push "notes"/false
    // "blah waste..." - push "notes"/false
    // "F" - push "case_type"/false
    // "blah." - push "notes"/false
    // "John Doe" - start new record, pop through last "name"/true (from "Jonathon Zackary"), push "name"/true

    val state = config.states[stateId] ?: throw Exception( "Missing state \"${stateId}\"" )

    if ( state.startRecordForEachValue ) return true

    return state.startRecord && ( state.startRecordRequiredState == null
        || s.stateStack.hasIntermediateState( state.startRecordRequiredState ) )
  }

  /** Map of value type ID to record type ID of the record that contains the value */
  private val valueTypeIdToRecordTypeId:Map<String,String> by lazy {
    val m:MutableMap<String,String> = mutableMapOf()
    config.recordTypes.forEach { ( recordTypeId, recordType ) ->
      recordType.valueTypes.forEach { valueTypeId ->
        m.put( valueTypeId, recordTypeId )
      }
    }
    m
  }

  private fun findRecordTypeId( valueTypeId:String ): String {
    return valueTypeIdToRecordTypeId[valueTypeId] ?: throw Exception( "No type containing value type \"${valueTypeId}\"" )
  }

  /**
   * Map of parent record type to
   * ( map of record type to which child of the parent record type contains the type )
   *
   * e.g.:
   * data record types:
   *         A
   *       /  \
   *     B     C
   *    / \  /  \
   *   D  E  F  G
   *  / \
   * H  I
   *
   * will result in:
   *  {
   * 		A: {
   * 	  	B > B, D > B, E > B, H > B, I > B,
   * 		  C > C, F > C, G > C
   * 		},
   * 	  B: {
   * 	  	D > D, H > D, I > D,
   * 	  	E > E
   * 	  }
   * 	  D: {
   *      H > H,
   *      I > I
   * 	  },
   * 	  C: {
   *      F > F,
   *      G > G
   * 	  }
   *  }
   */
  private val recordTypeToDescendantToChild:Map<String,Map<String,String>> by lazy {
    config.recordTypes.mapValues { ( _, type ) ->

      // descendant to child of type
      val m:MutableMap<String,String> = mutableMapOf()

      fun add( childTypeId:String, descendantTypeId:String ) {
        m.put( descendantTypeId, childTypeId )
        val descendantType = config.recordTypes[descendantTypeId] ?: throw Exception( "Missing type \"${descendantTypeId}\"" )
        descendantType.children.forEach { furtherDescendantTypeId ->
          add( childTypeId, furtherDescendantTypeId )
        }
      }

      type.children.forEach { childTypeId ->
        add( childTypeId, childTypeId )
      }

      m
    }
  }

  private infix fun String.findAncestorThatIsAChildOf( parent:String ): String? {
    val childRecordTypeId = recordTypeToDescendantToChild[parent]?.get( this )
    return childRecordTypeId
  }
  // wrapper for unit tests
  internal fun findAncestorThatIsAChildOf( type:String, parent:String ) = type findAncestorThatIsAChildOf parent

  /**
   * Find the Record for the typeId under the supplied root, creating child recs if necessary
   */
  private fun findRecord( rec:Record, recordTypeId:String ):Record {
    if ( rec.typeId == recordTypeId ) {
      // this is it
      return rec
    } else {
      val childTypeId = recordTypeId findAncestorThatIsAChildOf rec.typeId
      if ( childTypeId != null ) {
        // the desired record is either this child type or under it
        val children:MutableList<Record> = rec.children.getOrPut( childTypeId, { mutableListOf() } )
        if ( children.isEmpty() ) {
          children.add(Record(rec.pageNumber, childTypeId))
        }
        return findRecord( children.last(), recordTypeId )
      } else {
        throw Exception( "Cannot find record of type \"${recordTypeId}\" (under \"${rec.typeId}\")" )
      }
    }
  }

  private fun findParentRecord( s:ParseState, pageNumber:Int, recordTypeId:String ):Record {
    if ( s.root == null ) {
      s.root = Record(pageNumber, config.rootRecordType)
    }
    return findParentRecord( s.root!!, recordTypeId )
  }

  /**
   * Find the parent root for the recordTypeId under the supplied root, creating child recs if necessary
   */
  private fun findParentRecord( rec:Record, recordTypeId:String ):Record {
    if ( rec.typeId == recordTypeId) {
      throw Exception( "rec is type \"${recordTypeId}\"" )
    } else {

      if ( rec.children.containsKey(recordTypeId) ) return rec // optimization

      val childRecordTypeId = recordTypeId findAncestorThatIsAChildOf rec.typeId
      if ( childRecordTypeId != null ) {
        // the desired type is either this child type or under it

        if ( childRecordTypeId == recordTypeId) {
          // the desired type is this child type; return the parent
          return rec
        } else {
          // the desired type is a child of the child type
          val children:MutableList<Record> = rec.children.getOrPut( childRecordTypeId, { mutableListOf() } )
          if ( children.isEmpty() ) {
            children.add(Record(rec.pageNumber, childRecordTypeId))
          }
          val last = children.last()
          return if ( last.typeId == recordTypeId) {
            rec
          } else {
            findParentRecord( last, recordTypeId)
          }
        }

      } else {
        throw Exception( "Cannot find record of type \"${recordTypeId}\" (under \"${rec.typeId}\")" )
      }
    }
  }

}

/**
 * Determine if the state stack has the specified state since the latest new record.
 * If the latest new record is the latest record to have the specified state, returns false.
 */
fun Stack<Pair<String,Boolean>>.hasIntermediateState( state:String ): Boolean {
  val i = listIterator(size)
  while ( i.hasPrevious() ) {
    val p = i.previous()
    if ( p.second ) return false
    if ( p.first == state ) return true
  }
  return false
}

/**
 * Remove elements until an element matches the given predicate.
 */
fun <T> Stack<T>.popUntil( predicate: (T)->Boolean ) {
  while ( ! isEmpty() && ! predicate( peek() ) ) pop()
}

/**
 * Remove elements until an element matches the given predicate, and remove that element too.
 */
fun <T> Stack<T>.popThrough( predicate: (T)->Boolean ) {
  popUntil( predicate )
  if ( ! isEmpty() && predicate( peek() ) ) pop()
}
