package io.spoud.kafka.connect.extensions

import io.spoud.kafka.connect.extensions.check.PolicyViolation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class ConnectorConfigRequestFilterTest {

    private lateinit var filter: ConnectorConfigRequestFilter
    private val requiresConfigCheckMethod = ConnectorConfigRequestFilter::class.java.getDeclaredMethod(
        "requiresConfigCheck",
        ContainerRequestContext::class.java
    )
    private val mockContainerRequestContext = mock(ContainerRequestContext::class.java, RETURNS_DEEP_STUBS)

    @BeforeEach
    fun setUp() {
        filter = ConnectorConfigRequestFilter()
        requiresConfigCheckMethod.isAccessible = true
    }

    @Test
    fun `should abort context when errors found`() {
        // given
        prepapreMock("POST", "connectors")
        `when`(mockContainerRequestContext.entityStream).thenReturn("{}".trimIndent().byteInputStream())
        PolicyConfiguration.policies.add(MockPolicyCheck(PolicyViolation("violation", "mock-check")))
        val responseCaptor = ArgumentCaptor.forClass(Response::class.java)

        // when
        filter.filter(mockContainerRequestContext)

        // then
        verify(mockContainerRequestContext, times(1)).abortWith(responseCaptor.capture())
        assertThat(responseCaptor.value.status).isEqualTo(400)
        assertThat(responseCaptor.value.mediaType).isEqualTo(MediaType.APPLICATION_JSON_TYPE)
    }

    @Test
    fun `should do nothing when config is valid`() {
        // given
        prepapreMock("POST", "connectors")
        `when`(mockContainerRequestContext.entityStream).thenReturn("{}".trimIndent().byteInputStream())
        PolicyConfiguration.policies.add(MockPolicyCheck(null))

        // when
        filter.filter(mockContainerRequestContext)

        // then
        verify(mockContainerRequestContext, times(0)).abortWith(any())
    }

    @Test
    fun `should extract connector name from URL`() {
        // given
        prepapreMock("PUT", "connectors/my-test-connector/config")
        `when`(mockContainerRequestContext.entityStream).thenReturn("{}".trimIndent().byteInputStream())
        val mockPolicyCheck = MockPolicyCheck(null)
        PolicyConfiguration.policies.add(mockPolicyCheck)
        // when
        filter.filter(mockContainerRequestContext)
        // then
        assertThat(mockPolicyCheck.connectorName).isEqualTo("my-test-connector")
    }

    @Test
    fun `should extract connector name from json payload`() {
        // given
        prepapreMock("POST", "connectors")
        `when`(mockContainerRequestContext.entityStream).thenReturn(
            """{
                    "name": "my-test-connector",
                    "config": {}
            }""".trimIndent().byteInputStream()
        )
        val mockPolicyCheck = MockPolicyCheck(null)
        PolicyConfiguration.policies.add(mockPolicyCheck)
        // when
        filter.filter(mockContainerRequestContext)
        // then
        assertThat(mockPolicyCheck.connectorName).isEqualTo("my-test-connector")
    }

    @Test
    fun `should not be invoked for DELETE requests`() {
        prepapreMock("DELETE", "/connectors/my-connector-to-delete/")

        filter.filter(mockContainerRequestContext)
        val res = requiresConfigCheckMethod.invoke(filter, mockContainerRequestContext)
        if (res is Boolean) assertFalse(res, "Filter should not be applied")
    }

    @Test
    fun `should be invoked for validating requests`() {
        prepapreMock("PUT", "connector-plugins/my-connector-name/config/validate")

        val res = requiresConfigCheckMethod.invoke(filter, mockContainerRequestContext)
        if (res is Boolean) assertTrue(res, "Filter should be applied")
    }

    @Test
    fun `should be invoked for validating requests, with a slash at the end`() {
        prepapreMock("PUT", "connector-plugins/this-is-a.strange.name/config/validate/")

        val res = requiresConfigCheckMethod.invoke(filter, mockContainerRequestContext)
        println(res)
        if (res is Boolean) assertTrue(res, "Filter should be applied")
    }

    @Test
    fun `should be invoked for post new configs requests`() {
        prepapreMock("POST", "connectors")

        val res = requiresConfigCheckMethod.invoke(filter, mockContainerRequestContext)
        if (res is Boolean) assertTrue(res, "Filter should be applied")
    }

    private fun prepapreMock(method: String, path: String) {
        `when`(mockContainerRequestContext.uriInfo.path).thenReturn(path)
        `when`(mockContainerRequestContext.method).thenReturn(method)
    }

    @AfterEach
    fun cleanMocks() {
        reset(mockContainerRequestContext)
    }
}
