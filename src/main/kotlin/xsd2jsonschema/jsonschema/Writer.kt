package xsd2jsonschema.jsonschema

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.json.JSONArray
import org.json.JSONObject
import xsd2jsonschema.jsonschema.Constants.Companion.ALLOF
import xsd2jsonschema.jsonschema.Constants.Companion.ARRAY
import xsd2jsonschema.jsonschema.Constants.Companion.ATSTRING
import xsd2jsonschema.jsonschema.Constants.Companion.ATTRIBUTESTRING
import xsd2jsonschema.jsonschema.Constants.Companion.CHOICESTRING
import xsd2jsonschema.jsonschema.Constants.Companion.COMMENT
import xsd2jsonschema.jsonschema.Constants.Companion.COMPLEXTYPESTRING
import xsd2jsonschema.jsonschema.Constants.Companion.DECIMALCOMMENT
import xsd2jsonschema.jsonschema.Constants.Companion.ELEMENTSTRING
import xsd2jsonschema.jsonschema.Constants.Companion.EXTENSIONSTRING
import xsd2jsonschema.jsonschema.Constants.Companion.FRACTIONDIGITSCOMMENT
import xsd2jsonschema.jsonschema.Constants.Companion.HASHNAMESTRING
import xsd2jsonschema.jsonschema.Constants.Companion.ID
import xsd2jsonschema.jsonschema.Constants.Companion.ITEMS
import xsd2jsonschema.jsonschema.Constants.Companion.MAXITEMS
import xsd2jsonschema.jsonschema.Constants.Companion.MAXOCCURS
import xsd2jsonschema.jsonschema.Constants.Companion.MINITEMS
import xsd2jsonschema.jsonschema.Constants.Companion.MINOCCURS
import xsd2jsonschema.jsonschema.Constants.Companion.OBJECTSTRING
import xsd2jsonschema.jsonschema.Constants.Companion.ONEOF
import xsd2jsonschema.jsonschema.Constants.Companion.PROPERTIES
import xsd2jsonschema.jsonschema.Constants.Companion.REQUIRED
import xsd2jsonschema.jsonschema.Constants.Companion.SCHEMASTRING
import xsd2jsonschema.jsonschema.Constants.Companion.SCHEMAURL
import xsd2jsonschema.jsonschema.Constants.Companion.SEQUENCESTRING
import xsd2jsonschema.jsonschema.Constants.Companion.SIMPLECONTENTSTRING
import xsd2jsonschema.jsonschema.Constants.Companion.STRINGS
import xsd2jsonschema.jsonschema.Constants.Companion.TOTALDIGITSCOMMENT
import xsd2jsonschema.jsonschema.Constants.Companion.UNBOUNDED
import xsd2jsonschema.jsonschema.Constants.Companion.USE
import xsd2jsonschema.jsonschema.Keys.Companion.BASE_64_BINARY
import xsd2jsonschema.jsonschema.Keys.Companion.BOOLEAN
import xsd2jsonschema.jsonschema.Keys.Companion.DATE_TIME
import xsd2jsonschema.jsonschema.Keys.Companion.DATE_TIME_JSON
import xsd2jsonschema.jsonschema.Keys.Companion.DECIMAL
import xsd2jsonschema.jsonschema.Keys.Companion.DEFINITIONS
import xsd2jsonschema.jsonschema.Keys.Companion.ENUM
import xsd2jsonschema.jsonschema.Keys.Companion.ENUMERATION
import xsd2jsonschema.jsonschema.Keys.Companion.FORMAT
import xsd2jsonschema.jsonschema.Keys.Companion.FRACTIONDIGITS
import xsd2jsonschema.jsonschema.Keys.Companion.MAXLENGTH
import xsd2jsonschema.jsonschema.Keys.Companion.MINIMUM
import xsd2jsonschema.jsonschema.Keys.Companion.MININCLUSIVE
import xsd2jsonschema.jsonschema.Keys.Companion.MINLENGTH
import xsd2jsonschema.jsonschema.Keys.Companion.NAME
import xsd2jsonschema.jsonschema.Keys.Companion.NUMBER
import xsd2jsonschema.jsonschema.Keys.Companion.PATTERN
import xsd2jsonschema.jsonschema.Keys.Companion.REF
import xsd2jsonschema.jsonschema.Keys.Companion.RESTRICTION
import xsd2jsonschema.jsonschema.Keys.Companion.SIMPLE
import xsd2jsonschema.jsonschema.Keys.Companion.STRING
import xsd2jsonschema.jsonschema.Keys.Companion.TOTALDIGITS
import xsd2jsonschema.jsonschema.Keys.Companion.TYPE
import xsd2jsonschema.xsdparser.Attr
import xsd2jsonschema.xsdparser.Line
import xsd2jsonschema.xsdparser.Schema
import xsd2jsonschema.xsdparser.Type.*
import xsd2jsonschema.xsdparser.TypeLines
import java.util.*

private val logger = KotlinLogging.logger {}

class Writer {

    private var strings: MutableSet<String> = HashSet()

    internal fun getJSONSchema(schemas: List<Schema>): JSONObject {
        val root = JSONObject()
        schemas.forEach { schema -> processSchema(root, schema) }
        return root
    }

    private fun processSchema(root: JSONObject, schema: Schema) {
        root.put(SCHEMASTRING, SCHEMAURL)
        root.put(ID, schema.targetNameSpace)
        val definitionsData = JSONObject()
        val allOfData = JSONArray()
        val allOfDataObject = JSONObject()
        allOfData.put(allOfDataObject)
        root.put(DEFINITIONS, definitionsData)
        root.put(ALLOF, allOfData)
        schema.lines.forEach { lines -> processLines(lines, definitionsData, allOfDataObject) }
        logger.info("$STRINGS$strings")
    }

    private fun processLines(typeData: TypeLines, definitionsData: JSONObject, allOfDataObject: JSONObject) {
        val linesData = Optional.of(typeData.lines)
        when (typeData.type) {
            ELEMENT -> createElement(allOfDataObject, linesData)
            SIMPLETYPE -> linesData
                    .filter { lines -> lines.isNotEmpty() }
                    .ifPresent { lines -> createSimpleType(definitionsData, lines) }
            COMPLEXTYPE -> linesData
                    .filter { lines -> lines.isNotEmpty() }
                    .ifPresent { lines -> createComplexTypeWrapper(definitionsData, lines) }
            else -> {
            }
        }
    }

    private fun createElement(allOfDataObject: JSONObject, linesData: Optional<MutableList<Line>>) {
        linesData
                .filter { lines -> lines.isNotEmpty() }
                .map { lines -> lines[0] }
                .map { line -> line.attrs }
                .filter { attrs -> attrs.isNotEmpty() }
                .ifPresent { attrs ->
                    attrs
                            .stream()
                            .filter { attr ->
                                StringUtils.isNotBlank(attr.localpart) &&
                                        StringUtils.isNotBlank(attr.value) &&
                                        attr.localpart.equals(TYPE, ignoreCase = true)
                            }
                            .findAny()
                            .ifPresent { attr -> allOfDataObject.put(REF, DEFINITIONS + attr.value) }
                }
    }

    private fun createSimpleType(definitionsData: JSONObject, lines: List<Line>) {
        val simpleObject = JSONObject()
        lines.forEach { line ->
            val name = line.name
            line.attrs
                    .forEach { attr -> handleSimpleAttr(definitionsData, simpleObject, name, attr) }
        }
    }


    private fun createComplexTypeWrapper(definitionsData: JSONObject, lines: List<Line>) {
        val complexObject = JSONObject()
        lines.forEach { line ->
            val name = line.name
            strings.add(name)
            when (name) {
                COMPLEXTYPESTRING -> createComplexType(line, definitionsData, complexObject)
                CHOICESTRING -> createChoice(complexObject)
                SIMPLECONTENTSTRING -> createJSONObject(complexObject)
                EXTENSIONSTRING -> createExtension(line, definitionsData, complexObject)
                ATTRIBUTESTRING -> createAttribute(line, complexObject)
                SEQUENCESTRING -> createJSONObject(complexObject)
                ELEMENTSTRING -> createElement(line, complexObject)
            }
        }
    }

    private fun createElement(line: Line, complexObject: JSONObject) {
        val complexElement = ComplexElement()
        line.attrs.forEach { attr -> createAttrElement(attr, complexElement) }
        when {
            complexObject.has(ONEOF) -> createOneOf(complexObject, complexElement)
            else -> {
                createSequenceType(complexElement, complexObject)
            }
        }
    }

    private fun createSequenceType(complexElement: ComplexElement, complexObject: JSONObject) {
        when {
            complexElement.minimum >= 1 -> {
                createRequired(complexObject, complexElement)
            }
        }
        when {
            complexElement.maximum > 1 || complexElement.minimum > 1 -> {
                createArray(complexElement, complexObject)
            }
            else -> {
                createProperties(complexObject, complexElement)
            }
        }
    }

    private fun createRequired(complexObject: JSONObject, complexElement: ComplexElement) {
        when {
            !complexObject.has(REQUIRED) -> {
                complexObject.put(REQUIRED, JSONArray())
            }
        }
        complexObject.getJSONArray(REQUIRED).put(complexElement.name)
    }

    private fun createArray(complexElement: ComplexElement, complexObject: JSONObject) {
        val arrayObject = JSONObject()
        arrayObject.put(TYPE, ARRAY)
        arrayObject.put(ITEMS, JSONObject().put(REF, DEFINITIONS + complexElement.type!!))
        arrayObject.put(MINITEMS, complexElement.minimum)
        arrayObject.put(MAXITEMS, complexElement.maximum)
        complexObject.getJSONObject(PROPERTIES).put(complexElement.name!!, arrayObject)
    }

    private fun createOneOf(complexObject: JSONObject, complexElement: ComplexElement) {
        // This is the choice type
        createProperties(complexObject, complexElement)
        complexObject
                .getJSONArray(ONEOF)
                .put(JSONObject().put(REQUIRED, JSONArray().put(complexElement.name)))
    }

    private fun createProperties(complexObject: JSONObject, complexElement: ComplexElement) {
        complexObject
                .getJSONObject(PROPERTIES)
                .put(complexElement.name!!, JSONObject().put(REF, DEFINITIONS + complexElement.type!!))
    }

    private fun createAttrElement(attr: Attr, complexElement: ComplexElement) {
        val value = attr.value
        when (attr.localpart) {
            MINOCCURS -> complexElement.minimum = Integer.valueOf(value)
            NAME -> complexElement.name = value
            MAXOCCURS ->
                when {
                    value.equals(UNBOUNDED, ignoreCase = true) -> complexElement.maximum = 999
                    else -> complexElement.maximum = Integer.valueOf(value)
                }
            TYPE -> complexElement.type = value
        }
    }

    private fun createChoice(complexObject: JSONObject) {
        createJSONObject(complexObject)
        complexObject.put(ONEOF, JSONArray())
    }

    private fun createComplexType(line: Line, definitionsData: JSONObject, complexObject: JSONObject) {
        line.attrs.stream()
                .filter { attr -> StringUtils.containsIgnoreCase(attr.localpart, NAME) }
                .findAny()
                .ifPresent { attr -> definitionsData.put(attr.value, complexObject) }
    }


    private fun createExtension(line: Line, definitionsData: JSONObject, complexObject: JSONObject) {
        line.attrs
                .filter { attr -> attr.localpart.equals("base", ignoreCase = true) && definitionsData.has(attr.value) }
                .forEach { attr -> complexObject.getJSONObject(PROPERTIES).put(HASHNAMESTRING, definitionsData.get(attr.value)) }
    }

    private fun createJSONObject(complexObject: JSONObject) {
        complexObject.put(TYPE, OBJECTSTRING)
        complexObject.put(PROPERTIES, JSONObject())
    }


    private fun createAttribute(line: Line, complexObject: JSONObject) {
        val attribute = readAttribute(line)
        complexObject.getJSONObject(PROPERTIES).put(ATSTRING + attribute.name!!, DEFINITIONS + attribute.type!!)
        when {
            attribute.use.equals(REQUIRED, ignoreCase = true) -> {
                complexObject.put(REQUIRED, JSONArray().put(HASHNAMESTRING).put(ATSTRING + attribute.name))
            }
        }
    }


    private fun readAttribute(line: Line): ComplexAttribute {
        val attribute = ComplexAttribute()
        line.attrs
                .forEach { attr ->
                    val value = attr.value
                    when (attr.localpart) {
                        NAME -> attribute.name = value
                        TYPE -> attribute.type = value
                        USE -> attribute.use = value
                    }
                }
        return attribute
    }

    private fun handleSimpleAttr(definitionsData: JSONObject, simpleObject: JSONObject, name: String, attr: Attr) {
        val value = attr.value
        when (name) {
            SIMPLE -> definitionsData.put(value, simpleObject)
            RESTRICTION -> handleSimpleRestrictions(simpleObject, value)
            ENUMERATION -> handleSimpleEnums(simpleObject, value)
            PATTERN -> simpleObject.put(PATTERN, value)
            MINLENGTH -> simpleObject.put(MINLENGTH, value)
            MAXLENGTH -> simpleObject.put(MAXLENGTH, value)
            TOTALDIGITS -> addComment(simpleObject, "$TOTALDIGITSCOMMENT$value")
            FRACTIONDIGITS -> addComment(simpleObject, "$FRACTIONDIGITSCOMMENT$value")
            MININCLUSIVE -> simpleObject.put(MINIMUM, Integer.valueOf(value))
        }
    }


    private fun handleSimpleEnums(simpleObject: JSONObject, value: String) {
        when {
            !simpleObject.has(ENUM) -> simpleObject.put(ENUM, JSONArray().put(value))
            else -> {
                simpleObject.getJSONArray(ENUM).put(value)
            }
        }
    }

    private fun handleSimpleRestrictions(simpleObject: JSONObject, value: String) {
        when {
            value.equals(STRING, ignoreCase = true) || value.equals(BASE_64_BINARY, ignoreCase = true) -> {
                simpleObject.put(TYPE, STRING)
            }
            value.equals(DATE_TIME, ignoreCase = true) -> {
                simpleObject.put(TYPE, STRING)
                simpleObject.put(FORMAT, DATE_TIME_JSON)
            }
            value.equals(BOOLEAN, ignoreCase = true) -> {
                simpleObject.put(TYPE, BOOLEAN)
            }
            value.equals(DECIMAL, ignoreCase = true) -> {
                simpleObject.put(TYPE, NUMBER)
                addComment(simpleObject, DECIMALCOMMENT)
            }
        }
    }

    private fun addComment(simpleObject: JSONObject, s: String) {
        when {
            simpleObject.has(COMMENT) -> simpleObject.put(COMMENT, simpleObject.getString(COMMENT) + " , $s")
            else -> simpleObject.put(COMMENT, s)
        }
    }
}
