package mas2019.group11;

import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import genius.core.Domain;
import genius.core.boaframework.NegotiationSession;
import genius.core.Bid;
import genius.core.issue.Objective;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.UserModel;

import ilog.concert.*;
import ilog.cplex.*;

/* This class estimates a linear utility space given a limited set of ranked bids.
 * It does so by treating the problem as a linear optimisation problem, based on
 * the method outlined in Tsimpoukis et al. 2018, except that the constraints are
 * adapted to fit the specific user model given in the current problem.
 * The class works quite well for bid samples of size 20 and higher, but not very
 * well for small bid samples of e.g. size 10.
 */

public abstract class group11_PU {
	
	public static AbstractUtilitySpace estimateUtilitySpace(Domain domain, UserModel userModel)
	{
		IloCplexModeler icp = new IloCplexModeler();
		// Obtain the domain and list of issues.
		List<Issue> issues = domain.getIssues();
		int numberIssues = issues.size();
		// Create an array that stores all issues for easy lookup of issue numbers.
		Issue[] issueArray = new Issue[issues.size()];
		// Fill the issue array and calculate the total number of values.
		int index = 0;
		for (Issue issue: issues) {
			issueArray[index] = issue;
			index++;
		} 
		// Also create a list containing an array of discrete values for each issue for easy lookup later on.
		List<ValueDiscrete[]> valueArrayList = new ArrayList<ValueDiscrete[]>();
		// Fill the value array list.
		for (int i=0; i<numberIssues; i++) {
			Issue issue = issueArray[i];
			IssueDiscrete discIssue = (IssueDiscrete) issue;
			List<ValueDiscrete> discValues = discIssue.getValues();
			ValueDiscrete[] array = new ValueDiscrete[discValues.size()];
			index =0;
			for (ValueDiscrete value: discValues) {
				array[index] = value;
				index++;
			}
			valueArrayList.add(array);
		}
		for (int i=0; i<numberIssues; i++) {
			ValueDiscrete[] printArray = valueArrayList.get(i);
			//for (int j=0; j<printArray.length; j++) {
				//System.out.println("We have the value " + printArray[j].toString() + " in the value array list.");
			//}
		}
		BidRanking bidRanking = userModel.getBidRanking();
		int numberBids = bidRanking.getSize();
		try {
			/* Now we solve the linear optimisation problem. This goes in three steps. 
			 * Step one: initialise the unknown variables and the expression to be minimised.
			 */
			
			// Initialise the model.
			IloCplex model = new IloCplex();
			// Initialise the first set of entry arrays. We initialise one entry array for the phi values of each issue.
			List<IloNumVar[]> entryArrayList = new ArrayList<IloNumVar[]>();
			for (int i=0; i<numberIssues; i++) {
				Issue issue = issueArray[i];
				IssueDiscrete discIssue = (IssueDiscrete) issue;
				List<ValueDiscrete> discValues = discIssue.getValues();
				entryArrayList.add(new IloNumVar[discValues.size()]);
			}
			// Fill the first set of entry arrays.
			for (IloNumVar[] phiArray: entryArrayList) {
				for (int i =0; i<phiArray.length; i++) {
					phiArray[i]=model.numVar(0.0, Double.MAX_VALUE);
				}
			}
			
			// Initialise the second entry array, which contains all slack variables.
			IloNumVar[] z = new IloNumVar[numberBids-1];
			// Fill the second entry array.
			for (int i=0; i<numberBids-1; i++) {
				z[i] = model.numVar(0, Double.MAX_VALUE);
			}
			
			// Initialise the third entry array, which contains the max phi values for each issue.
			IloNumVar[] m = new IloNumVar[numberIssues];
			// Fill the third array.
			for (int i=0; i<numberIssues; i++) {
				m[i] = model.numVar(0, Double.MAX_VALUE);
			}
			
			// Initialise the expression to be minimised, i.e. the sum of all slack variables.
			IloLinearNumExpr expression = model.linearNumExpr();
			for (int i=0; i<numberBids-1; i++) {
				expression.addTerm(1, z[i]);
			}
			model.addMinimize(expression);
			
			//System.out.println("Step 1 check.");
			
			/* Step two: input the constraints. Note that the positivity constraints are already contained in the definition of the unknown variables, therefore
			 * we do not explicitly have to add the constraints that the slack variables and the phi-variables should be positive.
			 */
			
			// Initialise the first set of constraints, i.e. for each outcome pair the sum of the slack variable and the difference in utility has to be larger than 0.
			for (int i=0; i<numberBids-1; i++) {
				IloLinearNumExpr constraint = model.linearNumExpr();
				// Add the slack variable.
				constraint.addTerm(1, z[i]);
				Bid higherBid = bidRanking.getBidOrder().get(numberBids-1-i);
				Bid lowerBid = bidRanking.getBidOrder().get(numberBids-2-i);
				// Add the difference in utility, which is the sum of the difference in phi variables for each issue.
				for (int j=0; j<numberIssues; j++) {
					Issue issue = issueArray[j];
					Value higherValue = higherBid.getValue(issue);
					ValueDiscrete higherValueDiscrete = (ValueDiscrete) higherValue;
					Value lowerValue = lowerBid.getValue(issue);
					ValueDiscrete lowerValueDiscrete = (ValueDiscrete) lowerValue;
					int higherValueIndex = Arrays.asList(valueArrayList.get(j)).indexOf(higherValueDiscrete);
					int lowerValueIndex = Arrays.asList(valueArrayList.get(j)).indexOf(lowerValueDiscrete);
					constraint.addTerm(1, entryArrayList.get(j)[higherValueIndex]);
					constraint.addTerm(-1, entryArrayList.get(j)[lowerValueIndex]);
				}
				// The sum has to be greater than 0.
				model.addGe(constraint,0.0);
			}
			
			// Initialise the second constraint, i.e. that the phi variables of the values of the highest bid should sum to the total highest utility.
			IloLinearNumExpr constraint2 = model.linearNumExpr();
			Bid bestBid = bidRanking.getMaximalBid();
			for (int i=0; i<numberIssues; i++) {
				Issue issue = issueArray[i];
				Value highestValue = bestBid.getValue(issue);
				ValueDiscrete highestValueDiscrete = (ValueDiscrete) highestValue;
				int highestValueIndex = Arrays.asList(valueArrayList.get(i)).indexOf(highestValueDiscrete);
				constraint2.addTerm(1, entryArrayList.get(i)[highestValueIndex]);
			}
			// The sum has to be equal to the highest utility.
			model.addEq(constraint2,bidRanking.getHighUtility());
			
			// Initialise the third constraint, i.e. that the phi variables of the values of the lowest bid should sum to the total lowest utility.
			IloLinearNumExpr constraint3 = model.linearNumExpr();
			Bid worstBid = bidRanking.getMinimalBid();
			for (int i=0; i<numberIssues; i++) {
				Issue issue = issueArray[i];
				Value lowestValue = worstBid.getValue(issue);
				ValueDiscrete lowestValueDiscrete = (ValueDiscrete) lowestValue;
				int lowestValueIndex = Arrays.asList(valueArrayList.get(i)).indexOf(lowestValueDiscrete);
				constraint3.addTerm(1, entryArrayList.get(i)[lowestValueIndex]);
			}
			// The sum has to be equal to the lowest utility.
			model.addEq(constraint3,bidRanking.getLowUtility());
			
			// One final set of constraints: the sum of the highest phi variables for each issue has to be 1.
			
			// First, set constraints that the maximum variables should be equal to the maximum phi values of the given index.
			
			for (int i=0; i<numberIssues;i++) {
				IloLinearNumExpr constraint4 = model.linearNumExpr();
				constraint4.addTerm(1,m[i]);
				constraint4.addTerm(-1,(IloNumVar)icp.max(entryArrayList.get(i)));
				model.addEq(constraint4,0);
			} 
			
			// Then, set the constraint that the maximum variables should sum to 1.
			IloLinearNumExpr constraint5 = model.linearNumExpr();
			for (int i=0; i<numberIssues; i++) {
				constraint5.addTerm(1, m[i]);
			}
			model.addEq(constraint5, 1.0);
			
			//System.out.println("Step 2 check.");
			
			/* Step three: solve the linear optimisation problem. */
			boolean solved = model.solve();
			if (solved) {
				/* If the problem has been solved, we wish to create a utility space that the other classes can work with.
				 * This can simply be done by setting all weights to 1 and setting the evaluation of each value equal to
				 * its associated phi-value. (Since the phi-value equals the product of the weight and the evaluation, this
				 * will lead to the right utility estimates.
				 */
				//System.out.println("Problem solved.");
				AdditiveUtilitySpaceFactory utilSpace = new AdditiveUtilitySpaceFactory(domain);
				index = 0;
				for (Issue issue: issues) {
					
					IssueDiscrete discIssue = (IssueDiscrete) issue;
					List<ValueDiscrete> discValues = discIssue.getValues();
					double totalPhi = 0.0;
					double maxphi = 0.0;
					int nonZeroValues = 0;
					for (ValueDiscrete value: discValues) {
						int valueIndex = Arrays.asList(valueArrayList.get(index)).indexOf(value);
						double valueEvaluation = model.getValue(entryArrayList.get(index)[valueIndex]);
						
						totalPhi += valueEvaluation;
						if (valueEvaluation > maxphi) {
							maxphi = valueEvaluation;
						}
						if (valueEvaluation == 0) {
							nonZeroValues++;
						}
					}
					utilSpace.setWeight(issue, maxphi);
					
					
					/* Sometimes the optimisation problem results in 0 values for certain phi variables. In this case, we
					 * change the value to a more plausible value, namely the highest value divided by the number of values.
					 */
					 
					
					double averagePhi = totalPhi / discValues.size();
					//System.out.println("the average value of the issue ("+issue+") is: "+averagePhi);
					for (ValueDiscrete value: discValues) {
						int valueIndex = Arrays.asList(valueArrayList.get(index)).indexOf(value);
						//System.out.println("we have the value "+value+" with index "+valueIndex);
						double valueEvaluation = model.getValue(entryArrayList.get(index)[valueIndex]);
						//System.out.println("the evaluation of the value is "+valueEvaluation);
						if (valueEvaluation == 0.0) {
							//utilSpace.setUtility(issue, value, valueEvaluation);
							//System.out.println("we are finally in the value == 0");
							valueEvaluation = averagePhi;					
						}
						
						
						utilSpace.setUtility(issue, value,valueEvaluation);
						//System.out.println("We just set value "+value.getValue()+" to "+ 0.5*averagePhi);
						
						//System.out.println("Issue" + issue.convertToString() + "has a value" + value.getValue() + "with evaluation" + valueEvaluation);
					}
					
					
					index++;
				}
				
				//System.out.println("The max value is "+bidRanking.getHighUtility()+" and the calculated max value is "+ estimatedMax);
				//System.out.println("The estimated value of the max bid is "+utilSpace.getUtilitySpace().getUtility(bestBid));
				return utilSpace.getUtilitySpace();
			} else {
				//System.out.println("Model not solved");
				return null;
			}
			
		} catch (IloException ex) {
			//System.out.println("There's a problem. But where? Who knows?");
			ex.printStackTrace();
			return null;
		}
		
	}
	
	public static AbstractUtilitySpace makeUtilitySpace(Domain domain, AbstractUtilitySpace utilitySpace, UserModel userModel) {
		//System.out.println("we are summoned");
		if (userModel != null) {
			return estimateUtilitySpace(domain,userModel);
		} else {
			//System.out.println("It all goes wrong here");
			return utilitySpace;
		}
	}
}