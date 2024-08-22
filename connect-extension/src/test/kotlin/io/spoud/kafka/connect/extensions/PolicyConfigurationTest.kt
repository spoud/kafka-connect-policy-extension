package io.spoud.kafka.connect.extensions

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.util.Files
import org.junit.jupiter.api.Test

class PolicyConfigurationTest {

    @Test
    fun `should load configuration`() {
        val confFile = javaClass.getResource("/rest-policy-config-regex.json")
        check(confFile != null)
        PolicyConfiguration.configure(mutableMapOf(RestPolicyExtension.CONF_FILE_PATH_PROPERTY to confFile.path))

        assertThat(PolicyConfiguration.policies).hasSize(1)
    }

    @Test
    fun `should reload configuration changes when json files are updated`() {
        // given configuration with 1 policy
        val requireContactInfoJson = javaClass.getResource("/require-contact-info.json")
        val restPolicyJson = javaClass.getResource("/rest-policy-config.json")
        val contactInfoCheck = Files.newTemporaryFile().apply {
            check(requireContactInfoJson != null)
            writeBytes(requireContactInfoJson.readBytes())

        }
        val config = Files.newTemporaryFile().apply {
            check(restPolicyJson != null)
            writeText(restPolicyJson.readText().replace("require-contact-info.json", contactInfoCheck.name))

        }
        PolicyConfiguration.configure(mutableMapOf(RestPolicyExtension.CONF_FILE_PATH_PROPERTY to config.path))
        assertThat(PolicyConfiguration.policies).hasSize(1)

        // when json config changes before reload
        config.writeText("[]")
        PolicyConfiguration.reload()
        // then policy count should be updated
        assertThat(PolicyConfiguration.policies).hasSize(0)
    }

}
