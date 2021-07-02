package ph.com.globe.edo.aim.steps.service;

import java.time.LocalDate;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.ConfigProvider;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.ResultCode;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;

import lombok.extern.slf4j.Slf4j;
import ph.com.globe.edo.aim.steps.dto.SubGapPolicyDTO;
import ph.com.globe.edo.aim.steps.dto.SubGapPolicyResponse;

@Slf4j
@ApplicationScoped
public class SubGapPolicyService {
	
	public SubGapPolicyResponse upsertSubGapPolicy(List<SubGapPolicyDTO> requestBodyList) {
		 AerospikeClient client = new AerospikeClient(ConfigProvider.getConfig().getValue("aerospike.server", String.class), 
				 ConfigProvider.getConfig().getValue("aerospike.port", Integer.class));
		 try {
			 WritePolicy writePolicy = new WritePolicy();
			 writePolicy.sendKey = true;
			 writePolicy.expiration = -1;
			 for(SubGapPolicyDTO subGapPolicyDTO:requestBodyList) {
				 try {
					 writePolicy.recordExistsAction = RecordExistsAction.CREATE_ONLY;
					 saveSubGapPolicy(client, writePolicy, subGapPolicyDTO,false);
				 }catch (AerospikeException e) {
					 if(e.getResultCode() == ResultCode.KEY_EXISTS_ERROR) {
						 writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
						 saveSubGapPolicy(client, writePolicy, subGapPolicyDTO,true);
					 }
				}
			 }
		 }catch (Exception e) {
			log.error(e.getMessage());
		}finally {
			client.close();
		}
		return new SubGapPolicyResponse(null, null);
	}
	
	public void saveSubGapPolicy(AerospikeClient client, WritePolicy writePolicy,SubGapPolicyDTO subGapPolicyDTO,Boolean isExist) {
		Key key = new Key(ConfigProvider.getConfig().getValue("aerospike.namespace", String.class), 
		ConfigProvider.getConfig().getValue("subsgappolicy.set", String.class), subGapPolicyDTO.getMsisdn());
		client.put(writePolicy, key, 
				new Bin("MSISDN", subGapPolicyDTO.getMsisdn()), 
				new Bin("GP_MKTG", "MKT_ACT"),
				new Bin("GP_CHANNEL", "CHANNEL"),
				new Bin("GP_REF_DT", LocalDate.now().toString()),
				new Bin("GP_RESPOND", "1"),
				isExist?new Bin("LAST_UPDATE_DT", LocalDate.now().toString()):new Bin("REC_CREATE_DTTM", LocalDate.now().toString()));
	}
}