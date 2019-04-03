package mas2019.group11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import genius.core.Bid;
import genius.core.Domain;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.Value;
import genius.core.uncertainty.OutcomeComparison;
import genius.core.uncertainty.UserModel;



public class group11_AS extends AcceptanceStrategy {
	
	

	private double bestGoal = 0.95;
	private double ASconst = 0.15;
	
	private group11_OM om = new group11_OM();

	int round;

	@Override
	public void init(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel,
			Map<String, Double> parameters) throws Exception {
		this.negotiationSession = negoSession;
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
		double discount = negotiationSession.getDiscountFactor();
		System.out.println(discount);
		
		
		
		

		if (myUserModel == null) {
			if (lastOpponentBid != null && lastOwnBid != null) {

				// since our BS already take in account a Tit4Tat model there is no reason to
				// make AS very complex
				// I think it is enough to accept if it over a certain treshold + if the utility
				// that we get is > than what we are going to offer from BS to offer
				/*if(round>155) {
				double e = lastOwnBid.getMyUndiscountedUtil();
				utilityList.add(e);
				
				}*/
				if (lastOpponentBid.getMyUndiscountedUtil() > bestGoal) {
					System.out.println(round +" we accepted because utility was high");
					return Actions.Accept;
				}
				if (round <= 175 ) {	
					return Actions.Reject;
				}
				if (lastOpponentBid.getMyUndiscountedUtil() >= bestGoal
						|| lastOpponentBid.getMyUndiscountedUtil() >= offeringStrategy.getNextBid().getMyUndiscountedUtil()) {
					System.out.println(round);
					System.out.println(round +"we accepted because utility was higher than our next bid");
					return Actions.Accept;
				}
				if (om.getBidEvaluation(estimateBid) > lastOpponentBid.getMyUndiscountedUtil()+ ASconst ) {
					return Actions.Reject;
				}

			
				
				//if(utilityList.contains(negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil()
					//	||negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil() > utilityList.)) {
				//	return Actions.Accept;
				//}
				

			} else
				return Actions.Reject;
		}
		if (myUserModel != null) {
			
			List<Bid> bidRanking = myUserModel.getBidRanking().getBidOrder();
			
			double minRankedValue = myUserModel.getBidRanking().getLowUtility();
			double maxRankedValue = myUserModel.getBidRanking().getHighUtility();
			//index size-1 (last)
			Bid bestRankedBid = myUserModel.getBidRanking().getMaximalBid();
			//index 0 (first)
			Bid worstRankedBid = myUserModel.getBidRanking().getMinimalBid();
			List<Issue> bestIssues = myUserModel.getBidRanking().getMaximalBid().getIssues();
			List<Issue> worstIssues = myUserModel.getBidRanking().getMinimalBid().getIssues();
			
			
			
			//bid1 < bid2 is coded by comparisonResult = -1.
			ArrayList<OutcomeComparison> bidCompare =  (ArrayList<OutcomeComparison>) myUserModel.getBidRanking().getPairwiseComparisons();
			
			
			
			int j =0;
			
				for(OutcomeComparison pair : bidCompare) {
					j++;
					Bid bid1 = pair.getBid1();
					Bid bid2 = pair.getBid2();
					int result = pair.getComparisonResult();
					int equalValues = bid1.countEqualValues(bid2);
					
					
					for(int i = 0; i< bestIssues.size()-1; i++) {
						
						Issue issue = bestIssues.get(i);
						Value value = myUserModel.getBidRanking().getMaximalBid().getValue(issue);
						int IssueID = issue.getNumber();
						
						String IssueName = issue.getName();
						System.out.println(IssueID + " " + IssueName+" value is "+ value.toString() + " max value is  "+maxRankedValue);
					
					}
					
					for(int k = 0; k< worstIssues.size()-1; k++) {
						Issue issue = worstIssues.get(k);
						Value value = myUserModel.getBidRanking().getMaximalBid().getValue(issue);
						int IssueID = issue.getNumber();
						
						
						String IssueName = issue.getName();
						System.out.println(IssueID + " " + IssueName+" value is "+ value.toString()+ " min is  "+minRankedValue);
						
					
					}
					if(j>2)return Actions.Break;
				}
		
		}
		return Actions.Break;
	}

	@Override
	public String getName() {

		return "Dolos_AS";
	}

}