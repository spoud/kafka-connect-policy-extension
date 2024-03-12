package io.spoud.kafka.connect.extensions

import io.spoud.kafka.connect.extensions.check.JsonSchemaCheck
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class JsonSchemaPolicyCheckTest {


    private lateinit var config: Map<String, String>
    private lateinit var confDir: File

    @BeforeEach
    fun setUp() {
        val schemaFile = javaClass.getResource("/require-contact-info.json")
        check(schemaFile != null)
        confDir = File(schemaFile.file).parentFile
        config = mapOf("file" to File(schemaFile.file).name)
    }

    @Test
    fun `should return errors when fields are missing`() {
        val missingContact = Json.parseToJsonElement(
            """
            {
                "connector.class": "org.apache.kafka.connect.tools.MockSinkConnector",
                "tasks.max": 1,
                "topics": "test-topic"
            }
        """.trimIndent()
        )
        val errors = JsonSchemaCheck(confDir).apply {
            configure(config)
        }.check(missingContact, "connector1")
        assertThat(errors).hasSize(1)
        assertThat(errors.first().message).isEqualTo("missing required properties: [contact.email, contact.teams]")
    }

    @Test
    fun `should pass when required fields are present`() {
        val contactValid = Json.parseToJsonElement(
            """
            {
                "connector.class": "org.apache.kafka.connect.tools.MockSinkConnector",
                "tasks.max": 1,
                "topics": "test-topic",
                "contact.email": "team-x@company.com",
                "contact.teams": "https://teams-channel-link"
            }
        """.trimIndent()
        )
        val errors = JsonSchemaCheck(confDir).apply {
            configure(config)
        }.check(contactValid, "connector1")
        assertThat(errors).hasSize(0)
    }
}
