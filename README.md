# ![Textricator](./textricator-logo-text-paths.png)

_Textricator_ is a tool to extract text from documents and generate structured data.

If you have a bunch of PDFs with the same format (or one big, consistently formatted PDF)
and you want to extract the data to CSV or JSON, _Textricator_ can help!
It can even work on OCR'ed documents!

_Textricator_ is released under the
[GNU Affero General Public License Version 3](https://www.gnu.org/licenses/agpl-3.0.en.html).

_Textricator_ is deployed to [Maven Central](https://repo1.maven.org/maven2/io/mfj/textricator/) with GAV `io.mfj:textricator`.

This application is actively used and developed by [Measures for Justice](https://measuresforjustice.org).
We welcome feedback, bug reports, and contributions. Create an issue, send a pull request,
or email us at <textricator@mfj.io>. If you use _Textricator_, please let us know.
Send us your mailing address and we will mail you a sticker.

`io.mfj.textricator.Textricator` is the main entry point for library usage.

`io.mfj.textricator.cli.TextricatorCli` is the command-line interface.

The CLI has three subcommands, to use the three main features of Textricator:
  * text - Extract text from the PDF and generate JSON.
  * table - Parse the text that is in columns and rows. See [Table](#table) section.
  * form - Parse the text with a configured finite state machine. See [Form](#form) section.

## Quick Start

- Install Java (version 8+)
  * Windows & Macos: Download from [https://java.com](https://java.com) and install.
  * Linux: Use your package manager.
- Download the latest build of _Textricator_ from [https://repo1.maven.org/maven2/io/mfj/textricator/](https://repo1.maven.org/maven2/io/mfj/textricator/) - click on the directory for the latest version and download `textricator-VERSION-bin.tgz` (or `textricator-VERSION-bin.zip` for Windows).
- Extract it.
- Run a shell
  * Windows: run _Windows Powershell_ (it should be in the start menu)
    * The following examples start with `./textricator`. On Windows, use `.\textricator.bat`.
  * MacOS: Run _Terminal_ (type "terminal" in Spotlight)
- Show help
    - `./textricator --help`
- Download the example files to the textricator directory:
  * https://github.com/measuresforjustice/textricator/blob/main/src/test/resources/io/mfj/textricator/examples/school-employee-list.pdf
  * https://github.com/measuresforjustice/textricator/blob/main/src/test/resources/io/mfj/textricator/examples/school-employee-list.yml
- Extract raw text from a PDF to standard out
    - `./textricator text --input-format=pdf.pdfbox school-employee-list.pdf`
- Parse a PDF to CSV
    - `./textricator form --config=school-employee-list.yml school-employee-list.pdf school-employee-list.csv`
      * This uses the configuration file `school-employee-list.yml` to parse `school-employee-list.pdf`.
        To parse your own PDF form, you will need to write your own configuration file.
        See the [Form](#form) section for details.
        If your PDF has a tabular layout, see the [Table](#table) section.

## Logging

Add the `--debug` flag to log everything.

## Extracting text

To extract the text from a PDF, run
`textrictor text --input-format=pdf.itext5 input.pdf input-text.csv`
for any `input.pdf` and then open `input-text.csv` in your favorite spreadsheet program.
It will show you every bit of text that Textricator sees with its position, size,
and font information. This information is very useful for building configuration to parse
tables or forms using Textricator (see the following two sections).

Try `--input-format=pdf.itext7` and `--input-format=pdf.pdfbox` to see how Textricator
extracts the texts using the different parser engines. Some work better for some documents
than others.

## Table

In table mode, the data is grouped into columns based on the x-coordinate of the text.

### Example

This is an example for `src/test/resources/io/mfj/textricator/examples/probes.pdf`.

```yaml
# All measurements are in points. 1 point = 1/72 of an inch.
# x-coordinates are from the left edge of the page.
# y-coordinates are from the top edge of the page.

# Use the built-in pdfbox extractor
extractor: "pdf.pdfbox"

# Ignore everything above 88pt from the top
top: 88

# Ignore everything below 170pt from the top
bottom: 170

# If multiple text segments are withing 2pt vertically, consider them in the same row.
maxRowDistance: 2

# Define the columns, based on the x-coordinate where the column starts:
cols:
  "name": 0
  "launched": 132
  "speed": 235
  "cospar": 249
  "power": 355
  "mass": 415

types:
  "name":
    label: "Name"

  "launched":
    label: "Launch Date"

  "speed":
    label: "Speed (km/s)"
    type: "number"

  "cospar":
    label: "COSPAR ID"

  "power":
    label: "Power (watts)"
    type: "number"

  "mass":
    label: "Mass (pounds)"
    # Add .0 to the end of mass
    replacements:
      -
        pattern: "^(.*)$"
        replacement: "$1.0"

# Omit if Power is less than 200
filter: 'power >= 200'
```

## Form

In form mode, the data is parsed by Textricator using a
[finite-state machine](https://en.wikipedia.org/wiki/Finite-state_machine) (FSM),
and the FSM and additional parsing and formatting parameters are configured with
[YAML](https://en.wikipedia.org/wiki/YAML), indicated by command line option `--config`.

### Conditions

State transitions are selected by evaluating conditions. Conditions are expressions parsed by
[Expr](https://github.com/measuresforjustice/expr).

#### Available variables:

  * `ulx` - x coordinate of the upper-left corner of the text box
  * `uly` - y coordinate of the upper-left corner of the text box
  * `lrx` - x coordinate of the lower-right corner of the text box
  * `lry` - y coordinate of the lower-right corner of the text box
  * `text` - the text
  * `page` - page number
  * `page_prev` - page number of the previous text
  * `fontSize` - font size
  * `font` - font name
  * `color` - text color
  * `bgcolor` - background color
  * `width` - width of the text box
  * `height` - height of the text box
  * `ulx_rel` - difference in `ulx` between the previous and current texts
  * `uly_rel` - difference in `uly` between the previous and current texts
  * `lrx_rel` - difference in `lrx` between the previous and current texts
  * `lry_rel` - difference in `lry` between the previous and current texts
  * [added Variables](#variables)

### Example

This is an example for `src/test/resources/io/mfj/textricator/examples/school-employee-list.pdf`.

```yaml
# Use the built-in pdfbox parser
extractor: "pdf.pdfbox"

# All measurements are in points. 1 point = 1/72 of an inch.
# x-coordinates are from the left edge of the page.
# y-coordinates are from the top edge of the page.
header:
    # ignore anything less than this many points from the top, default and per-page
  default: 130
footer:
    # ignore anything less than this many points from the bottom, default and per-page
  default: 700

# Text segments are generally parsed in order, top to bottom, left to right.
# If two text segments have y-coordinates within this many points, consider them on the same line,
# and process the one further left first, even if it is 0.4pt lower on the page.
maxRowDistance: 2

# Define the output data record.
# Since the main record type we're collecting information on is our employees,
# we'll have that be the root type for our harvested information.
rootRecordType: employee
recordTypes:
  employee:
    label: "employee" # Labels are used when nested recordTypes come into play, like this document.
    valueTypes:
      # Not sure what to name a valueType? Just make something up!
      - employee
      - name
      - hiredate
      - occupation
      - showinfo
      - bool1
      - bool2
      - bool3
      - salary
    children:
      # In this example, there are multiple children nested under an employee,
      # so we'll treat it as a 'child' to the 'employee' recordType.
      - child
  child:
    label: "child"
    valueTypes:
      - child
      - grade

valueTypes:
  employee:
    # In the CSV, use "Employee ID" as the column header instead of "employee".
    label: "Employee ID"
  name:
    label: "Name"
  hiredate:
    label: "Hire Date"
  occupation:
    label: "Occupation"
  salary:
    label: "Salary"
  showinfo:
    label: "Important Info?"
  bool1:
    label: "Boolean 1"
  bool2:
    label: "Boolean 2"
  bool3:
    label: "Boolean 3"
  child:
    label: "Attending Child"
  grade:
    label: "Grade"

# Now we define the finite-state machine
# Let's name the state that our machine starts off with:
initialState: "INIT"

# When each text segment is encountered, each transition for the current state is checked.
states:
  INIT:
    transitions:
      # The first bit of text we reach is 'ID-0001', so we'll try the only transition that should work here.
      -
        # If this condition matches (which it should)
        condition: employee # Curious about the condition? Sxroll further down to the conditions section of this YAML.
        # Then we'll switch to the 'employee' state!
        nextState: employee

  employee: # ID number with the format 'ID-####'
    startRecord: true # When we enter this stage, we'll create a new "case" record.
    transitions:
      - # Now we move on to the name label. Once again, by varifying the condition and moving on after that.
        condition: namelabel
        nextState: namelabel

  namelabel:
    include: false # The label isn't important information in and of itself, so we can just not include it in the data.
    transitions:
      -
        condition: name
        nextState: name

  name:
    transitions:
      -
        # Sometimes a name will be in two segments, and we'll hit another 'name' text segment before anything else.
        # In that case, a state can transition to itself, compounding the information picked up in it.
        condition: name
        nextState: name
      -
        # Does the first condition not match the text? We move onto the next one.
        condition: hiredateLabel
        nextState: hiredateLabel

  hiredateLabel:
    include: false
    transitions:
      -
        condition: hiredateLabel
        nextState: hiredateLabel
      -
        condition: hiredate
        nextState: hiredate

  hiredate:
    transitions:
      -
        condition: occupationLabel
        nextState: occupationLabel

  occupationLabel:
    include: false
    transitions:
      -
        condition: occupation
        nextState: occupation

  occupation:
    transitions:
      -
        condition: occupation
        nextState: occupation
      -
        # This state and the next are an example of how you can choose, using conditions, what to include or not.
        # They share the same area of a document, but have qualities to them that can be distinguishable.
        # Does it meet 'showinfo' conditions? Then we go to the 'showinfo' state that includes it.
        condition: showinfo
        nextState: showinfo
      -
        # Doesn't meet 'showinfo'? Then check for 'notinfo' and exclude it.
        condition: notinfo
        nextState: notinfo
  showinfo:
    transitions:
      -
        condition: showinfo
        nextState: showinfo
      -
        condition: bool1
        nextState: bool1
  notinfo:
    include: false
    transitions:
      -
        condition: notinfo
        nextState: notinfo
      -
        condition: bool1
        nextState: bool1

  bool1:
    transitions:
      -
        condition: bool2
        nextState: bool2
  bool2:
    transitions:
      -
        condition: bool3
        nextState: bool3
  bool3:
    transitions:
      -
        condition: salaryLabel
        nextState: salaryLabel

  salaryLabel:
    include: false
    transitions:
      -
        condition: salary
        nextState: salary

  salary:
    transitions:
      -
        condition: childrenLabel
        nextState: childrenLabel
      -
        condition: employee
        nextState: employee
      -
        condition: end
        nextState: end

  childrenLabel:
    include: false
    transitions:
      -
        condition: childrenLabel
        nextState: childrenLabel
      -
        condition: childLabel
        nextState: childLabel

  childLabel:
    include: false
    transitions:
      -
        condition: child
        nextState: child

  child:
    # Here we reach a datatype nested within another datatype. We can start records using this child datatype.
    # In the process, we'll be making multiple rows for the parent datatype, each one holding onto it's own child.
    startRecord: true
    transitions:
      -
        condition: child
        nextState: child
      -
        condition: gradeLabel
        nextState: gradeLabel
      -
        condition: childLabel
        nextState: childLabel

  gradeLabel:
    include: false
    transitions:
      -
        # Normally, there would always been an instance of a grade appearing right after the label.
        # But in this document, we have one instance of that not happening under ID-0007's child.
        condition: grade
        nextState: grade
      -
        # So we just account for that possibility by adding a transition out of the label.
        condition: employee
        nextState: employee

  grade:
    transitions:
      -
        condition: employee
        nextState: employee
      -
        condition: childLabel
        nextState: childLabel
      -
        # Reach the end of the usable info in a document, but there's still text left to go?
        # An easy fix is to just create a looping, not-included state to finish the document off.
        condition: end
        nextState: end

  end:
    # We reached a point in the document where all the useful information is gone, but we still have text to go.
    include: false
    transitions:
      -
        # By using an always-true condition such as 'any', we can loop this state until the document has been completely gone through.
        condition: any
        nextState: end

# Here we define the conditions:
conditions:

  # An example of comparing text with regex.
  # In this case, we're making sure that the text contains the characters 'ID-' followed by any amount of numbers.
  employee: 'text =~ /ID-(\\d)*/'

  # You can match based on the x- and y- coordinates of the upper left and lower right corners of the rectangle
  # containing the text. ulx = Upper-Left X-coordinate. lry = Lower-Right Y-coordinate. Also uly and lrx.
  # You can define the lower and upper limit for each, inclusive.
  namelabel: '70 < ulx < 80 and font = "BCDFEE+Calibri-Bold"'

  # You can also match based on the type of font used, including if it was bolded or italicized.
  name: '112 < ulx < 200 and font = "BCDEEE+Calibri"'

  hiredateLabel: '230 < ulx < 270 and font = "BCDFEE+Calibri-Bold"'

  hiredate: '280 < ulx < 290 and font = "BCDEEE+Calibri"'

  occupationLabel: '391 < ulx < 393 and font = "BCDFEE+Calibri-Bold"'

  occupation: '394 < ulx < 700 and font = "BCDEEE+Calibri"'

  showinfo: 'font = "BCDJEE+Georgia"'

  notinfo:  'font = "BCDEEE+Calibri"'

  bool1:  'font = "BCDIEE+Cambria"'

  bool2:  'font = "BCDIEE+Cambria"'

  bool3:  'font = "BCDIEE+Cambria"'

  salaryLabel: '391 < ulx < 393 and font = "BCDFEE+Calibri-Bold"'

  salary: '394 < ulx < 700 and font = "BCDEEE+Calibri"'

  childrenLabel: '70 < ulx < 140 and font = "BCDFEE+Calibri-Bold" and text =~ /(Attending)|(Children:)/'

  childLabel: '230 < ulx < 240 and font = "BCDFEE+Calibri-Bold"'

  child: '230 < ulx < 380 and font = "BCDEEE+Calibri"'

  gradeLabel: '391 < ulx < 393 and font = "BCDFEE+Calibri-Bold"'

  grade: '394 < ulx < 700 and font = "BCDEEE+Calibri"'

  # You can also match based on the size of the font and on specific text.
  end: 'fontSize = 16.0 and text = "TOTAL:"'

  # Need a condition that is always true? "1=1" does that for you.
  any: "1 = 1"
```

### Advanced functionality

#### Regex matching

You can use regular expression matching instead of exact string matching in conditions:

```yaml
conditions:
  caseTypeLabel: 'text =~ /Case Type:?/' # maybe sometimes they forgot the ":"
```

#### Font matching

You can match font and font size:

```yaml
conditions:
  helvetica8: 'font =~ /.*Helvetica.*/ and fontSize = 8'
```

#### Page number matching

You can match the page number

```yaml
conditions:
  # Match the textbox that starts at 100pt,120pt on page 9.
  specificTextbox: 'page = 9 and ulx = 100 and uly = 120'
```

#### Requiring intermediate state

There may be cases where you want to transition from state "B" to state "A" and start a new record,
but ONLY if you were in state "C" since last starting a new record:

```yaml
states:
  A:
    startRecord: true
    startRecordRequiredState: C
    # ...
```

#### Combining states

You may have 2 different states that you want to combine into one column in the output:

```yaml
recordTypes:
  record:
    label: "Record"
    valueTypes:
      - A
      # no A2
      - B

states:
  A:
    # ...
  A2:
    valueTypes: [ A ] # combine this with state "A".
    # ...
```

#### Joining text segments in a state

When there are multiple text segments in the same state, by default they are concatenated with a space in between.
Any string, including an empty string, can be used as the separator:

```yaml
valueTypes:
  recordId:
    # concatenate without spaces between
    label: "Record ID"
    separator: ""
  description:
    # put newlines between text segments
    label: "Description"
    separator: "
"
```

#### Exclude types from output

A value type (`dataRecordMember`) can be excluded from the output.
This is useful if the type is repeated on the PDF and needed as a data type to set new records,
but should not be in the output.

```yaml
valueTypes:
  repeatedId:
    include: false
```
#### Relative offsets

Conditions can evaluate coordinates relative to the coordinates of the previous text.
This is useful for matching only something on the next line:

```yaml
maxRowDistance: 2
conditions:
  # These should generally be the same (absolute value) as maxRowDistance
  descriptionSameLine: '-2 <= uly_rel <= 2'
  descriptionOneLineDown: '12 <= uly_rel <= 16' # for 12pt, single-spaced font.
```

#### Control which page number in output

If a complex data record spans multiple pages, which page number is used for the output can be controlled.
Each type (`dataRecordType`) has a page priority (default: 0).
The page number for the record comes from the type with the highest priority.

```yaml
recordTypes:
  agency:
    label: "Agency"
    valueTypes:
      - agencyName
    children:
      - case
    # Agency may span hundreds of pages
    # default page priority of 0
  case:
    label: "Case"
    valueTypes:
      - caseNumber
      - name
    children:
      - charge
    # Case record may span multiple pages.
    # Higher priority than agency but lower than charges,
    so case is used if there are no charges.
    pagePriority: 1
  charge:
    label: "Charge"
    valueTypes:
      - chargeNumber
    # Highest priority. Use charge's page number.
    pagePriority: 2
```

#### Exclude 

Text segments can be excluded before processing by the finite-state machine
by adding condition name to the `excludeConditions` list.
If any of the conditions match, the text segment is excluded.

For example, to exclude all text segments that consist solely of underscores:

```yaml
excludeConditions:
  - underline
  
conditions:
  underline: 'text =~ /_+/'
```

#### Starting new records for each value

TODO

#### Regex replacement of values

Sometimes the value contains a label, or other text you want to remove.
In a `dataRecordMember`, add a `replacement`, which has a `pattern`,
which is a regular expression with capturing groups,
and `replacement`, which is a replacement string with group references.
See `java.util.regex.Pattern` and `java.util.regex.Matcher` for details.

```yaml
# Replace "Bond Agency: Fred's Bonds" with "Fred's Bonds"
valueTypes:
  bondagency:
    label: "Bond Agency"
    replacements:
      -
        pattern: "Bond Agency:\ *(.*)"
        replacement: "$1"
```

#### Variables<a name="variables"></a>

On entering a state, a variable can be set.

In `State`, add a `setVariable`, which has `name` of the variable and the `value` to set.

If the `value` starts with `{` and ends with `}`, the content can be any built-in variable or previously
set `variable` - the same things usable as variables for conditions.

For example: a condition checks that has the same background color as the last caseNo:

```yaml
states:
  caseNo:
    setVariables:
      -
        name: "lastCaseBgColor"
        value: "{bgcolor}"

conditions:
  sameBgColor: >
    145 <= ulx
    and fontSize = 8
    and bgcolor = lastCaseBgColor
```

#### Splitting one PDF field into multiple CSV columns

One field in the PDF can be broken up into multiple columns in the CSV file, based on
different regular expressions and replacements:

```yaml
# Take a field that contains lastName,firstName and split it into 2 fields - lastName and firstName
recordTypes:
  inmateName:
    label: "inmateName"
    valueTypes:
      - lastName
      - firstName

valueTypes:
  lastName:
    label: "Last Name"
    replacements:
      -
        # lastName column will contain what's before comma
        pattern: "(.*),.*"
        replacement: "$1"
  firstName:
    label: "First Name"
    replacements:
      -
        # firstName column will contain what's after comma
        pattern: ".*,(.*)"
        replacement: "$1"

states:
  name:
    # when FSM hits state "name", split data into lastName and firstName
    valueTypes:
      - lastName
      - firstName
```

#### Hyperlinks

If the text is a hyperlink, the URL can be used intead of the text by
setting `valueTypes.attribute` to `url`.

This is supported only by the `itext5` and `itext7` parsers.

```yaml
recordTypes:
  company:
    label: "Company"
    valueTypes:
      - name
      - website

valueTypes:
  name:
    label: "Company Name"
  website:
    label: "Website"
    attribute: url # use the link URL instead of the text.

states:
  name:
    # Put the value in both "name" and "website" to get both the
    # text and the url into the output.
    valueTypes:
      - name
      - website
```

#### Filtering records

Records can be filtered before output by setting a filter on the `dataRecordType`. Root records
with a non-matching filter will not be output. Child records with a non-matching filter will
be removed from their parent - the root will still be output.

Filters are expressions parsed by
[Expr](https://github.com/measuresforjustice/expr)

The variables are the fields in the record. The default type for variables is `STRING`.
The type can be set by setting `type` in the `dataRecordMember` to `string` or `number`.

```yaml
recordTypes:
  case:
    label: "Case"
    # Only include cases in 2009-2013
    filter: "2009 <= year <= 2013"
    valueTypes:
      - year
      - name
    children:
      - charge
  charge:
    label: "Charge"
    # omit charges of type "dummy"
    filter: >
      not( type = "dummy" )
    valueTypes:
      - code
      - type

valueTypes:
  year:
    type: number
```

Filtering happens after replacements and before member include checking.
This allows splitting a field (e.g.: extracting a year from a date)
to use in the filter, and not including the split field in the output.

## Extractors

Extractors extract text (instances of `io.mfj.textricator.text.Text`) from a source.
There are four included extractors:

  * _pdf.pdfbox_ - Extract text from PDF files using [Apache PDFBox](https://pdfbox.apache.org).
  * _pdf.itext5_ - Extract text from PDF files using [iText 5](https://itextpdf.com/itext-5-core).
  * _pdf.itext7_ - Extract text from PDF files using [iText 7](https://itextpdf.com/itext-7-core).
  * _json_ - Parse text from the JSON format generated by the "text" subcommand of the CLI and by
  `io.mfj.textricator.Textricator.parseText()`.

Other extractors modules may be loaded, which may support different source types,
capture different information or split the text up differently.
An extractor module is loaded by adding a properties file
`io/mfj/textricator/extractor/textExtractor.properties` to the classpath with a single property -
the key is the extractor name and the value is the fully-qualified class name of an implementation of
`io.mfj.textricator.extractor.TextExtractorFactory`.
Typically an extractor module will be distributed as a JAR that includes `textExtractor.properties`.

Indicate which extractor to use by setting `extractor` in the yaml configuration,
or overriding it by passing the extractor name to the `inputFormat`/`--input-format` option.
If the extractor is not specified and the input is PDF, `pdf.itext5` is used.

## Versioning

_Textricator_'s version is of the format `major.minor.build`.

The major number is incremented for breaking changes or major new features.

The minor number is incremented for minor new features.

The build number is generated by [Measures for Justice](https://measuresforjustice.org)'s private CI tool.
It is incremented for each build, regardless of the major and minor numbers
(it does not reset to zero when minor or major numbers are increased).

## Acknowledgements

Much credit is due to some people who do not show up in the commit history:

* Joe Hale, for the original idea and prototype
* John Castaneda and Abbie Miehle who, as the first end users, provided excellent
bug reports, improvements, documentation, and examples.

![mascot](./textricator-mascot.png)
