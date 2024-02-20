package io.spoud.kafka.connect.extensions

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import javax.ws.rs.container.ContainerRequestContext
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
    fun `should not be invoked for DELETE requests`() {
        prepapreMock("DELETE", "/connectors/my-connector-to-delete/")

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
        prepapreMock("POST", "connectors/new-connector-name")

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
