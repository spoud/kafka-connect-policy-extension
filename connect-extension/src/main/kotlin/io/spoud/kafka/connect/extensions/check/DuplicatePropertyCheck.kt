package io.spoud.kafka.connect.extensions.check

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DuplicatePropertyCheck(private val conf: String) : PolicyCheck {

    private val effect: Effect = parseConfig(conf)

    override fun check(connectorConfiguration: JsonElement, connectorName: String): Collection<PolicyViolation> {
        //FIXME currently this check won't work because JsonElement will eliminate duplicates before we can check

        val duplicates = lookForDuplicate(connectorConfiguration.jsonObject)

        duplicates.forEach {
            LOGGER.warn("$it is present multiple times in the configuration")
        }

        return if (duplicates.isNotEmpty() && effect == Effect.FAIL) duplicates.map {
            PolicyViolation("Duplicate property: $it", this.javaClass.name)
        } else emptyList()
    }

    private fun lookForDuplicate(jsonObject: JsonObject): Set<String> {
        val properties = mutableSetOf<String>()
        val duplicates = mutableSetOf<String>()
        for (propertyname in jsonObject.keys)
            if (propertyname in properties) duplicates.add(propertyname) else properties.add(propertyname)

        return duplicates
    }


    override fun toString(): String {
        return "DuplicateConfigurationCheck(schema=$conf)"
    }

    companion object {

        private val LOGGER: Logger = LoggerFactory.getLogger(DuplicatePropertyCheck::class.java)
        private fun parseConfig(conf: String): Effect {
            return when (Json.decodeFromString<Configuration>(conf).effect) {
                "FAIL" -> Effect.FAIL
                "WARN" -> Effect.WARN
                else -> throw IllegalArgumentException("Invalid effect for DuplicatePropertyCheck")
            }
        }

        @Serializable
        class Configuration(val effect: String) {
            override fun toString(): String {
                return "DuplicateConfiguration(effect=$effect)"
            }
        }

        enum class Effect { FAIL, WARN }
    }


}
