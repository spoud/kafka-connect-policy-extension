package io.spoud.kafka.connect.extensions

import io.spoud.kafka.connect.extensions.RestPolicyExtension.Companion.CONF_FILE_PATH_PROPERTY
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileNotFoundException

class RestPolicyExtensionTest {


    private lateinit var extension: RestPolicyExtension

    @BeforeEach
    fun setUp() {
        val confFile = javaClass.getResource("/rest-policy-config.json")
        check(confFile != null)

        extension = RestPolicyExtension()
        extension.configure(mutableMapOf(CONF_FILE_PATH_PROPERTY to confFile.path))
    }

    @Test
    fun `should fail when path to json config is wrong`() {
        assertThatThrownBy {
            RestPolicyExtension().configure(mutableMapOf(CONF_FILE_PATH_PROPERTY to "/invalid/path/file.json"))
        }
            .isInstanceOf(FileNotFoundException::class.java)
            .hasMessageContaining("/invalid/path/file.json (No such file or directory")

        //TODO fails on windows because of \ instead of /
    }

    @Test
    fun `should fail when config policy file is invalid`() {
        val badConfFile = javaClass.getResource("/bad-extension-config.json")?.path

        // when
        assertThatThrownBy {
            RestPolicyExtension().configure(mutableMapOf(CONF_FILE_PATH_PROPERTY to badConfFile))
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Policy schema is not valid")
    }
}
