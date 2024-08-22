package io.spoud.kafka.connect.extensions.check

import io.github.optimumcode.json.schema.JsonSchema
import io.github.optimumcode.json.schema.ValidationError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.File

class JsonSchemaCheck(private val configurationDir: File) : PolicyCheck {

    init {
        check(configurationDir.exists() && configurationDir.isDirectory)
    }

    private lateinit var schema: JsonSchema

    override fun configure(config: Map<String, String>) {
        if (config.contains("file") && !config["file"].isNullOrEmpty()) {
            val confFile = File(configurationDir, config["file"]!!)
            check(confFile.exists()) { confFile.absoluteFile }
            this.schema = parseSchemaString(confFile.readText())
        }
    }


    private fun matchesSchema(connectorConfiguration: JsonElement): MutableList<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        schema.validate(connectorConfiguration, errors::add)
        return errors
    }

    override fun check(connectorConfiguration: JsonElement, connectorName: String): Collection<PolicyViolation> {
        return matchesSchema(connectorConfiguration).map {
            PolicyViolation(message = it.message, check = this.javaClass.name)
        }
    }

    override fun toString(): String {
        return "JsonSchemaCheck(schema=$schema)"
    }


    companion object {
        private val metaSchema =
            JsonSchema.fromDefinition(JsonSchemaCheck::class.java.getResource("/meta.json-schema.json")!!.readText())

        private fun parseSchemaString(schemaString: String): JsonSchema {
            val schema = Json.parseToJsonElement(schemaString)
            val errors = mutableListOf<ValidationError>()
            if (metaSchema.validate(schema, errors::add)) {
                return JsonSchema.fromJsonElement(schema)
            } else {
                val errorMsg = errors.joinToString { "${it.message} (path ${it.schemaPath})" }
                throw IllegalArgumentException("Policy schema is not valid: $errorMsg")
            }

        }
    }


}
