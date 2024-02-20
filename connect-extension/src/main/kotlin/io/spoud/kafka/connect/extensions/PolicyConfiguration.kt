package io.spoud.kafka.connect.extensions

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
            val file = File(configs[RestPolicyExtension.CONF_FILE_PATH_PROPERTY].toString())
            val jsonString = file.readText()
            val confChecks = Json.decodeFromString<List<ConfCheck>>(jsonString)

            policies.clear()
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
        return policies
    }
}


