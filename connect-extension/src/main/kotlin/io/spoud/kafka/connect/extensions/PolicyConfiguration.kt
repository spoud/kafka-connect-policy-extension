package io.spoud.kafka.connect.extensions

import io.spoud.kafka.connect.extensions.check.ConnectorNameCheck
import io.spoud.kafka.connect.extensions.check.JsonSchemaCheck
import io.spoud.kafka.connect.extensions.check.PolicyCheck
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

object PolicyConfiguration {
    private lateinit var configs: MutableMap<String, *>
    private val LOGGER = LoggerFactory.getLogger(PolicyConfiguration::class.java)
    val policies: MutableList<PolicyCheck> = mutableListOf()


    fun configure(configs: MutableMap<String, *>) {
        this.configs = configs
        reload()
    }

    fun reload(): MutableList<PolicyCheck> {
        if (configs.contains(RestPolicyExtension.CONF_FILE_PATH_PROPERTY)) {
            val file = configurationFile()
            val jsonString = file.readText()
            val confChecks = Json.decodeFromString<List<ConfigurationEntry>>(jsonString)

            policies.clear()
            policies.addAll(confChecks.map(::mapToPolicyCheck))

            LOGGER.info("configured {} policies", policies.size)
        }
        return policies
    }

    fun configurationFile() = File(configs[RestPolicyExtension.CONF_FILE_PATH_PROPERTY].toString())

    private fun mapToPolicyCheck(configurationEntry: ConfigurationEntry) =
        when (configurationEntry.className) {
            JsonSchemaCheck::class.simpleName -> {
                JsonSchemaCheck(configurationFile().parentFile).apply { configure(configurationEntry.config) }
            }

            ConnectorNameCheck::class.simpleName -> {
                ConnectorNameCheck().apply { configure(configurationEntry.config) }
            }

            else -> {
                throw IllegalArgumentException("no implementation for ${configurationEntry.className} found")
            }
        }
}


