package io.spoud.kafka.connect.extensions

import io.spoud.kafka.connect.extensions.check.JsonSchemaCheck
import io.spoud.kafka.connect.extensions.check.PolicyCheck
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.kafka.connect.rest.ConnectRestExtension
import org.apache.kafka.connect.rest.ConnectRestExtensionContext
import org.slf4j.LoggerFactory
import java.io.File

class RestPolicyExtension : ConnectRestExtension {

    private var policies: MutableList<PolicyCheck> = mutableListOf()

    override fun configure(configs: MutableMap<String, *>) {
        // extract file location for validation rules
        // read validation rules
        LOGGER.info("configuring the RestPolicyExtension")

        if (configs.contains(CONF_FILE_PATH_PROPERTY)) {
            val file = File(configs[CONF_FILE_PATH_PROPERTY].toString())
            val jsonString = file.readText()

            val confChecks = Json.decodeFromString<List<ConfCheck>>(jsonString)

            policies.addAll(confChecks.map {
                val path = "${file.parentFile.path}/${it.configFile}"
                if (it.className == JsonSchemaCheck::class.qualifiedName) {
                    JsonSchemaCheck(File(path).readText())
                } else {
                    throw IllegalArgumentException("no implementation for ${it.className} found")
                }
            })

            LOGGER.info("configured {} policies", policies.size)
        }

    }

    override fun version(): String = "1"

    override fun close() {
    }

    override fun register(restPluginContext: ConnectRestExtensionContext) {
        restPluginContext.configurable().register(ConnectorConfigRequestFilter(policies))
    }

    fun policiesSize(): Int = policies.size

    companion object {
        const val CONF_FILE_PATH_PROPERTY = "rest.policy.conf"
        private val LOGGER = LoggerFactory.getLogger(RestPolicyExtension::class.java)
    }
}

@Serializable
data class ConfCheck(val name: String, val className: String, val configFile: String)
