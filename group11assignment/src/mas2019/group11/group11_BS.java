package mas2019.group11;


import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.timeline.DiscreteTimeline;
import genius.core.timeline.TimeLineInfo;
import genius.core.timeline.Timeline;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import negotiator.boaframework.offeringstrategy.anac2011.hardheaded.BidSelector;
import negotiator.boaframework.opponentmodel.DefaultModel;
import negotiator.boaframework.sharedagentstate.anac2011.HardHeadedSAS;

public class group11_BS extends OfferingStrategy{

	
		
		private double maxUtil = 1;
		private LinkedList<Entry<Double, Bid>> offerQueue;
		private BidSelector BSelector;
		private double concConst = 0.005;
		private double oppConst = 0.0;
		double concessionThreshold = 1.0;
		double oppConcThreshold = 0.5;
		Bid LastOpponentBid = null;
		double maxValue = 0.95;
		int turn = 0;
		private double oppmax = 0.0;
		double minUtil = 0.75;
		Bid opponentbestbid = null;
		double opbestvalue = 0.0;
		int secondPlayer = 1;
		TimeLineInfo timeline = null;
		int totalRounds = 0;
		int timePoint1 = 0;
		int timePoint2 = 0;
		int timePoint3 = 0;
		AbstractUtilitySpace utilitySpace = null;
		
		public group11_BS() {
		}
		
		/* The init and initializeAgent functions are adapted from HardHeaded. They are slightly adapted to accommodate preference uncertainty
		 * and initialise the timespan.
		 */
		@Override
		public void init(NegotiationSession negotiationSession, OpponentModel model, OMStrategy oms,
				Map<String, Double> parameters) throws Exception {
			if (model instanceof DefaultModel) {
				model = new group11_OM();
				model.init(negotiationSession, null);
				oms.setOpponentModel(model);
			}
			initializeAgent(negotiationSession, model, oms);
		}
		
		public void initializeAgent(NegotiationSession negoSession, OpponentModel model, OMStrategy oms) {
			this.negotiationSession = negoSession;
			
			// If we have preference uncertainty, a utility space is created by the PU helper function.
			utilitySpace = group11_PU.makeUtilitySpace(negotiationSession.getDomain(),negotiationSession.getUtilitySpace(),negotiationSession.getUserModel());
			BSelector = new BidSelector((AdditiveUtilitySpace) utilitySpace);
			
			//System.out.println("Highest bid in BidSelector is" + BSelector.getBidList().lastEntry().getKey());
			offerQueue = new LinkedList<Entry<Double, Bid>>();
			this.opponentModel = model;
			this.omStrategy = oms;
			//Initialise the time.
			timeline = negotiationSession.getTimeline();
			totalRounds=(int)timeline.getTotalTime();
			timePoint1 = (int)(0.8611*(double)totalRounds);
			timePoint2 = (int)(0.9611*(double)totalRounds);
			timePoint3 = (int)(0.9722*(double)totalRounds);
			//System.out.println("The time points are "+timePoint1+timePoint2+timePoint3);
			helper = new HardHeadedSAS(negoSession);
			Entry<Double, Bid> highestBid = BSelector.getBidList().lastEntry();
			try {
				maxUtil = utilitySpace.getUtility(highestBid.getValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
		/* InitializeBid is a function that creates bids in the first few turns. It can do so in two ways:
		 * when an opponent model is present, we choose a bid that has minimal utility for the opponent, and at
		 * least 0.75 utility for us. If no opponent model is present, we choose a random bid that has in between
		 * 0.75 and 0.8 utility for us.
		 */
		public BidDetails initializeBid(BidSelector BSelector, OpponentModel model) {
			TreeMap<Double, Bid> BidList2 = new TreeMap<Double, Bid>();
			BidDetails ReturnBidDetails = null;
			
			if (negotiationSession.getOpponentBidHistory().getHistory().isEmpty())
			{
				/* Create a second bidlist (BidList2) which contains all bids that have a value between 0.75 and 0.8
				 * for us.
				 */
				for (Entry<Double, Bid> entry : BSelector.getBidList().entrySet()) {
				    if (entry.getKey().doubleValue() >= 0.75 && entry.getKey().doubleValue() <= 0.8) {
				    	//System.out.println("Selected a bid with value "+ entry.getKey());
				        BidList2.put(entry.getKey(), entry.getValue());
				    }
				}
				/* If BidList2 is still empty, there probably were no entries with utility between 0.75 and 0.8.
				 * Instead, we just take all bids that are between 0.75 and 0.8 towards the maximum utility.
				 */
				if (BidList2.isEmpty()) {
					int size = BSelector.getBidList().size();
					int minimum = (int)(0.75*(double)size);
					int maximum = (int)(0.8*(double)size);
					double from = (double)BSelector.getBidList().keySet().toArray()[minimum];
					double to = (double)BSelector.getBidList().keySet().toArray()[maximum];
					SortedMap<Double,Bid> submap = BSelector.getBidList().subMap(from,to);
					for (double key: submap.keySet()) {
						BidList2.put(key, submap.get(key));
					}
				}
				/* If BidList2 is still empty, there are probably hardly any bids in the domain. Instead, we just
				 * take the two highest bids and put them in BidList2.
				 */
				if (BidList2.isEmpty()) {
					BidList2.put(BSelector.getBidList().lastEntry().getKey(),BSelector.getBidList().lastEntry().getValue());
					double key = BSelector.getBidList().lowerKey(1.0);
					BidList2.put(key,BSelector.getBidList().get(key));
				}
			    Object[] Keys = BidList2.keySet().toArray();
				Object key = Keys[new Random().nextInt(Keys.length)];
				ReturnBidDetails = new BidDetails(BidList2.get(key),(double)key);
			}
			else
			//Get the worst bid for the opponent with a utility higher than 0.75 for us.
			{
				//System.out.println("At least we got here.");
				for (Entry<Double, Bid> entry : BSelector.getBidList().entrySet()) {
			        if (entry.getKey().doubleValue() >= 0.75) {
			            BidList2.put(entry.getKey(), entry.getValue());
			        }
				}
				/* BidList2 should not be empty, because there should always be at least one bid
				 * with value 1 for us. But just in case, we do the same as before.
				 */
				if (BidList2.isEmpty()) {
					int size = BSelector.getBidList().size();
					int minimum = (int)(0.75*(double)size);
					int maximum = (int)(1*(double)size);
					double from = (double)BSelector.getBidList().keySet().toArray()[minimum];
					double to = (double)BSelector.getBidList().keySet().toArray()[maximum-1];
					SortedMap<Double,Bid> submap = BSelector.getBidList().subMap(from,to);
					for (double key: submap.keySet()) {
						BidList2.put(key, submap.get(key));
					}
				}
				double minopp = 1.1;
			    for (Entry<Double, Bid> entry2 : BidList2.entrySet())
			    {
			    	if (model.getBidEvaluation(entry2.getValue())<minopp) {
			    		ReturnBidDetails = new BidDetails(entry2.getValue(),entry2.getKey());
			    		minopp = model.getBidEvaluation(entry2.getValue());
			    		//System.out.println("Selected a bid with value "+ entry2.getKey() + "for us and value "+ minopp + "for the opponent.");
			    	}
			    }
			}
			if(ReturnBidDetails == null) {
				//System.out.println("empty bid");
			}
			//System.out.println("The last bid has value " + ReturnBidDetails.getMyUndiscountedUtil() + "for us and value "+ model.getBidEvaluation(ReturnBidDetails.getBid()) + "for the opponent.");
			return ReturnBidDetails;
			
		}
		
		/* The function makeConcession determines the bids after the first few turns. There are two options: either we are still in the phase where we make 
		 * pseudo-concessions, or we are making true concessions. If we are in the pseudo-concession phase, we take a bid slightly above the current
		 * concession threshold that is within a prespecified range for the opponent. If we are in the true concession phase, we take the bid above the
		 * current concession threshold that is estimated to be best for the opponent.
		 */
		public BidDetails makeConcession(double concessionThreshold, double oppConcThreshold, Bid lastBid, double lastBidUtil, OpponentModel model, BidSelector BSelector, int pseudoPhase, double concConst, double oppConstant) {
			double maxopp = 0.0;
			BidDetails ReturnBidDetails = null;
			TreeMap<Double, Bid> BidList2 = new TreeMap<Double, Bid>();
			
			//double util= bidList.getLastBidDetails();
			if (pseudoPhase == 1) {
				
				for (Entry<Double, Bid> entry : BSelector.getBidList().entrySet()) {
			        if (entry.getKey().doubleValue() >= concessionThreshold && entry.getKey().doubleValue() <= concessionThreshold+concConst) {
			            BidList2.put(entry.getKey(), entry.getValue());
			        }
			    }
				
				for (Entry<Double, Bid> entry2: BidList2.entrySet()) {
					if (model.getBidEvaluation(entry2.getValue())>= maxopp && 
							model.getBidEvaluation(entry2.getValue())<oppConcThreshold+oppConstant) {
						ReturnBidDetails = new BidDetails(entry2.getValue(),entry2.getKey());
						maxopp = model.getBidEvaluation(entry2.getValue());
						//System.out.println("added a bid with estimated opp util "+maxopp+" while the oppThreshold is "+oppConcThreshold);
					}
				}
				
			} else {
				for (Entry<Double, Bid> entry : BSelector.getBidList().entrySet()) {
			        if (entry.getKey().doubleValue() >= concessionThreshold) {
			            BidList2.put(entry.getKey(), entry.getValue());
			        }
			    }
				
				for (Entry<Double, Bid> entry2: BidList2.entrySet()){
					if (model.getBidEvaluation(entry2.getValue())>maxopp) {
						ReturnBidDetails = new BidDetails(entry2.getValue(),entry2.getKey());
						maxopp = model.getBidEvaluation(entry2.getValue());
					}
				}
			}
			// If some error occurred and the return bid has not been found, we just repeat the last bid.
			if (ReturnBidDetails == null) {
				return new BidDetails(lastBid,lastBidUtil);
			}
			return ReturnBidDetails;
		}
		
		/* This is the main function, which calls either initializeBid or makeConcession to make a bid. It goes through five stages:
		 * first, the initialisation stage, in which the bids are initialised; second, the pseudo-concession stage, where we gradually
		 * heighten our own utility; third, the tit for tat phase, in which we concede dependently on how much the opponent concedes;
		 * fourth, the scare tactic phase, in which we move away from the opponent to draw out hard-headed opponents, and finally the
		 * rapid concession phase, in which we make rapid concessions to maximise the chance of arriving at a compromise.
		 */
		public BidDetails SelectBid(NegotiationSession negoSession) {
			BidDetails ReturnBidDetails = null;
			Entry<Double,Bid> opponentbestentry;
			double opponentLowering;
			OpponentModel model = this.opponentModel;
			OMStrategy omstrategy = this.omStrategy;
			Bid LastBid = null;
			double lastBidUtil = 0.75;
			
			/* We initialise a bidlist with the highest values for ourselves for easy lookup later.
			 */
			TreeMap<Double, Bid> HighBidList = new TreeMap<Double, Bid>();
			for (Entry<Double, Bid> entry : BSelector.getBidList().entrySet()) {
		        if (entry.getKey().doubleValue() >= maxValue) {
		            HighBidList.put(entry.getKey(), entry.getValue());
		            //System.out.println("Something was put in HighBidList");
		        }
		    }
			
			// If HighBidList is still empty, we just take all bids within the specified range.
			if (HighBidList.isEmpty()) {
				int size = BSelector.getBidList().size();
				int minimum = (int)(maxValue*(double)size);
				int maximum = (int)(1.0*(double)size);
				double from = (double)BSelector.getBidList().keySet().toArray()[minimum];
				double to = (double)BSelector.getBidList().keySet().toArray()[maximum-1];
				SortedMap<Double,Bid> submap = BSelector.getBidList().subMap(from,to);
				for (double key: submap.keySet()) {
					HighBidList.put(key, submap.get(key));
				}
			}
			turn++;
			
			/* The following two if-loops were taken from HardHeaded and determine the best opponent bid so far.
			 */
			if (turn != 1) {
				LastBid = negoSession.getOwnBidHistory().getLastBid();
				lastBidUtil = utilitySpace.getUtility(negoSession.getOwnBidHistory().getLastBidDetails().getBid());
			}
			if (!negotiationSession.getOpponentBidHistory().getHistory().isEmpty()) {
				Bid opponentLastBid = negotiationSession.getOpponentBidHistory().getLastBidDetails().getBid();
				try {
					if (opponentbestbid == null)
						opponentbestbid = opponentLastBid;
					else if (utilitySpace.getUtility(opponentLastBid) > utilitySpace.getUtility(opponentbestbid)) {
						opponentbestbid = opponentLastBid;
					}

					opbestvalue = BSelector.getBidList()
							.floorEntry(utilitySpace.getUtility(opponentbestbid)).getKey();

					while (!BSelector.getBidList().floorEntry(opbestvalue).getValue().equals(opponentbestbid)) {
						opbestvalue = BSelector.getBidList().lowerEntry(opbestvalue).getKey();
					}
					opponentbestentry = BSelector.getBidList().floorEntry(opbestvalue);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			
			// First phase: initialisation phase.
			if (turn+secondPlayer < 4) {
				ReturnBidDetails = initializeBid(BSelector,model);
				concessionThreshold = utilitySpace.getUtility(ReturnBidDetails.getBid());
				//System.out.println("OppConcThreshold set to " + oppConcThreshold);
				
			// Second phase: pseudo-concession phase.
			} else if (turn+secondPlayer < timePoint1) {
				double maxopp = 0.0;
				BidDetails MaxBidDetails = null;
				for (Entry<Double, Bid> entry: HighBidList.entrySet()){
					//System.out.println("All fine so far: high bid list still exists. Turn is "+turn);
					if (model.getBidEvaluation(entry.getValue())>maxopp) {
						MaxBidDetails = new BidDetails(entry.getValue(),entry.getKey());
						//System.out.println("We are here. But is there a maxbiddetails?");
						maxopp = model.getBidEvaluation(entry.getValue());
					}
					
				}
				oppmax = maxopp;
				if (MaxBidDetails == null) {
						Object[] Keys = HighBidList.keySet().toArray();
						Object key = Keys[new Random().nextInt(Keys.length)];
						MaxBidDetails = new BidDetails(HighBidList.get(key),(double)key);
						oppmax = 0.3;
				}
				
				//System.out.println("Estimated oppmax is" + oppmax + ", estimated own max is" + MaxBidDetails.getMyUndiscountedUtil());
				//System.out.println("Estimated opp utility last turn is" + model.getBidEvaluation(LastBid));
				//System.out.println("The undiscounted util is "+MaxBidDetails.getMyUndiscountedUtil());
				//System.out.println("The timePoint1 is "+timePoint1);
				concConst = (MaxBidDetails.getMyUndiscountedUtil() - concessionThreshold) / (timePoint1-turn);
				concessionThreshold += concConst;
				if (turn < 15) {
					oppConcThreshold = model.getBidEvaluation(negotiationSession.getOwnBidHistory().getHistory().get(2-secondPlayer).getBid());
					oppConst = (oppmax-oppConcThreshold) / (timePoint1-3-secondPlayer);
					oppConcThreshold += (turn-secondPlayer - 3)*oppConst;
				} else {
					oppConst = (oppmax-oppConcThreshold) / (timePoint1-turn-secondPlayer);
					oppConcThreshold += oppConst;
				}
				//System.out.println("Now going into makeConcession, with concConst" + concConst + "and oppConst" + oppConst + "and concession threshold" + concessionThreshold + "and opp conc threshold "+oppConcThreshold);
				ReturnBidDetails = makeConcession(concessionThreshold,oppConcThreshold, LastBid, lastBidUtil, model, BSelector, 1, concConst, oppConst);
			
			// Third phase: tit for tat phase.
			} else if (turn+secondPlayer < timePoint2) {
				//System.out.println("Reached the third stage at time "+turn);
				List<BidDetails> oppBidHistory = negotiationSession.getOpponentBidHistory().getHistory();
				double oppUtilSecondLast = model.getBidEvaluation(oppBidHistory.get(oppBidHistory.size()-2).getBid());
				double oppUtilLast = model.getBidEvaluation(oppBidHistory.get(oppBidHistory.size()-1).getBid());
				opponentLowering = oppUtilSecondLast - oppUtilLast;
				//System.out.println("The opponent is believed to have lowered by"+opponentLowering);
				if (opponentLowering > 0.025) {
					concessionThreshold -= 0.025;
				} else if (opponentLowering < -0.025) {
					concessionThreshold += 0.025;
				} else {
					concessionThreshold -= opponentLowering;
				}
				ReturnBidDetails = makeConcession(concessionThreshold, 1.0, LastBid, lastBidUtil, model, BSelector, 0, 0.0, 0.0);
			
			// Fourth phase: scare tactics phase.
			} else if (turn+secondPlayer < timePoint3) {
				//System.out.println("Reached the fourth stage at time "+turn);
				concessionThreshold += (0.9-concessionThreshold) / (timePoint3+1-turn-secondPlayer);
				ReturnBidDetails = makeConcession(concessionThreshold, 1.0, LastBid, lastBidUtil, model, BSelector, 0, 0.0, 0.0);
			
			// Final phase: rapid concession phase.
			} else if (turn+secondPlayer < totalRounds-1){
				//System.out.println("Reached the fifth stage at time "+turn);
				concessionThreshold = minUtil + ((totalRounds-1-turn-secondPlayer)*(0.9-minUtil));
				if (opbestvalue > concessionThreshold) {
					ReturnBidDetails = new BidDetails(opponentbestbid,opbestvalue);
				} else {
					ReturnBidDetails = makeConcession(concessionThreshold, 1.0, LastBid, lastBidUtil, model, BSelector, 0, 0.0, 0.0);
				}
				
			/* In the last turn we first see if we can make a bid that does not further lower our utility and is likely acceptable for the opponent.
			 * If that is not the case, we see if the best opponent bid so far is good enough for us to return. If that is also not the case, we 
			 * gradually create lower bids, and offer one whenever it is likely to be acceptable for the opponent (making sure we do not concede
			 * too much).
			 */
			} else {
				//System.out.println("Last turn.");
				
				double oppUtilLast = model.getBidEvaluation(negotiationSession.getOpponentBidHistory().getHistory().get(negotiationSession.getOpponentBidHistory().size()-1).getBid());
				double ownUtilLast = negotiationSession.getOwnBidHistory().getLastBidDetails().getMyUndiscountedUtil();
				
				ReturnBidDetails = makeConcession(ownUtilLast,1.0, LastBid, lastBidUtil, model, BSelector, 0, 0.0, 0.0);
				
				if (model.getBidEvaluation(ReturnBidDetails.getBid())>1.1*oppUtilLast) {
					//System.out.println("First attempt was good enough for the opponent.");
					return ReturnBidDetails;
				} else {
					if (opbestvalue > 0.845) {
						ReturnBidDetails = new BidDetails(opponentbestbid,opbestvalue);
						return ReturnBidDetails;
					}
					BidDetails DiscountedBid = null;
					for (double d = 0.025;ownUtilLast-d>0.45;d+=0.025) {
						if (ownUtilLast-d < opbestvalue) {
							ReturnBidDetails = new BidDetails(opponentbestbid,opbestvalue);
							break;
						}
						//System.out.println("At this point, d has become "+d);
						DiscountedBid = makeConcession(ownUtilLast-d,1.0, LastBid, lastBidUtil, model, BSelector, 0, 0.0, 0.0);
						//System.out.println("Found a discounted bid with utility "+DiscountedBid.getMyUndiscountedUtil());
						if (model.getBidEvaluation(ReturnBidDetails.getBid())>oppUtilLast) {
							ReturnBidDetails = DiscountedBid;
							//System.out.println("This bid is thought to be good enough for the opponent.");
							break;
						}
						if (model.getBidEvaluation(DiscountedBid.getBid())>1.2*DiscountedBid.getMyUndiscountedUtil()) {
							//System.out.println("We have conceded as much as we reasonably can.");
							break;
						} else {
							ReturnBidDetails = DiscountedBid;
						}
					}
				}
			}
			
			if (ReturnBidDetails != null) {
				//System.out.println("The bid utility of turn" + turn + "is:" + ReturnBidDetails.getMyUndiscountedUtil());
				return ReturnBidDetails;
			} else {
				//System.out.println("The failed bid utility of turn" + turn + "is:" + negoSession.getOwnBidHistory().getLastBidDetails().getMyUndiscountedUtil());
				return negoSession.getOwnBidHistory().getLastBidDetails();
			}
		}

		@Override
		public BidDetails determineOpeningBid() {
			if (negotiationSession.getOpponentBidHistory().getHistory().isEmpty()) {
				secondPlayer =0;
			}
			return SelectBid(negotiationSession);
		}

		@Override
		public BidDetails determineNextBid() {
			BidDetails returnBid = SelectBid(negotiationSession);
			//System.out.println("The bid utility of the return bid in turn " + turn + " is " + utilitySpace.getUtility(returnBid.getBid()));
			return returnBid;
		}

		@Override
		public String getName() {
			return "Dolos";
		}
	
}