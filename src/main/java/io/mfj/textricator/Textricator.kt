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

import io.mfj.textricator.table.TableParser
import io.mfj.textricator.table.config.TableParseConfig
import io.mfj.textricator.extractor.TextExtractor
import io.mfj.textricator.extractor.TextExtractorFactory
import io.mfj.textricator.extractor.TextExtractorOptions
import io.mfj.textricator.form.*
import io.mfj.textricator.form.config.FormParseConfig
import io.mfj.textricator.form.config.FormParseConfigUtil
import io.mfj.textricator.record.Record
import io.mfj.textricator.record.RecordFilter
import io.mfj.textricator.record.RecordModel
import io.mfj.textricator.record.output.*
import io.mfj.textricator.table.config.TableParseConfigUtil
import io.mfj.textricator.text.*
import io.mfj.textricator.text.output.CsvTextOutput
import io.mfj.textricator.text.output.JsonTextOutput
import io.mfj.textricator.text.output.TextOutput

import java.io.File
import java.io.InputStream
import java.io.OutputStream

import org.slf4j.LoggerFactory

/**
 * Textricator. This is the primary starting point.
 */
object Textricator {

  const val DEFAULT_PDF_PARSER = "pdf.itext5"

  const val TEXT_OUTPUT_FORMAT_CSV = "csv"
  const val TEXT_OUTPUT_FORMAT_JSON = "json"

  const val RECORD_OUTPUT_FORMAT_CSV = "csv"
  const val RECORD_OUTPUT_FORMAT_JSON = "json"
  const val RECORD_OUTPUT_FORMAT_JSON_FLAT = "json-flat"
  const val RECORD_OUTPUT_FORMAT_NULL = "null"
  const val RECORD_OUTPUT_FORMAT_XML = "xml"

  private val log = LoggerFactory.getLogger( Textricator::class.java )

  // extract text

  @JvmStatic
  fun extractText(
      input:InputStream, inputFormat:String,
      output:OutputStream, outputFormat:String,
      pageFilter:PageFilter = ALL_PAGES,
      textExtractorOptions:TextExtractorOptions = TextExtractorOptions(),
      maxRowDistance:Float = 0f ) {

    when ( outputFormat ) {
      TEXT_OUTPUT_FORMAT_CSV -> CsvTextOutput(output)
      TEXT_OUTPUT_FORMAT_JSON -> JsonTextOutput(output)
      else -> throw Exception( "Invalid output format: ${outputFormat}" )
    }.use { textOutput ->
      getExtractor( input, inputFormat, textExtractorOptions ).use { extractor ->
        extractText( extractor, textOutput, maxRowDistance, pageFilter )
      }
    }
  }

  @JvmStatic
  fun extractText(
      extractor:TextExtractor,
      output:TextOutput,
      maxRowDistance:Float = 0f,
      pageFilter:PageFilter = ALL_PAGES ) {

    output.write( extractText( extractor, maxRowDistance, pageFilter ) )

  }

  @JvmStatic
  fun extractText(
      extractor:TextExtractor,
      maxRowDistance:Float = 0f,
      pageFilter:PageFilter = ALL_PAGES ): Sequence<Text> {

    return extract( extractor, pageFilter ).groupRows( maxRowDistance )

  }

  // parse form to records

  @JvmStatic
  fun parseForm(
      inputFile:File,
      outputFile:File,
      configFile:File ) {

    val config = FormParseConfigUtil.parseYaml(configFile)
    val inputFormat = inputFile.extension.lowercase()
    val outputFormat = outputFile.extension.lowercase()

    inputFile.inputStream().use { input ->
      outputFile.outputStream().use { output ->

        parseForm(
            input, inputFormat,
            output, outputFormat,
            config )

      }

    }
  }

  @JvmStatic
  fun parseForm(
      inputFile:File,
      outputFile:File,
      config:FormParseConfig ) {

    val inputFormat = inputFile.extension.lowercase()
    val outputFormat = outputFile.extension.lowercase()

    inputFile.inputStream().use { input ->
      outputFile.outputStream().use { output ->

        parseForm(
            input, inputFormat,
            output, outputFormat,
            config )

      }

    }
  }

  @JvmStatic
  fun parseForm(
      input:InputStream, inputFormat:String,
      output:OutputStream, outputFormat:String,
      config:FormParseConfig,
      eventListener:FormParseEventListener = LoggingEventListener
      ) {

    getExtractor( input, inputFormat, config ).use { extractor ->
      getRecordOutput( outputFormat, output, config ).use { recordOutput ->
        parseForm( extractor, recordOutput, config, eventListener )
      }
    }
  }

  @JvmStatic
  fun parseForm(
      extractor:TextExtractor,
      recordOutput:RecordOutput,
      config:FormParseConfig,
      eventListener:FormParseEventListener = LoggingEventListener
  ) {
    val records = parseForm(
        extractor,
        config,
        eventListener )

    recordOutput.write( records )
  }

  @JvmStatic
  fun parseForm(
      extractor:TextExtractor,
      config:FormParseConfig,
      eventListener:FormParseEventListener = LoggingEventListener
  ): Sequence<Record> {

    // TextExtractor parses PDF to sequence of Text
    val textSeq = extract( extractor, config.pages.toPageFilter() ).groupRows( config.maxRowDistance )

    // FSMParser parse sequence of Text to sequence of StateValue
    val stateTextsSeq = FsmParser( config, eventListener ).parse( textSeq )

    // RecordParser parses sequence of StateValue to sequence of Record
    val recSeq = RecordParser( config, eventListener ).parse( stateTextsSeq )

    // RecordFilter filters out non-matching records.
    val filteredRecSeq = RecordFilter( config ).filter( recSeq )

    return filteredRecSeq
  }

  @JvmStatic
  private fun getRecordOutput( outputFormat:String, output:OutputStream, config:RecordModel):RecordOutput {
    return when ( outputFormat ) {
      RECORD_OUTPUT_FORMAT_CSV -> CsvRecordOutput(config,output)
      RECORD_OUTPUT_FORMAT_JSON -> JsonRecordOutput(config,output)
      RECORD_OUTPUT_FORMAT_JSON_FLAT -> JsonFlatRecordOutput(config,output)
      RECORD_OUTPUT_FORMAT_XML -> XmlRecordOutput(config,output)
      RECORD_OUTPUT_FORMAT_NULL -> NullOutput
      else -> throw IllegalArgumentException( "Unsupported output format \"${outputFormat}\"." )
    }
  }

  // parse table to records

  @JvmStatic
  fun parseTable(
      inputFile:File,
      outputFile:File,
      configFile:File ) {

    val config = TableParseConfigUtil.parseYaml(configFile)
    val inputFormat = inputFile.extension.lowercase()
    val outputFormat = outputFile.extension.lowercase()

    inputFile.inputStream().use { input ->
      outputFile.outputStream().use { output ->
        parseTable(
            input, inputFormat,
            output, outputFormat,
            config )
      }
    }
  }

  @JvmStatic
  fun parseTable(
      input:InputStream, inputFormat:String,
      output:OutputStream, outputFormat:String,
      config:TableParseConfig) {

    getExtractor( input, inputFormat, config ).use { extractor ->
      getRecordOutput(outputFormat,output,config).use { output ->
        parseTable( extractor, output, config )
      }
    }
  }

  @JvmStatic
  fun parseTable(
      extractor:TextExtractor,
      output:RecordOutput,
      config:TableParseConfig) {

    val records = parseTable( extractor, config )

    output.write( records )
  }

  @JvmStatic
  fun parseTable(
      extractor:TextExtractor,
      config:TableParseConfig): Sequence<Record> {

    // TextExtractor parses PDF to sequence of Text
    val pages = extractPages( extractor, config.pages.toPageFilter() ).groupRowsPaged( config.maxRowDistance )

    val recSeq = TableParser(config).parse( pages )

    // RecordFilter filters out non-matching records.
    val filteredRecSeq = RecordFilter(config).filter( recSeq )

    return filteredRecSeq
  }

  @JvmStatic
  fun getExtractor( input:InputStream, inputFormat:String, options:TextExtractorOptions ): TextExtractor {

    val extractorName:String =
        if ( options.extractor == null ) {
          inputFormat
        } else {
          val i = inputFormat.indexOf(".")
          val fileType = if ( i > 0 ) inputFormat.substring(0,i) else inputFormat
          if ( options.extractor!!.startsWith( "${fileType}." ) ) {
            options.extractor!!
          } else {
            // Using a different input format than [options.extractor] is set for.
            inputFormat
          }
        }
        .let { extractorName -> if ( extractorName == "pdf" ) DEFAULT_PDF_PARSER else extractorName }

    log.debug( "Using extractor \"${extractorName}\" for input format \"${inputFormat}\"." )

    return TextExtractorFactory
        .getFactory( extractorName )
        .create( input, options )
  }

  @JvmStatic
  fun extract( extractor:TextExtractor, pageFilter:PageFilter = ALL_PAGES ): Sequence<Text> =
      (1..extractor.getPageCount())
          .asSequence()
          .filter { pageNumber ->
            pageFilter( pageNumber )
          }
          .flatMap { pageNumber ->
            extractor.extract( pageNumber ).asSequence()
          }

  @JvmStatic
  fun extractPages( extractor:TextExtractor, pageFilter:PageFilter = ALL_PAGES ): Sequence<Page> =
      (1..extractor.getPageCount())
          .asSequence()
          .filter { pageNumber ->
            pageFilter( pageNumber )
          }
          .map { pageNumber ->
            Page( pageNumber, extractor.extract( pageNumber ) )
          }

}
