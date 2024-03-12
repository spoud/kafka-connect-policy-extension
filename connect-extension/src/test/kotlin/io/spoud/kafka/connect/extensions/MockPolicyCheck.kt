package io.spoud.kafka.connect.extensions

import io.spoud.kafka.connect.extensions.check.PolicyCheck
import io.spoud.kafka.connect.extensions.check.PolicyViolation
import kotlinx.serialization.json.JsonElement

class MockPolicyCheck(
    private val policyViolation: PolicyViolation?,
) : PolicyCheck {
    private var _connectorName: String? = null
    val connectorName
        get() = _connectorName

    override fun check(connectorConfiguration: JsonElement, connectorName: String): Collection<PolicyViolation> {
        this._connectorName = connectorName
        return if(policyViolation != null) {
            listOf(policyViolation)
        } else {
            emptyList()
        }
    }
}
