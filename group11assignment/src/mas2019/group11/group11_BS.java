package mas2019.group11;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.OfferingStrategy;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import genius.core.Bid;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.NoModel;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.utility.AdditiveUtilitySpace;
import java.util.Random;
import java.util.TreeMap;

import negotiator.boaframework.offeringstrategy.anac2011.hardheaded.BidSelector;
import negotiator.boaframework.opponentmodel.DefaultModel;
import negotiator.boaframework.opponentmodel.HardHeadedFrequencyModel;
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
		
		public group11_BS() {
		}
		
		@Override
		public void init(NegotiationSession negotiationSession, OpponentModel model, OMStrategy oms,
				Map<String, Double> parameters) throws Exception {
			if (model instanceof DefaultModel) {
				model = new HardHeadedFrequencyModel();
				model.init(negotiationSession, null);
				oms.setOpponentModel(model);
			}
			initializeAgent(negotiationSession, model, oms);
		}
		
		public void initializeAgent(NegotiationSession negoSession, OpponentModel model, OMStrategy oms) {
			this.negotiationSession = negoSession;
			BSelector = new BidSelector((AdditiveUtilitySpace) negotiationSession.getUtilitySpace());
			offerQueue = new LinkedList<Entry<Double, Bid>>();
			this.opponentModel = model;
			this.omStrategy = oms;
			helper = new HardHeadedSAS(negoSession);
			Entry<Double, Bid> highestBid = BSelector.getBidList().lastEntry();
			try {
				maxUtil = negotiationSession.getUtilitySpace().getUtility(highestBid.getValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			/*
			if (TEST_EQUIVALENCE) {
				random100 = new Random(100);
				random200 = new Random(200);
			} else {
				random100 = new Random();
				random200 = new Random();
			}
			*/
		}
		
		public BidDetails initializeBid(BidSelector BSelector, OpponentModel model) {
			TreeMap<Double, Bid> BidList2 = new TreeMap<Double, Bid>();
			BidDetails ReturnBidDetails = null;
			
			if (negotiationSession.getOpponentBidHistory().getHistory().isEmpty())
			{
				for (Entry<Double, Bid> entry : BSelector.getBidList().entrySet()) {
				    if (entry.getKey().doubleValue() >= 0.75 && entry.getKey().doubleValue() <= 0.8) {
				        BidList2.put(entry.getKey(), entry.getValue());
				    }
				}
			    Object[] Keys = BidList2.keySet().toArray();
				Object key = Keys[new Random().nextInt(Keys.length)];
				ReturnBidDetails = new BidDetails(BidList2.get(key),(double)key);
			}
			else
			{
				for (Entry<Double, Bid> entry : BSelector.getBidList().entrySet()) {
			        if (entry.getKey().doubleValue() >= 0.75) {
			            BidList2.put(entry.getKey(), entry.getValue());
			        }
				}
				double minopp = 1.0;
			    for (Entry<Double, Bid> entry2 : BidList2.entrySet())
			    {
			    	if (model.getBidEvaluation(entry2.getValue())<minopp) {
			    		ReturnBidDetails = new BidDetails(entry2.getValue(),entry2.getKey());
			    		minopp = model.getBidEvaluation(entry2.getValue());
			    		System.out.println("Selected a bid with value "+ entry2.getKey() + "for us and value "+ minopp + "for the opponent.");
			    	}
			    }
			}
			System.out.println("The last bid has value " + ReturnBidDetails.getMyUndiscountedUtil() + "for us and value "+ model.getBidEvaluation(ReturnBidDetails.getBid()) + "for the opponent.");
			return ReturnBidDetails;
			// Return bid details instead of individual bid.
		}
		
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
			if (ReturnBidDetails == null) {
				return new BidDetails(lastBid,lastBidUtil);
			}
			return ReturnBidDetails;
		}
		
		public BidDetails SelectBid(NegotiationSession negoSession) {
			BidDetails ReturnBidDetails = null;
			Entry<Double,Bid> opponentbestentry;
			double opponentLowering;
			OpponentModel model = this.opponentModel;
			OMStrategy omstrategy = this.omStrategy;
			Bid LastBid = null;
			double lastBidUtil = 0.75;
			TreeMap<Double, Bid> HighBidList = new TreeMap<Double, Bid>();
			for (Entry<Double, Bid> entry : BSelector.getBidList().entrySet()) {
		        if (entry.getKey().doubleValue() >= maxValue) {
		            HighBidList.put(entry.getKey(), entry.getValue());
		        }
		    }
			turn++;
			if (turn != 1) {
				LastBid = negoSession.getOwnBidHistory().getLastBid();
				lastBidUtil = negoSession.getOwnBidHistory().getLastBidDetails().getMyUndiscountedUtil();
			}
			if (!negotiationSession.getOpponentBidHistory().getHistory().isEmpty()) {
				Bid opponentLastBid = negotiationSession.getOpponentBidHistory().getLastBidDetails().getBid();
				try {
					if (opponentbestbid == null)
						opponentbestbid = opponentLastBid;
					else if (negotiationSession.getUtilitySpace().getUtility(opponentLastBid) > negotiationSession
							.getUtilitySpace().getUtility(opponentbestbid)) {
						opponentbestbid = opponentLastBid;
					}

					opbestvalue = BSelector.getBidList()
							.floorEntry(negotiationSession.getUtilitySpace().getUtility(opponentbestbid)).getKey();

					while (!BSelector.getBidList().floorEntry(opbestvalue).getValue().equals(opponentbestbid)) {
						opbestvalue = BSelector.getBidList().lowerEntry(opbestvalue).getKey();
					}
					opponentbestentry = BSelector.getBidList().floorEntry(opbestvalue);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			
			if (turn < 4) {
				ReturnBidDetails = initializeBid(BSelector,model);
				concessionThreshold = negotiationSession.getUtilitySpace().getUtility(ReturnBidDetails.getBid());
				oppConcThreshold = model.getBidEvaluation(ReturnBidDetails.getBid());
			} else if (turn < 155) {
				double maxopp = 0.0;
				BidDetails MaxBidDetails = null;
				for (Entry<Double, Bid> entry: HighBidList.entrySet()){
					if (model.getBidEvaluation(entry.getValue())>maxopp) {
						MaxBidDetails = new BidDetails(entry.getValue(),entry.getKey());
						maxopp = model.getBidEvaluation(entry.getValue());
					}
				}
				oppmax = maxopp;
				System.out.println("Estimated oppmax is" + oppmax + ", estimated own max is" + MaxBidDetails.getMyUndiscountedUtil());
				System.out.println("Estimated opp utility last turn is" + model.getBidEvaluation(LastBid));
				concConst = (MaxBidDetails.getMyUndiscountedUtil() - concessionThreshold) / (155-turn);
				concessionThreshold += concConst;
				oppConst = (oppmax-oppConcThreshold) / (155-turn);
				oppConcThreshold += oppConst;
				System.out.println("Now going into makeConcession, with concConst" + concConst + "and oppConst" + oppConst + "and concession threshold" + concessionThreshold);
				ReturnBidDetails = makeConcession(concessionThreshold,oppConcThreshold, LastBid, lastBidUtil, model, BSelector, 1, concConst, oppConst);
			} else if (turn < 175) {
				List<BidDetails> oppBidHistory = negotiationSession.getOpponentBidHistory().getHistory();
				double oppUtilSecondLast = model.getBidEvaluation(oppBidHistory.get(oppBidHistory.size()-2).getBid());
				double oppUtilLast = model.getBidEvaluation(oppBidHistory.get(oppBidHistory.size()-1).getBid());
				opponentLowering = oppUtilSecondLast - oppUtilLast;
				System.out.println("The opponent is believed to have lowered by"+opponentLowering);
				if (opponentLowering > 0.025) {
					concessionThreshold -= 0.025;
				} else if (opponentLowering < -0.025) {
					concessionThreshold += 0.025;
				} else {
					concessionThreshold -= opponentLowering;
				}
				ReturnBidDetails = makeConcession(concessionThreshold, 1.0, LastBid, lastBidUtil, model, BSelector, 0, 0.0, 0.0);
			} else if (turn < 178) {
				concessionThreshold += (0.9-concessionThreshold) / (178-turn);
				ReturnBidDetails = makeConcession(concessionThreshold, 1.0, LastBid, lastBidUtil, model, BSelector, 0, 0.0, 0.0);
			} else {
				concessionThreshold = minUtil + ((180-turn)*(0.9-minUtil));
				if (opbestvalue > concessionThreshold) {
					ReturnBidDetails = new BidDetails(opponentbestbid,opbestvalue);
				} else {
					ReturnBidDetails = makeConcession(concessionThreshold, 1.0, LastBid, lastBidUtil, model, BSelector, 0, 0.0, 0.0);
				}
			}
			
			if (ReturnBidDetails != null) {
				System.out.println("The bid utility of turn" + turn + "is:" + ReturnBidDetails.getMyUndiscountedUtil());
				return ReturnBidDetails;
			} else {
				System.out.println("The failed bid utility of turn" + turn + "is:" + negoSession.getOwnBidHistory().getLastBidDetails().getMyUndiscountedUtil());
				return negoSession.getOwnBidHistory().getLastBidDetails();
			}
		}

		@Override
		public BidDetails determineOpeningBid() {
			return SelectBid(negotiationSession);
		}

		@Override
		public BidDetails determineNextBid() {
			BidDetails returnBid = SelectBid(negotiationSession);
			System.out.println("The bid utility of the return bid is" + negotiationSession.getUtilitySpace().getUtility(returnBid.getBid()));
			return returnBid;
		}

		@Override
		public String getName() {
			return "Dolos";
		}
	
}