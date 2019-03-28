package mas2019.group11;

import java.util.List;

import genius.core.Bid;
import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;
import genius.core.uncertainty.UserModel;

public class group11_AS extends AcceptanceStrategy {

	@Override
	public Actions determineAcceptability() {
		Bid receivedBid = negotiationSession.getOpponentBidHistory().getLastBid();
		Bid lastOwnBid = negotiationSession.getOwnBidHistory().getLastBid();
		if (receivedBid == null || lastOwnBid == null) {
			return Actions.Reject;
		}

		UserModel userModel = negotiationSession.getUserModel();
		if (userModel != null) {
			List<Bid> bidOrder = userModel.getBidRanking().getBidOrder();
			if (bidOrder.contains(receivedBid)) {
				double percentile = (bidOrder.size() - bidOrder.indexOf(receivedBid)) / (double) bidOrder.size();
				if (percentile < 0.1)
					return Actions.Accept;
			}
		} else {
			// we have a normal utilityspace
			double otherLastUtil = negotiationSession.getUtilitySpace().getUtility(receivedBid);
			double myLastUtil = negotiationSession.getUtilitySpace().getUtility(lastOwnBid);
			if (otherLastUtil >= 0.9 * myLastUtil) {
				return Actions.Accept;
			}
		}
		return Actions.Reject;
	}

	

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

}
