package io.spoud.kafka.connect.extensions

import io.spoud.kafka.connect.extensions.check.PolicyCheck
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Path("policies")
@Produces("application/json")
class PoliciesResource {

    @GET
    fun getPolicies(): List<String> {
        return PolicyConfiguration.policies.map { it.toString() }
    }

    @PUT
    @Path("reload")
    fun reloadConfig(): List<String> {
        return PolicyConfiguration.reload().map { it.toString() }
    }

}
