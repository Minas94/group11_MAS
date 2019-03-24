package mas2019.group11;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.BoaParty;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.parties.NegotiationInfo;
import negotiator.boaframework.acceptanceconditions.other.AC_Next;
import negotiator.boaframework.offeringstrategy.other.TimeDependent_Offering;
import negotiator.boaframework.omstrategy.BestBid;
import negotiator.boaframework.opponentmodel.HardHeadedFrequencyModel;

public class agentBuild extends BoaParty {

	/**
	 * 
	 */
	

	public agentBuild(AcceptanceStrategy ac, Map<String, Double> acParams, OfferingStrategy os,
			Map<String, Double> osParams, OpponentModel om, Map<String, Double> omParams, OMStrategy oms,
			Map<String, Double> omsParams) {
		super();
		
	}

	@Override
	public void init(NegotiationInfo info) {
		AcceptanceStrategy ac = new AC_Next();
		OfferingStrategy os = new TimeDependent_Offering();
		OpponentModel om = new HardHeadedFrequencyModel();
		OMStrategy oms = new BestBid();

		@SuppressWarnings("unchecked")
		HashMap<String, Double> noparams = (HashMap<String, Double>) Collections.EMPTY_MAP;
		HashMap<String, Double> osParams = new HashMap<String, Double>();

		osParams.put("e", 0.2);

		configure(ac, noparams, os, osParams, om, noparams, oms, noparams);
		super.init(info);
	}

	@Override
	public String getDescription() {
		 
		return this.getDescription();
	}

}
