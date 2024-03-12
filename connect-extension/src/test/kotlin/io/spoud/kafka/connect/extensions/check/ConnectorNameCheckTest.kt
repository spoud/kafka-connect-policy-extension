package io.spoud.kafka.connect.extensions.check

import kotlinx.serialization.json.JsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConnectorNameCheckTest {

    @Test
    fun `should validate connector name with regex`() {
        val regexNameCheck = ConnectorNameCheck().apply {
            configure(mapOf("regex" to "^((dev)|(tst)|(prd))\\..+$"))
        }

        var result = regexNameCheck.check(JsonPrimitive(""), "my-dev-connector")
        println(result)
        assertThat(result.size).isEqualTo(1)

        result = regexNameCheck.check(JsonPrimitive(""), "tst.mydevconnector")
        assertThat(result.size).isEqualTo(0)
    }
}
