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
		OpponentModel model = null;
		double concessionThreshold = 1.0;
		Bid LastOpponentBid = null;
		int pseudoPhase = 1;
		double maxValue = 0.95;
		int turn = 1;
		private double oppmax = 0.0;
		double minUtil = 0.68;
		
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
		
		public Bid initializeBid(BidSelector BSelector, OpponentModel model) {
			TreeMap<Double, Bid> BidList2 = new TreeMap<Double, Bid>();
			Bid ReturnBid = null;
			
			if (negotiationSession.getOpponentBidHistory().getHistory().isEmpty())
			{
				for (Entry<Double, Bid> entry : BSelector.getBidList().entrySet()) {
				    if (entry.getKey().doubleValue() >= 0.75 && entry.getKey().doubleValue() <= 0.8) {
				        BidList2.put(entry.getKey(), entry.getValue());
				    }
				}
			    Object[] Keys = BidList2.keySet().toArray();
				Object key = Keys[new Random().nextInt(Keys.length)];
				ReturnBid = BidList2.get(key);
			}
			else
			{
				for (Entry<Double, Bid> entry : BSelector.getBidList().entrySet()) {
			        if (entry.getKey().doubleValue() >= 0.7) {
			            BidList2.put(entry.getKey(), entry.getValue());
			        }
				}
				double minopp = 1.0;
			    for (Bid bid: BidList2.values())
			    {
			    	if (model.getBidEvaluation(bid)<minopp) {
			    		ReturnBid = bid;
			    		minopp = model.getBidEvaluation(bid);
			    	}
			    }
			}
			return ReturnBid;
			// Return bid details instead of individual bid.
		}
		
		public Bid makeConcession(double concessionThreshold, Bid lastBid, OpponentModel model, BidSelector BSelector, int pseudoPhase, double concConst, double oppConstant) {
			double maxopp = 0.0;
			Bid ReturnBid = null;
			TreeMap<Double, Bid> BidList2 = new TreeMap<Double, Bid>();
			
			//double util= bidList.getLastBidDetails();
			if (pseudoPhase == 1) {
				for (Entry<Double, Bid> entry : BSelector.getBidList().entrySet()) {
			        if (entry.getKey().doubleValue() >= concessionThreshold && entry.getKey().doubleValue() <= concessionThreshold+concConst) {
			            BidList2.put(entry.getKey(), entry.getValue());
			        }
			    }
				
				for (Bid bid: BidList2.values()) {
					if (model.getBidEvaluation(bid)>= model.getBidEvaluation(lastBid) && 
							model.getBidEvaluation(bid)<model.getBidEvaluation(lastBid)+oppConstant) {
						ReturnBid = bid;
						break;
					}
				}
				if (ReturnBid == null) {
					for (Bid bid: BidList2.values()) {
						if (model.getBidEvaluation(bid) > maxopp && model.getBidEvaluation(bid) < model.getBidEvaluation(lastBid)+oppConstant) {
							ReturnBid = bid;
							maxopp = model.getBidEvaluation(bid);
						}
					}
				}
				
			} else {
				for (Entry<Double, Bid> entry : BSelector.getBidList().entrySet()) {
			        if (entry.getKey().doubleValue() >= concessionThreshold) {
			            BidList2.put(entry.getKey(), entry.getValue());
			        }
			    }
				
				for (Bid bid: BidList2.values()){
					if (model.getBidEvaluation(bid)>maxopp) {
						ReturnBid = bid;
						maxopp = model.getBidEvaluation(bid);
					}
				}
			}
			
			return ReturnBid;
		}
		
		public Bid SelectBid(NegotiationSession negoSession) {
			BSelector = new BidSelector((AdditiveUtilitySpace) negotiationSession.getUtilitySpace());
			Bid LastBid = negoSession.getOwnBidHistory().getLastBid();
			Bid bid = null;
			Bid opponentbestbid = null;
			Entry<Double,Bid> opponentbestentry;
			double opbestvalue = 0.0;
			double opponentLowering;
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
			
			if (turn == 1) {
				bid = initializeBid(BSelector,model);
				concessionThreshold = negotiationSession.getUtilitySpace().getUtility(bid);
			} else if (turn < 150) {
				oppmax = model.getBidEvaluation(BSelector.getBidList().floorEntry(maxValue).getValue());
				concConst = (maxValue - concessionThreshold) / (150-turn);
				concessionThreshold += concConst;
				oppConst = (oppmax-model.getBidEvaluation(LastBid)) / (150-turn);
				bid = makeConcession(concessionThreshold, LastBid, model, BSelector, 1, concConst, oppConst);
			} else if (turn < 175) {
				List<BidDetails> oppBidHistory = negotiationSession.getOpponentBidHistory().getHistory();
				double oppUtilSecondLast = model.getBidEvaluation(oppBidHistory.get(oppBidHistory.size()-2).getBid());
				double oppUtilLast = model.getBidEvaluation(oppBidHistory.get(oppBidHistory.size()-1).getBid());
				opponentLowering = oppUtilSecondLast - oppUtilLast;
				concessionThreshold -= opponentLowering;
				bid = makeConcession(concessionThreshold, LastBid, model, BSelector, 0, 0.0, 0.0);
			} else if (turn < 178) {
				concessionThreshold += (0.9-concessionThreshold) / (178-turn);
				bid = makeConcession(concessionThreshold, LastBid, model, BSelector, 0, 0.0, 0.0);
			} else {
				if (opbestvalue > minUtil) {
					bid = opponentbestbid;
				} else {
					bid = makeConcession(minUtil, LastBid, model, BSelector, 0, 0.0, 0.0);
				}
			}
			
			turn++;
			return bid;
		}

		@Override
		public BidDetails determineOpeningBid() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public BidDetails determineNextBid() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return null;
		}
	
}
