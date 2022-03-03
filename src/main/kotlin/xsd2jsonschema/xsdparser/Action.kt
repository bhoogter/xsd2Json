package xsd2jsonschema.xsdparser

import xsd2jsonschema.xsdparser.Type.TARGETNAMESPACE

internal abstract class Action(val tracker: Tracker) {
    internal abstract fun perform(line: Line)
}

internal class StartSchema(tracker: Tracker, val addNewSchema: (schema: Schema) -> Unit) : Action(tracker) {
    public override fun perform(line: Line) {
        val targetNS = line.attrs
            .first { attr -> attr.localpart.equals(TARGETNAMESPACE.name, true) }
        addNewSchema(Schema(line, targetNS.value))
    }
}

internal class StartComplexType(tracker: Tracker, val typeDataHolder: (type: Type) -> Unit) : Action(tracker) {
    public override fun perform(line: Line) {
        tracker.inComplexType = true
        typeDataHolder(Type.COMPLEXTYPE)
    }
}

internal class StartSimpleType(tracker: Tracker, val typeDataHolder: (type: Type) -> Unit) : Action(tracker) {
    public override fun perform(line: Line) {
        tracker.inSimpleType = true
        typeDataHolder(Type.SIMPLETYPE)
    }
}

internal class StartElement(tracker: Tracker, val typeDataHolder: (type: Type) -> Unit) : Action(tracker) {
    public override fun perform(line: Line) {
        // This is only for the root element, all others must go in simple or complex types
        when {
            !tracker.inSimpleType && !tracker.inComplexType && !tracker.inElementType -> {
                tracker.inElementType = true
                typeDataHolder(Type.ELEMENT)
            }
        }
    }
}

internal class EndComplexType(tracker: Tracker) : Action(tracker) {
    public override fun perform(line: Line) {
        tracker.inComplexType = false
    }
}

internal class EndSimpleType(tracker: Tracker) : Action(tracker) {
    public override fun perform(line: Line) {
        tracker.inSimpleType = false
    }
}

internal class EndElement(tracker: Tracker) : Action(tracker) {
    public override fun perform(line: Line) {
        when {
            !tracker.inSimpleType && !tracker.inComplexType && !tracker.inElementType -> {
                tracker.inElementType = false
            }
        }
    }
}