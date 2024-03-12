package io.spoud.kafka.connect.extensions.check

import kotlinx.serialization.json.JsonElement

class ConnectorNameCheck() : PolicyCheck {


    private lateinit var nameRegex: Regex

    override fun configure(config: Map<String, String>) {
        this.nameRegex = config["regex"]?.toRegex()
            ?: throw IllegalArgumentException("Please specify a regex pattern. e.g. with \"config\": {\"regex\": \".*\"}")

    }
    override fun check(connectorConfiguration: JsonElement, connectorName: String): Collection<PolicyViolation> {
        if (!nameRegex.matches(connectorName)) {
            return listOf(
                PolicyViolation(
                    message = "Connector name '$connectorName' does not match the required pattern '${nameRegex.pattern}'",
                    check = this.javaClass.name
                )
            )
        }
        return emptyList()
    }

    override fun toString(): String {
        return "ConnectorNameCheck(regex=${nameRegex.pattern})"
    }


}
