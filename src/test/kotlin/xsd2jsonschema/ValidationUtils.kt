package xsd2jsonschema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.exceptions.ProcessingException
import com.github.fge.jsonschema.main.JsonSchema
import com.github.fge.jsonschema.main.JsonSchemaFactory

import java.io.File
import java.io.IOException
import java.net.URL

object ValidationUtils {

    val JSON_V4_SCHEMA_IDENTIFIER = "http://json-schema.org/draft-04/schema#"
    val JSON_SCHEMA_IDENTIFIER_ELEMENT = "\$schema"

    @Throws(IOException::class)
    fun getJsonNode(jsonText: String): JsonNode {
        return JsonLoader.fromString(jsonText)
    } // getJsonNode(text) ends

    @Throws(IOException::class)
    fun getJsonNode(jsonFile: File): JsonNode {
        return JsonLoader.fromFile(jsonFile)
    } // getJsonNode(File) ends

    @Throws(IOException::class)
    fun getJsonNode(url: URL): JsonNode {
        return JsonLoader.fromURL(url)
    } // getJsonNode(URL) ends

    @Throws(IOException::class)
    fun getJsonNodeFromResource(resource: String): JsonNode {
        return JsonLoader.fromResource(resource)
    } // getJsonNode(Resource) ends

    @Throws(IOException::class, ProcessingException::class)
    fun getSchemaNode(schemaText: String): JsonSchema {
        val schemaNode = getJsonNode(schemaText)
        return _getSchemaNode(schemaNode)
    } // getSchemaNode(text) ends

    @Throws(IOException::class, ProcessingException::class)
    fun getSchemaNode(schemaFile: File): JsonSchema {
        val schemaNode = getJsonNode(schemaFile)
        return _getSchemaNode(schemaNode)
    } // getSchemaNode(File) ends

    @Throws(IOException::class, ProcessingException::class)
    fun getSchemaNode(schemaFile: URL): JsonSchema {
        val schemaNode = getJsonNode(schemaFile)
        return _getSchemaNode(schemaNode)
    } // getSchemaNode(URL) ends

    @Throws(IOException::class, ProcessingException::class)
    fun getSchemaNodeFromResource(resource: String): JsonSchema {
        val schemaNode = getJsonNodeFromResource(resource)
        return _getSchemaNode(schemaNode)
    } // getSchemaNode() ends

    @Throws(ProcessingException::class)
    fun validateJson(jsonSchemaNode: JsonSchema, jsonNode: JsonNode) {
        val report = jsonSchemaNode.validate(jsonNode)
        when {
            !report.isSuccess -> {
                for (processingMessage in report) {
                    throw ProcessingException(processingMessage)
                }
            }
        }
    } // validateJson(Node) ends

    @Throws(ProcessingException::class)
    fun isJsonValid(jsonSchemaNode: JsonSchema, jsonNode: JsonNode): Boolean {
        val report = jsonSchemaNode.validate(jsonNode)
        return report.isSuccess
    } // validateJson(Node) ends

    @Throws(ProcessingException::class, IOException::class)
    fun isJsonValid(schemaText: String, jsonText: String): Boolean {
        val schemaNode = getSchemaNode(schemaText)
        val jsonNode = getJsonNode(jsonText)
        return isJsonValid(schemaNode, jsonNode)
    } // validateJson(Node) ends

    @Throws(ProcessingException::class, IOException::class)
    fun isJsonValid(schemaFile: File, jsonFile: File): Boolean {
        val schemaNode = getSchemaNode(schemaFile)
        val jsonNode = getJsonNode(jsonFile)
        return isJsonValid(schemaNode, jsonNode)
    } // validateJson(Node) ends

    @Throws(ProcessingException::class, IOException::class)
    fun isJsonValid(schemaURL: URL, jsonURL: URL): Boolean {
        val schemaNode = getSchemaNode(schemaURL)
        val jsonNode = getJsonNode(jsonURL)
        return isJsonValid(schemaNode, jsonNode)
    } // validateJson(Node) ends

    @Throws(IOException::class, ProcessingException::class)
    fun validateJson(schemaText: String, jsonText: String) {
        val schemaNode = getSchemaNode(schemaText)
        val jsonNode = getJsonNode(jsonText)
        validateJson(schemaNode, jsonNode)
    } // validateJson(text) ends

    @Throws(IOException::class, ProcessingException::class)
    fun validateJson(schemaFile: File, jsonFile: File) {
        val schemaNode = getSchemaNode(schemaFile)
        val jsonNode = getJsonNode(jsonFile)
        validateJson(schemaNode, jsonNode)
    } // validateJson(File) ends

    @Throws(IOException::class, ProcessingException::class)
    fun validateJson(schemaDocument: URL, jsonDocument: URL) {
        val schemaNode = getSchemaNode(schemaDocument)
        val jsonNode = getJsonNode(jsonDocument)
        validateJson(schemaNode, jsonNode)
    } // validateJson(URL) ends

    @Throws(IOException::class, ProcessingException::class)
    fun validateJsonResource(schemaResource: String, jsonResource: String) {
        val schemaNode = getSchemaNode(schemaResource)
        val jsonNode = getJsonNodeFromResource(jsonResource)
        validateJson(schemaNode, jsonNode)
    } // validateJsonResource() ends

    @Throws(ProcessingException::class)
    private fun _getSchemaNode(jsonNode: JsonNode): JsonSchema {
        when (jsonNode.get(JSON_SCHEMA_IDENTIFIER_ELEMENT)) {
            null -> {
                (jsonNode as ObjectNode).put(JSON_SCHEMA_IDENTIFIER_ELEMENT, JSON_V4_SCHEMA_IDENTIFIER)
            }
        }

        val factory = JsonSchemaFactory.byDefault()
        return factory.getJsonSchema(jsonNode)
    } // _getSchemaNode() ends
}
