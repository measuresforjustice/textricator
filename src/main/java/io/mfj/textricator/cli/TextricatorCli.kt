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

package io.mfj.textricator.cli

import io.mfj.textricator.*
import io.mfj.textricator.extractor.TextExtractorFactory
import io.mfj.textricator.extractor.TextExtractorOptions
import io.mfj.textricator.form.config.FormParseConfigUtil
import io.mfj.textricator.table.config.TableParseConfigUtil
import io.mfj.textricator.text.toPageFilter

import java.io.File
import java.io.InputStream

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.Level

import org.docopt.Docopt

import org.slf4j.LoggerFactory

/**
 * Command-line interface to [Textricator].
 *
 * Can just extract text or also run the form or table parser.
 */
object TextricatorCli {

  private val help = """
    Textricator

    Textricator extracts content from PDFs.

    "text" extracts the text from a PDF and outputs to CSV or JSON.
    "form" parse a form (using a finite state machine) and generate records.
    "table" parses a table and generates records.

    Output is to standard out if not specified.

    Usage:
      textricator text [--debug] [--pages=<pages>] [--max-row-distance=<max-row-distance>] [--box-precision=<points>] [--box-ignore-colors=<colors>] [--input-format=<input-format>] [--output-format=<output-format>] <input> [<output>]
      textricator form [--debug] --config=<config> [--pages=<pages>] [--input-format=<input-format>] [--output-format=<output-format>] <input> [<output>]
      textricator forms [--debug] --config=<config> --input-format=<input-format> [--output-format=<output-format>] <input-dir> [<output>]
      textricator table [--debug] --config=<config> [--pages=<pages>] [--input-format=<input-format>] [--output-format=<output-format>] <input> [<output>]
      textricator -h | --help
      textricator --version

    Options:
      --config PATH               Path to config file.
      --pages PAGES               Pages to include. E.g.: 1-4,5,9. Default: all pages.
      --max-row-distance POINTS   Order text boxes within this distance (points) by x-position. E.g.: 0.5. Default: no ordering.
      --box-precision POINTS      Consider text inside a box if it overflows by less than this many points (float).
      --ignore-box-colors COLORS  Ignore boxes of these colors (comma-separated).
      --input-format FORMAT       Input format. If not set, determine from file extension.
                                  Valid values: ${TextExtractorFactory.extractorNames.sorted().joinToString(", ")}
                                  ("pdf" is an alias for ${Textricator.DEFAULT_PDF_PARSER})
      --output-format FORMAT      Output format. If not set, determine from file extension.
                                  Valid values:
                                    ${Textricator.RECORD_OUTPUT_FORMAT_CSV} (default if output to standard out)
                                    ${Textricator.RECORD_OUTPUT_FORMAT_JSON}
                                    ${Textricator.RECORD_OUTPUT_FORMAT_JSON_FLAT}
                                    ${Textricator.RECORD_OUTPUT_FORMAT_XML}
                                    ${Textricator.RECORD_OUTPUT_FORMAT_NULL} (no output)
      --debug                     Enable debug logging
      --version                   Show version, copyright, and license information.
    """.trimIndent().trim()

  private fun Map<String,Any>.boolean( key:String ):Boolean = containsKey(key) && get(key) as Boolean
  private fun Map<String,Any>.file( key:String ):File? = get(key)?.toString()?.let { File(it) }
  private fun Map<String,Any>.string( key:String ):String? = get(key)?.toString()
  private fun Map<String,Any>.float( key:String ):Float? = get(key)?.toString()?.toFloat()

  @JvmStatic
  fun main(args: Array<String>) {

    val opts = Docopt(help)
        .withHelp(true)
        .withExit(true)
        .parse(args.toList())

    if (opts.boolean("--debug")) {
      ( LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger ).level = Level.DEBUG
    }

    try {
      when {
        opts.boolean("--version") -> version()
        opts.boolean("text") -> text(opts)
        opts.boolean("table") -> table(opts)
        opts.boolean("form") -> form(opts)
        opts.boolean("forms") -> forms(opts)
      }
    } catch ( e:SystemExitException) {
      System.err.println(e.message)
      System.exit(e.exitCode)
    }
  }

  private fun version() {
    println(
        """
        Textricator ${Version.version}
        Copyright ${Version.copyrightYear} Measures for Justice Institute.

        Licensed under the GNU Affero General Public License, Version 3.
        (Loaded modules may be licensed differently.)

        Source code is available at ${Version.sourceLocation}
        """.trimIndent()
    )
  }

  private fun text( opts:Map<String,Any> ) {

    val inputFile = opts.file("<input>")!!
    val inputFormat = opts.string("--input-format") ?: inputFile.extension.lowercase()

    val outputFile = opts.file("<output>")
    if ( outputFile != null ) outputFile.absoluteFile.parentFile.mkdirs()
    val outputFormat = opts.string("--output-format") ?:
        if ( outputFile == null ) {
          Textricator.TEXT_OUTPUT_FORMAT_CSV
        } else {
          outputFile.extension.lowercase()
        }

    val pages = opts.string("--pages").toPageFilter()
    val maxRowDistance = opts.float("--max-row-distance") ?: 0f
    val boxPrecision:Float = opts.float("--box-precision") ?: 0f
    val boxIgnoreColors:Set<String> = opts.string("--box-ignore-colors")?.split(",")?.toSet() ?: emptySet()

    val options = TextExtractorOptions(
        boxPrecision = boxPrecision,
        boxIgnoreColors = boxIgnoreColors )

    inputFile.inputStream().use { input ->

      ( if ( outputFile != null ) outputFile.outputStream() else System.out ).use { output ->

        Textricator.extractText(
            input = input,
            inputFormat = inputFormat,
            output = output,
            outputFormat = outputFormat,
            pageFilter = pages,
            textExtractorOptions = options,
            maxRowDistance = maxRowDistance )

      }

    }
  }

  private fun form( opts:Map<String,Any> ) {

    val inputFile = opts.file("<input>")!!
    val inputFormat = opts.string("--input-format") ?: inputFile.extension.lowercase()

    val outputFile = opts.file("<output>")
    if ( outputFile != null ) outputFile.absoluteFile.parentFile.mkdirs()
    val outputFormat = opts.string("--output-format") ?:
        if ( outputFile == null ) {
          throw SystemExitException( "--output-format is required if <output> is omitted.", 1 )
        } else {
          outputFile.extension.lowercase()
        }

    val configFile = opts.file("--config")!!

    val config = FormParseConfigUtil.parseYaml(configFile)

    opts.string("--pages")?.apply { config.pages = this }

    inputFile.inputStream().use { input ->

      ( if ( outputFile != null ) outputFile.outputStream() else System.out ).use { output ->

        Textricator.parseForm(
            input = input,
            inputFormat = inputFormat,
            output = output,
            outputFormat = outputFormat,
            config = config )

      }

    }


  }

  private fun forms( opts:Map<String,Any> ) {

    val inputDir = opts.file("<input-dir>")!!

    val inputFormat = opts.string("--input-format")!!
    val inputFormatUpper = inputFormat.uppercase()

    val outputFile = opts.file("<output>")
    if ( outputFile != null ) outputFile.absoluteFile.parentFile.mkdirs()
    val outputFormat = opts.string("--output-format") ?:
      if ( outputFile == null ) {
        throw SystemExitException( "--output-format is required if <output> is omitted.", 1 )
      } else {
        outputFile.extension.lowercase()
      }

    val configFile = opts.file("--config")!!

    val config = FormParseConfigUtil.parseYaml(configFile)

    val inputs:Sequence<Triple<String,()->InputStream,String>> = inputDir.walk().asSequence()
        .filter { file -> file.isFile }
        .filter { file -> file.extension.uppercase() == inputFormatUpper }
        .sorted()
        .map { inputFile ->
          Triple( inputFile.relativeTo(inputDir).path, { inputFile.inputStream() }, inputFormat )
        }

    ( outputFile?.outputStream() ?: System.out ).use { output ->
      Textricator.parseForms(
        inputs = inputs,
        output = output, outputFormat = outputFormat,
        config = config
      )
    }
  }

  private fun table( opts:Map<String,Any> ) {

    val inputFile = opts.file("<input>")!!
    val inputFormat = opts.string("--input-format") ?: inputFile.extension.lowercase()

    val outputFile = opts.file("<output>")
    if ( outputFile != null ) outputFile.absoluteFile.parentFile.mkdirs()
    val outputFormat = opts.string("--output-format") ?:
        if ( outputFile == null ) {
          throw SystemExitException( "--output-format is required if <output> is omitted.", 1 )
        } else {
          outputFile.extension.lowercase()
        }

    val configFile = opts.file("--config")!!

    val config = TableParseConfigUtil.parseYaml(configFile)

    opts.string("--pages")?.apply { config.pages = this }

    inputFile.inputStream().use { input ->

      ( if ( outputFile != null ) outputFile.outputStream() else System.out ).use { output ->

        Textricator.parseTable(
            input = input,
            inputFormat = inputFormat,
            output = output,
            outputFormat = outputFormat,
            config = config )

      }

    }

  }

  private class SystemExitException(message:String,val exitCode:Int): Exception(message)

}
