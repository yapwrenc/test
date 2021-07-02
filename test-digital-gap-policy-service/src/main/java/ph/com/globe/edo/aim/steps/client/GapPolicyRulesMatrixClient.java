package ph.com.globe.edo.aim.steps.client;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "gap-policy-matrix")
public interface GapPolicyRulesMatrixClient {

	@POST
	@Path("/RulesProcess")
	@Consumes(MediaType.APPLICATION_JSON)
	public String retrieveRulesMatrix(String subGapPolicyMap);
}
