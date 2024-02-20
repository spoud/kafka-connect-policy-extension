package io.spoud.kafka.connect.extensions

import io.spoud.kafka.connect.extensions.check.PolicyCheck
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.util.stream.Collectors
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST


class ConnectorConfigRequestFilter() : ContainerRequestFilter {

    private fun requiresConfigCheck(context: ContainerRequestContext): Boolean {
        val putConfig = context.method == "PUT" && CONFIG_REGEX.matches(context.uriInfo.path)
        val postNew = context.method == "POST" && POST_NEW_REGEX.matches(context.uriInfo.path)
        val validate = context.method == "PUT" && VALIDATE_REGEX.matches(context.uriInfo.path)
        return putConfig || postNew || validate
    }

    override fun filter(context: ContainerRequestContext) {
        if (requiresConfigCheck(context)) {
            LOGGER.debug("Policy check filter invoked for ${context.method} ${context.uriInfo.path}")
            val connectorConf = readConnectorConfig(context)
            val violations = PolicyConfiguration.policies.stream()
                .flatMap { it.check(connectorConf.config, connectorConf.name).stream() }
                .collect(Collectors.toList())
            if (violations.isNotEmpty()) {
                LOGGER.info("Policy check violations  found: {}", violations)
                val response = Response
                    .status(BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(violations)
                    .build()
                context.abortWith(response)
            }
        }
    }

    private fun readConnectorConfig(context: ContainerRequestContext): ConnectorConfig {
        var requestBody = readRequestBody(context)
        val connectorName: String
        if (context.method == "POST") {
            connectorName = requestBody.jsonObject["name"].toString()
            requestBody = requestBody.jsonObject["config"] ?: JsonObject(emptyMap())
        } else {
            connectorName = CONFIG_REGEX.find(context.uriInfo.path)?.groups?.get(0).toString()
        }
        return ConnectorConfig(name = connectorName, config = requestBody)
    }

    /**
     * Deserializes the request body to a JsonElement and resets the input stream, so that the Kafka Connect resource
     * handler can re-read the body.
     *
     * @param context request context
     */
    private fun readRequestBody(context: ContainerRequestContext): JsonElement {
        var stream = context.entityStream
        if (!stream.markSupported()) {
            stream = BufferedInputStream(stream)
        }
        stream.mark(0)
        val connectorConfiguration = Json.parseToJsonElement(String(stream.readAllBytes()))
        context.entityStream = stream.also { it.reset() }
        return connectorConfiguration
    }

    data class ConnectorConfig(val name: String, val config: JsonElement)

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(ConnectorConfigRequestFilter::class.java)
        private val CONFIG_REGEX: Regex = Regex("^connectors/.+/config/?$")
        private val POST_NEW_REGEX: Regex = Regex("^connectors/.+/?$")
        private val VALIDATE_REGEX: Regex = Regex("^connector-plugins/.+/config/validate/?$")
    }
}
