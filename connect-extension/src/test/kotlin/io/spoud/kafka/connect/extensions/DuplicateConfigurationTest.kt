package io.spoud.kafka.connect.extensions

import io.spoud.kafka.connect.extensions.check.DuplicatePropertyCheck
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DuplicateConfigurationTest {


    private lateinit var duplicatePropertiesConfig: String

    @BeforeEach
    fun setUp() {
        val regexFile = javaClass.getResource("/duplicate-configuration.json")
        check(regexFile != null)
        this.duplicatePropertiesConfig = regexFile.readText()
    }

    @Test
    fun `Should not return any errors if configuration is correct`() {
        val multipleProperty = Json.parseToJsonElement(
            """
            {
                "connector.class": "org.apache.kafka.connect.tools.MockSinkConnector",
                "tasks.max": 1,
                "tasks.max": 2,
                "tasks.max": 3,
                "topics": "test-topic",
                "contact.email": "team-x@company.com",
                "creationDate" : "01.01.1970"
            }
        """.trimIndent()
        )
        val errors = DuplicatePropertyCheck(duplicatePropertiesConfig).check(multipleProperty, "connector1")
        assertThat(errors).hasSize(2)
    }

}
