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
import ph.com.globe.edo.aim.steps.dto.SubGapPolicyDTO;
import ph.com.globe.edo.aim.steps.dto.SubGapPolicyResponse;
import ph.com.globe.edo.aim.steps.service.SubGapPolicyService;

@Slf4j
@Path("/sub-gap-policy-service")
@RegisterRestClient(configKey = "sub-gap-policy-service")
public class SubsGapPolicyResource {

	@Inject
	private SubGapPolicyService service;
	
	@POST
	@Path("/add")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public SubGapPolicyResponse addEditSubGapPolicy(List<SubGapPolicyDTO> requestBodyList) {
		log.info("Adding/Updating Sub Gap Policy:");
		log.info("RequestBodyList size: {}", requestBodyList.size());
		return service.upsertSubGapPolicy(requestBodyList);
	}
}