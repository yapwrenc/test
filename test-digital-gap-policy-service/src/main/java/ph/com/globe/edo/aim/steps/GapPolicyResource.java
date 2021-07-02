package ph.com.globe.edo.aim.steps;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import lombok.extern.slf4j.Slf4j;
import ph.com.globe.edo.aim.steps.dto.GapPolicyDTO;
import ph.com.globe.edo.aim.steps.dto.GapPolicyWhiteListRequestBody;
import ph.com.globe.edo.aim.steps.service.GapPolicyService;

@Slf4j
@Path("/gap-policy-service")
@RegisterRestClient(configKey = "gap-policy-service")
public class GapPolicyResource {

	@Inject
	private GapPolicyService service;

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public List<GapPolicyDTO> processGapPolicy(List<GapPolicyWhiteListRequestBody> requestBodyList) {
		log.info("Processing Gap Policy Check:");
		log.info("RequestBodyList size: {}", requestBodyList.size());

		return service.processEvent(requestBodyList);
	}
}