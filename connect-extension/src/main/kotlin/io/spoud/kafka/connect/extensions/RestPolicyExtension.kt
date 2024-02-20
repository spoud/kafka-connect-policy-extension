package io.spoud.kafka.connect.extensions

import kotlinx.serialization.Serializable
import org.apache.kafka.connect.rest.ConnectRestExtension
import org.apache.kafka.connect.rest.ConnectRestExtensionContext
import org.slf4j.LoggerFactory

class RestPolicyExtension : ConnectRestExtension {

    override fun configure(configs: MutableMap<String, *>) {
        // extract file location for validation rules
        // read validation rules
        LOGGER.info("configuring the RestPolicyExtension")
        if(!configs.contains(CONF_FILE_PATH_PROPERTY)) {
            LOGGER.warn("Configuration $CONF_FILE_PATH_PROPERTY is not set")
        }
        PolicyConfiguration.configure(configs)
    }

    override fun version(): String = "1"

    override fun close() {
    }

    override fun register(restPluginContext: ConnectRestExtensionContext) {
        restPluginContext.configurable()
            .register(ConnectorConfigRequestFilter())
            .register(PoliciesResource())
    }

    companion object {
        const val CONF_FILE_PATH_PROPERTY = "rest.policy.conf"
        private val LOGGER = LoggerFactory.getLogger(RestPolicyExtension::class.java)
    }
}

@Serializable
data class ConfCheck(val name: String?, val className: String?, val configFile: String?)
