package ph.com.globe.edo.aim.steps.service;

import static ph.com.globe.edo.aim.steps.component.utility.StepsConstants.MA_ID_BIN;
import static ph.com.globe.edo.aim.steps.component.utility.StepsConstants.MSISDN_BIN;
import static ph.com.globe.edo.aim.steps.component.utility.StepsConstants.OFFER_DATE_BIN;
import static ph.com.globe.edo.aim.steps.component.utility.StepsConstants.OFFER_VALIDITY_BIN;
import static ph.com.globe.edo.aim.steps.component.utility.StepsConstants.RESPONSE_DATE_BIN;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.springframework.util.StringUtils;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.BatchPolicy;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import ph.com.globe.edo.aim.steps.client.GapPolicyRulesMatrixClient;
import ph.com.globe.edo.aim.steps.component.utility.StepsDateTimeUtil;
import ph.com.globe.edo.aim.steps.dto.GapPolicyDTO;
import ph.com.globe.edo.aim.steps.dto.GapPolicyWhiteListRequestBody;
import ph.com.globe.edo.aim.steps.dto.RulesMatrixDTO;

@Slf4j
@ApplicationScoped
public class GapPolicyService {

	@Inject
	@RestClient
	private GapPolicyRulesMatrixClient gapPolicyRulesMatrixClient;

	@ConfigProperty(name = "aerospike.server")
	private String aeroSpikeServer;

	@ConfigProperty(name = "aerospike.port")
	private int aeroSpikePort;

	@ConfigProperty(name = "aerospike.namespace")
	private String aeroSpikeNameSpace;

	@ConfigProperty(name = "subsgappolicy.set")
	private String subsGapPolicy;

	private static final String RESPONSE = "response";
	private static final String RESPONDER = "R";
	private static final String NON_RESPONDER = "NR";
	private static final String RECORDS = "records";
	private static final String RECORDLIST = "recordList";
	private static final String GAP = "gap";
	private static final int GAP_POLICY = 1;
	private static final int OUTSIDE_GAP_POLICY = 0;

	public List<GapPolicyDTO> processEvent(List<GapPolicyWhiteListRequestBody> requestBodyList) {

		List<GapPolicyDTO> gappolicyList = new ArrayList<>();
		List<String> msisdnList = new ArrayList<>();

		try {
			for (GapPolicyWhiteListRequestBody req : requestBodyList) {
				msisdnList.add(req.getMsisdn());
			}
			Record[] recordArr = retrieveSubsGapPolicyMapList(msisdnList, MSISDN_BIN, MA_ID_BIN, OFFER_DATE_BIN,
					OFFER_VALIDITY_BIN, RESPONSE_DATE_BIN);

			if(recordArr != null) {
				log.info("Retrieve size of subs gap policy list: {}", recordArr.length);
				List<RulesMatrixDTO> recordList = getRecordList(requestBodyList, recordArr);
				gappolicyList = processRulesMatrix(recordList, recordArr);
			}
		} catch (Exception e) {
			log.error("Error processing gap policy: " + e.getMessage());
		}
		log.info("Processed gap policy size: {}", gappolicyList.size());
		return gappolicyList;
	}

	private List<RulesMatrixDTO> getRecordList(List<GapPolicyWhiteListRequestBody> requestBodyList,
			Record[] recordArr) {

		List<RulesMatrixDTO> recordList = new ArrayList<>();

		for (int i = 0; i < recordArr.length; i++) {
			Record tempRec = recordArr[i];
			RulesMatrixDTO rulesMatrixDTO = new RulesMatrixDTO();

			if (tempRec != null) {
				if (null != requestBodyList.get(i)) {
					rulesMatrixDTO.setTo(requestBodyList.get(i).getMaId());
				}
				if (null != tempRec.getValue(MA_ID_BIN)) {
					rulesMatrixDTO.setFrom(tempRec.getValue(MA_ID_BIN).toString());
				}
				if (tempRec.getValue(RESPONSE_DATE_BIN) != null
						&& !StringUtils.isEmpty(tempRec.getValue(RESPONSE_DATE_BIN).toString())) {
					rulesMatrixDTO.setResponse(RESPONDER);
				} else {
					rulesMatrixDTO.setResponse(NON_RESPONDER);
				}
			}
			recordList.add(rulesMatrixDTO);
		}
		return recordList;
	}

	private Record[] retrieveSubsGapPolicyMapList(List<String> msisdnList, String... retrieveBins) {
		AerospikeClient client = new AerospikeClient(aeroSpikeServer, aeroSpikePort);
		Record[] records = null;

		try {

			List<Key> keys = new ArrayList<>();
			List<String> bins = new ArrayList<>();

			bins = Arrays.asList(retrieveBins);

			for (String msisdn : msisdnList) {
				keys.add(new Key(aeroSpikeNameSpace, subsGapPolicy, msisdn));
			}

			records = client.get(new BatchPolicy(), keys.toArray(new Key[keys.size()]),
					bins.toArray(new String[bins.size()]));

		} catch (AerospikeException ex) {
			log.error("Transaction failed due to:{} ", ex.getMessage());

		} finally {

			client.close();
		}

		return records;
	}

	private String createRuleMatrixRequest(List<RulesMatrixDTO> recordList) {
		String jsonListString = "";

		JsonArray jsonArray = new JsonArray(recordList);
		JsonObject jsonRulesObject = new JsonObject().put(RECORDS, jsonArray);
		jsonListString = new JsonObject().put(RECORDLIST, jsonRulesObject).toString();

		return jsonListString;
	}

	private List<GapPolicyDTO> processRulesMatrix(List<RulesMatrixDTO> recordList, Record[] recordArr) {

		List<GapPolicyDTO> gappolicyList = new ArrayList<>();

		// REQUEST TO RULES MATRIX
		String rulesMatrixData = gapPolicyRulesMatrixClient.retrieveRulesMatrix(createRuleMatrixRequest(recordList));

		// PROCESS RULES MATRIX RESPONSE
		JsonObject records = new JsonObject(rulesMatrixData);
		JsonObject rulesRecordList = new JsonObject(records.getValue(RECORDLIST).toString());
		JsonArray gapPolicyBatch = new JsonArray(rulesRecordList.getValue(RECORDS).toString());

		for (int i = 0; i < gapPolicyBatch.size(); i++) {
			JsonObject gapDetails = gapPolicyBatch.getJsonObject(i);

			GapPolicyDTO gapPolicyDTO = new GapPolicyDTO();

			if (recordArr[i] != null) {
				gapPolicyDTO.setMsisdn(recordArr[i].getValue(MSISDN_BIN).toString());

				LocalDateTime now = StepsDateTimeUtil.getPhDateTime().toLocalDateTime();
				LocalDateTime prevDate = null;

				if (RESPONDER.compareTo(gapDetails.getValue(RESPONSE).toString()) == 0) {
					prevDate = StepsDateTimeUtil.formatDateTime(recordArr[i].getValue(RESPONSE_DATE_BIN).toString());
				} else {
					prevDate = StepsDateTimeUtil.formatDateTime(recordArr[i].getValue(OFFER_VALIDITY_BIN).toString());
				}

				// Calculation for Gap Policy
				LocalDateTime endGapPolicyDate = prevDate.plusDays((int) gapDetails.getValue(GAP));

				if (now.isBefore(endGapPolicyDate)) {
					gapPolicyDTO.setGap(GAP_POLICY);
				} else {
					gapPolicyDTO.setGap(OUTSIDE_GAP_POLICY);
				}

				log.info("MSISDN: {}, Response: {}, Gap Matrix Return: {}, Previous Date: {}, End Gap Policy Date: {}, Current Date: {}, Gap Return: {}", gapPolicyDTO.getMsisdn(),
						gapDetails.getValue(RESPONSE).toString(), (int) gapDetails.getValue(GAP), prevDate, endGapPolicyDate, now, gapPolicyDTO.getGap());

				gappolicyList.add(gapPolicyDTO);
			}

		}

		return gappolicyList;
	}
}