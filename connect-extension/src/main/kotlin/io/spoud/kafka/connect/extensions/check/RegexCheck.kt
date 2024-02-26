package io.spoud.kafka.connect.extensions.check

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class RegexCheck(private val regexListString: String) : PolicyCheck {

    private val regexValidations: List<RegexValidation> = parseRegexListString(regexListString)

    override fun check(connectorConfiguration: JsonElement, connectorName: String): Collection<PolicyViolation> {
        return regexValidations.fold(mutableListOf()) { errors, rv ->
            check(connectorConfiguration.jsonObject, errors, rv)
        }
    }

    private fun check(obj : JsonObject, errors : MutableList<PolicyViolation>, rv : RegexValidation) : MutableList<PolicyViolation> {
        if (rv.fieldName in obj) {
            if (!Regex(rv.regex).matches(obj[rv.fieldName].toString())) {
                errors.add(PolicyViolation(rv.message, this.javaClass.name))
            }
        } else {
            if (rv.mandatory) {
                errors.add(PolicyViolation(rv.message, this.javaClass.name))
            }
        }
        return errors
    }


    override fun toString(): String {
        return "RegexCheck(schema=$regexListString)"
    }

    companion object {
        private fun parseRegexListString(regexListString: String): List<RegexValidation> {
            return Json.decodeFromString<List<RegexValidation>>(regexListString)
        }

        @Serializable
        class RegexValidation(val fieldName: String, val regex: String, val mandatory: Boolean, val message: String) {
            override fun toString(): String {
                return "RegexValidation(fieldName=$fieldName, mandatory=$mandatory, regex=$regex)"
            }
        }
    }


}
