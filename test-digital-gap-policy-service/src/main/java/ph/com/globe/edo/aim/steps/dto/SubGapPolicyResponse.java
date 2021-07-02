package ph.com.globe.edo.aim.steps.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubGapPolicyResponse {
	String status;
	String message;
	
	public SubGapPolicyResponse(String status, String message){
		this.setStatus(status);
		this.setMessage(message);
	}
}