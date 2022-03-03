package xsd2jsonschema.xsdparser

import java.util.EnumSet
import java.util.Optional

internal enum class Type {
    SCHEMA, ELEMENT, COMPLEXTYPE, SIMPLETYPE, TARGETNAMESPACE;

    companion object {

        fun getType(name: String): Optional<Type> {
            return EnumSet.allOf<Type>(Type::class.java).stream().filter { v -> name.equals(v.name, ignoreCase = true) }
                .findAny()
        }
    }
}
