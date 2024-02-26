package io.spoud.kafka.connect.extensions

import io.spoud.kafka.connect.extensions.check.RegexCheck
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RegexPolicyCheckTest {


    private lateinit var regexTest: String

    @BeforeEach
    fun setUp() {
        val regexFile = javaClass.getResource("/regex-configuration.json")
        check(regexFile != null)
        this.regexTest = regexFile.readText()
    }

    @Test
    fun `Should not return any errors if configuration is correct`() {
        val missingContact = Json.parseToJsonElement(
            """
            {
                "connector.class": "org.apache.kafka.connect.tools.MockSinkConnector",
                "tasks.max": 1,
                "topics": "test-topic",
                "contact.email": "team-x@company.com",
                "creationDate" : "01.01.1970"
            }
        """.trimIndent()
        )
        val errors = RegexCheck(regexTest).check(missingContact, "connector1")
        assertThat(errors).hasSize(0)
    }

    @Test
    fun `should fail on bad email`() {
        val contactValid = Json.parseToJsonElement(
            """
            {
                "connector.class": "org.apache.kafka.connect.tools.MockSinkConnector",
                "tasks.max": 1,
                "topics": "test-topic",
                "contact.email": "hello this is not a valid email",
                "contact.teams": "https://teams-channel-link",
                "creationDate": "01.01.1970"
            }
        """.trimIndent()
        )
        val errors = RegexCheck(regexTest).check(contactValid, "connector1")
        assertThat(errors).hasSize(1)
        assertThat(errors.first().message).isEqualTo("valid email is requested")
    }

    @Test
    fun `should fail on missing field`() {
        val contactValid = Json.parseToJsonElement(
            """
            {
                "connector.class": "org.apache.kafka.connect.tools.MockSinkConnector",
                "tasks.max": 1,
                "topics": "test-topic",
                "contact.email": "name@company.com",
                "contact.teams": "https://teams-channel-link"
            }
        """.trimIndent()
        )
        val errors = RegexCheck(regexTest).check(contactValid, "connector1")
        assertThat(errors).hasSize(1)
        assertThat(errors.first().message).isEqualTo("creationDate has to be present and have be in \"mm.dd.yyyy\" format")
    }
    @Test
    fun `should fail on bad value`() {
        val contactValid = Json.parseToJsonElement(
            """
            {
                "connector.class": "org.apache.kafka.connect.tools.MockSinkConnector",
                "tasks.max": "this is not a number",
                "topics": "test-topic",
                "contact.email": "name@company.com",
                "contact.teams": "https://teams-channel-link",
                "creationDate": "01.01.1970"
            }
        """.trimIndent()
        )
        val errors = RegexCheck(regexTest).check(contactValid, "connector1")
        assertThat(errors).hasSize(1)
        assertThat(errors.first().message).isEqualTo("Field tasks.max should be present and have a numerical value")
    }

    @Test
    fun `should not fail on not mandatory field missing`() {
        val contactValid = Json.parseToJsonElement(
            """
            {
                "connector.class": "org.apache.kafka.connect.tools.MockSinkConnector",
                
                "topics": "test-topic",
                "contact.email": "name@company.com",
                "contact.teams": "https://teams-channel-link",
                "creationDate": "01.01.1970"
            }
        """.trimIndent()
        )
        val errors = RegexCheck(regexTest).check(contactValid, "connector1")
        assertThat(errors).hasSize(0)
    }
}
