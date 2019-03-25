package mas2019.group11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import agents.BidComparator;
import agents.anac.y2010.AgentSmith.Bounds;
import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.NoModel;
import genius.core.boaframework.OfferingStrategy;
import genius.core.issue.Issue;
import genius.core.issue.Value;

public class ExampleClass_BS extends OfferingStrategy{
	
	//please use this class to get an idea on how to implement a bidding strategy. 
	//unfortunately not all source code is available through the jar file(not the agents source code) 
	//so basically i did a copy/pasta from the web
	//
	
	private final static double sTimeMargin = 170.0 / 180.0;
	private final static double sUtilyMargin = 0.7;
	static private double UTILITY_THRESHOLD = 0.7;
	private int fIndex;

	
	
	@Override
	public BidDetails determineOpeningBid() {
		
		return determineNextBid();
	}

	@Override
	public BidDetails determineNextBid() {
		 // Time in seconds.
        double time = negotiationSession.getTime();
        Bid bid2Offer = null;
        try {
                // Check if the session (2 min) is almost finished
                if (time >= sTimeMargin) {
                        // If the session is almost finished check if the utility is
                        // "high enough"
                        BidDetails lastBid = negotiationSession.getOpponentBidHistory().getLastBidDetails();
                        if (lastBid.getMyUndiscountedUtil() < sUtilyMargin) {
                                nextBid = negotiationSession.getOpponentBidHistory().getBestBidDetails();
                        }
                } else {
                        bid2Offer = getMostOptimalBid();
                        nextBid = new BidDetails(bid2Offer, negotiationSession.getUtilitySpace().getUtility(bid2Offer),
                                        negotiationSession.getTime());
                }
        } catch (Exception e) {

        }
        return nextBid;
	}
	
	
	
	  Bid getMostOptimalBid() {
          ArrayList<Bid> allBids = getSampledBidList();

          ArrayList<Bid> removeMe = new ArrayList<Bid>();
          for (int i = 0; i < allBids.size(); i++) {
                  try {
                          if (negotiationSession.getUtilitySpace().getUtility(allBids.get(i)) < UTILITY_THRESHOLD) {
                                  removeMe.add(allBids.get(i));
                          }
                  } catch (Exception e) {
                          e.printStackTrace();
                  }
          }
          allBids.removeAll(removeMe);

          if (opponentModel instanceof NoModel) {
                  Bid bid = allBids.get(fIndex);
                  fIndex++;
                  return bid;
          } else {
                  // Log.logger.info("Size of bid space: " + lBids.size());
                  Comparator<Bid> lComparator = new BidComparator(negotiationSession.getUtilitySpace());

                  // sort the bids in order of highest utility
                  ArrayList<Bid> sortedAllBids = allBids;
                  Collections.sort(sortedAllBids, lComparator);

                  Bid lBid = sortedAllBids.get(fIndex);
                  if (fIndex < sortedAllBids.size() - 1)
                          fIndex++;
                  return lBid;
          }
  }
	  
      private ArrayList<Bid> getSampledBidList() {
          ArrayList<Bid> lBids = new ArrayList<Bid>();
          List<Issue> lIssues = negotiationSession.getIssues();
          HashMap<Integer, Bounds> lBounds = Bounds.getIssueBounds(lIssues);

          // first createFrom a new list
          HashMap<Integer, Value> lBidValues = new HashMap<Integer, Value>();
          for (Issue lIssue : lIssues) {
                  Bounds b = lBounds.get(lIssue.getNumber());
                  Value v = Bounds.getIssueValue(lIssue, b.getLower());
                  lBidValues.put(lIssue.getNumber(), v);
          }
          try {
                  lBids.add(new Bid(negotiationSession.getUtilitySpace().getDomain(), lBidValues));
          } catch (Exception e) {
          }

          // for each item permutate with issue values, like binary
          // 0 0 0
          // 0 0 1
          // 0 1 0
          // 0 1 1
          // etc.
          for (Issue lIssue : lIssues) {
                  ArrayList<Bid> lTempBids = new ArrayList<Bid>();
                  Bounds b = lBounds.get(lIssue.getNumber());

                  for (Bid lTBid : lBids) {
                          for (double i = b.getLower(); i < b.getUpper(); i += b.getStepSize()) {
                                  HashMap<Integer, Value> lNewBidValues = getBidValues(lTBid);
                                  lNewBidValues.put(lIssue.getNumber(), Bounds.getIssueValue(lIssue, i));

                                  try {
                                          Bid iBid = new Bid(negotiationSession.getUtilitySpace().getDomain(), lNewBidValues);
                                          lTempBids.add(iBid);

                                  } catch (Exception e) {

                                  }
                          }
                  }
                  lBids = lTempBids;
          }

          ArrayList<Bid> lToDestroy = new ArrayList<Bid>();
          for (Bid lBid : lBids) {
                  try {
                          if (negotiationSession.getUtilitySpace().getUtility(lBid) < UTILITY_THRESHOLD) {
                                  lToDestroy.add(lBid);
                          }
                  } catch (Exception e) {
                          e.printStackTrace();
                  }
          }
          for (Bid lBid : lToDestroy) {
                  lBids.remove(lBid);
          }

          return lBids;
  }
      
      private HashMap<Integer, Value> getBidValues(Bid pBid) {
          HashMap<Integer, Value> lNewBidValues = new HashMap<Integer, Value>();
          for (Issue lIssue : negotiationSession.getUtilitySpace().getDomain().getIssues()) {
                  try {
                          lNewBidValues.put(lIssue.getNumber(), pBid.getValue(lIssue.getNumber()));
                  } catch (Exception e) {

                  }
          }
          return lNewBidValues;
  }
	  
	  

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

}
