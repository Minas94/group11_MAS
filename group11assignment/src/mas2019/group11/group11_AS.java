package mas2019.group11;

import java.util.List;
import java.util.Map;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.uncertainty.UserModel;

import mas2019.group11.group11_PU;


public class group11_AS extends AcceptanceStrategy {

	private double bestGoal = 0.95;
	private double ASconst = 0.15;
	
	private group11_OM om = new group11_OM();
	
	private AbstractUtilitySpace utilitySpace = null;

	int round;

	@Override
	public void init(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel,
			Map<String, Double> parameters) throws Exception {
		this.negotiationSession = negoSession;
		utilitySpace = group11_PU.makeUtilitySpace(negoSession.getDomain(), negoSession.getUtilitySpace(), negoSession.getUserModel());
		offeringStrategy = strat;
		
	}

	@Override
	public Actions determineAcceptability() {
		round++;
		
		//SortedSet<Double> utilityList = new SortedSet<Double>();
		
		Bid estimateBid = negotiationSession.getOpponentBidHistory().getLastBid();
		BidDetails lastOpponentBid = negotiationSession.getOpponentBidHistory().getLastBidDetails();
		BidDetails lastOwnBid = negotiationSession.getOwnBidHistory().getLastBidDetails();
		UserModel myUserModel = negotiationSession.getUserModel();
		if (myUserModel != null && myUserModel.getBidRanking().getSize()<16) {
			// Since our user model doesn't really work for such small sizes, we just are very careful and never accept.
			return Actions.Reject;
		} else {
			if (lastOpponentBid != null && lastOwnBid != null) {

				// since our BS already take in account a Tit4Tat model there is no reason to
				// make AS very complex
				// I think it is enough to accept if it over a certain treshold + if the utility
				// that we get is > than what we are going to offer from BS to offer
				/*if(round>155) {
				double e = lastOwnBid.getMyUndiscountedUtil();
				utilityList.add(e);
				utilityList.sort(c);
				}*/
				if (utilitySpace.getUtility(lastOpponentBid.getBid()) > bestGoal) {
					System.out.println(round +" we accepted because utility was high");
					return Actions.Accept;
				}
				if (round <= 175 ) {	
					return Actions.Reject;
				}
				if (utilitySpace.getUtility(lastOpponentBid.getBid()) >= bestGoal
						|| utilitySpace.getUtility(lastOpponentBid.getBid()) >= offeringStrategy.getNextBid().getMyUndiscountedUtil()) {
					System.out.println(round);
					System.out.println(round +"we accepted because utility was higher than our next bid");
					return Actions.Accept;
				}
			/*
			 * if (om.getBidEvaluation(estimateBid) >
			 * utilitySpace.getUtility(lastOpponentBid.getBid())+ ASconst ) { return
			 * Actions.Reject; }
			 */

			
				
				//if(utilityList.contains(negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil()
					//	||negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil() > utilityList.)) {
				//	return Actions.Accept;
				//}
				

			} else
				return Actions.Reject;
		}
		return Actions.Reject;
	}

	@Override
	public String getName() {

		return "Dolos_AS";
	}

}