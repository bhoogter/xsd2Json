package xsd2jsonschema.xsdparser

import javax.xml.stream.events.XMLEvent

import javax.xml.stream.XMLStreamConstants.*
import xsd2jsonschema.xsdparser.Type.*
import java.util.*
import javax.xml.stream.events.Attribute

internal class Parser {

    private val tracker = Tracker();
    val schemas = ArrayList<Schema>();
    private var optionalSchema = Optional.empty<Schema>()
    private var optionalData = Optional.empty<TypeLines>()
    private val startMap = mapOf(
            SCHEMA to StartSchema(tracker, this::newSchema),
            COMPLEXTYPE to StartComplexType(tracker, this::newData),
            SIMPLETYPE to StartSimpleType(tracker, this::newData),
            ELEMENT to StartElement(tracker, this::newData)
    )
    private val endMap = mapOf(
            COMPLEXTYPE to EndComplexType(tracker),
            SIMPLETYPE to EndSimpleType(tracker),
            ELEMENT to EndElement(tracker)
    )

    fun parseEvent(event: XMLEvent) {
        when (event.eventType) {
            START_ELEMENT -> {
                val startLine = readStartLine(event)
                createDataIfNeeded(startLine)
                optionalData.ifPresent { typeLines -> typeLines.lines.add(startLine) }
            }
            END_ELEMENT -> {
                val endLine = readEndLine(event)
                endDataIfNeeded(endLine)
            }
            END_DOCUMENT -> {
                optionalSchema = Optional.empty()
                optionalData = Optional.empty()
            }
            else -> {
            }
        }
    }

    private fun createDataIfNeeded(line: Line) {
        performAction(line, startMap)
    }

    private fun endDataIfNeeded(line: Line) {
        performAction(line, endMap)
    }

    private fun performAction(line: Line, map: Map<Type, Action>) {
        Type
                .getType(line.name)
                .ifPresent { type ->
                    Optional
                            .ofNullable(map[type])
                            .ifPresent { action -> action.perform(line) }
                }
    }

    private fun readStartLine(event: XMLEvent): Line {
        val line = Line(
                event.asStartElement().name.localPart,
                event.asStartElement().name.namespaceURI
            )

        line.attrs.addAll(
                event.asStartElement().attributes.asSequence().filterIsInstance<Attribute>().map { attr ->
                    Attr(
                            attr.name.localPart,
                            attr.name.namespaceURI,
                            attr.name.prefix,
                            attr.value )}
        )
        return line
    }

    private fun readEndLine(event: XMLEvent): Line {
        return Line(
                event.asEndElement().name.localPart,
                event.asEndElement().name.namespaceURI
        )
    }

    private fun newData(element: Type) {
        val newTypeLines = TypeLines(element)
        this.optionalData = Optional.of(newTypeLines)
        optionalSchema.ifPresent { schema -> schema.lines.add(newTypeLines) }
    }

    private fun newSchema(newSchema: Schema){
        schemas.add(newSchema)
        optionalSchema = Optional.of(newSchema)
    }
}
