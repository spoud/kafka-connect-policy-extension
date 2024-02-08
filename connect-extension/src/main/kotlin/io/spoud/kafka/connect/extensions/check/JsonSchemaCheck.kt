package io.spoud.kafka.connect.extensions.check

import io.github.optimumcode.json.schema.JsonSchema
import io.github.optimumcode.json.schema.ValidationError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class JsonSchemaCheck(schemaString: String) : PolicyCheck {

    private val schema: JsonSchema = parseSchemaString(schemaString)

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

    companion object {
        private val metaSchema = JsonSchema.fromDefinition(JsonSchemaCheck::class.java.getResource("/meta.json-schema.json")!!.readText())

        private fun parseSchemaString(schemaString: String) : JsonSchema {
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
