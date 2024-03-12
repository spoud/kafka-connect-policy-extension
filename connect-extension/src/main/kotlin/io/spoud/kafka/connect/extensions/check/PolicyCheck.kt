package io.spoud.kafka.connect.extensions.check

import kotlinx.serialization.json.JsonElement

interface PolicyCheck {

    /**
     * @param connectorConfiguration Json configuration that was submitted to the rest endpoint
     * @param connectorName name of the connector
     * @return list of policy violations, an empty list means that the config passed all rules
     */
    fun check(connectorConfiguration: JsonElement, connectorName: String): Collection<PolicyViolation>

    fun configure(config: Map<String, String>)
}
