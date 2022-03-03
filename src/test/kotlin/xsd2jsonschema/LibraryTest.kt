/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package xsd2jsonschema

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonschema.core.exceptions.ProcessingException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

import java.io.File
import java.io.IOException

import org.junit.jupiter.api.Assertions.assertTrue

class LibraryTest {

    @Test
    fun `test Validate Json Schema Valid Data should complete successfully` () {

        val schemaFile = File("src/test/resources/json-schema/jsonTestSchema.json")
        val jsonFile = File("src/test/resources/dataTest.json")

        try {
            assertTrue(ValidationUtils.isJsonValid(schemaFile, jsonFile))
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ProcessingException) {
            e.printStackTrace()
        }

    }

    @Test
    fun `test Validate Json Schema Invalid Data should complete successfully` () {
        val schemaFile = File("src/test/resources/json-schema/jsonTestSchema.json")
        val jsonFile = File("src/test/resources/dataTestBad.json")

        try {
            assertTrue(!ValidationUtils.isJsonValid(schemaFile, jsonFile))
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ProcessingException) {
            e.printStackTrace()
        }

    }

    //get XSD files from https://www.iso20022.org/iso-20022-message-definitions?business-domain=1
    @Test
    fun `test Generate a PACS008 JSON Schema From PACS008 XSD Schema should complete successfully` () {
        val mapper = ObjectMapper()
        assertEquals(
            mapper.readTree(File("src/test/resources/json-schema/pacs.008.001.09.schema.json").readText(Charsets.UTF_8)),
            mapper.readTree(Library().generateFromSchema(File("src/test/resources/xsd/pacs.008.001.09.xsd")))
        )
    }

    @Test
    fun `test Generate a CAMT50 JSON Schema From CAMT50 XSD Schema should complete successfully` () {
        val mapper = ObjectMapper()
        assertEquals(
            mapper.readTree(File("src/test/resources/json-schema/camt.050.001.05.schema.json").readText(Charsets.UTF_8)),
            mapper.readTree(Library().generateFromSchema(File("src/test/resources/xsd/camt.050.001.05.xsd")))
        )
    }
}
