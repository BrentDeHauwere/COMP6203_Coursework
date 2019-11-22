package group1;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.EndNegotiation;
import genius.core.actions.Offer;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.ExperimentalUserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;
import genius.core.utility.UncertainAdditiveUtilitySpace;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * A simple example agent that makes random bids above a minimum target utility.
 *
 * @author Tim Baarslag
 */
public class Agent1 extends AbstractNegotiationParty
{
	private static double MINIMUM_TARGET = 0.8;
	private Bid lastOffer;

	/**
	 * Initializes a new instance of the agent.
	 */
	@Override
	public void init(NegotiationInfo info)
	{
		super.init(info);
		// Needed for sout statements
		System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
		// checks if the user has preference uncertainty.
		if (hasPreferenceUncertainty()) {
			System.out.println("Preference uncertainty is enabled.");
			// Exercise 2.2: Using the Uncertainty Java classes:
			AgentID agentID = info.getAgentID();
			System.out.println("The Agent ID is"+agentID);

			// The total number of possible bids/outcomes in the domain.
			long totalBids = userModel.getDomain().getNumberOfPossibleBids();
			System.out.println("The total number of possible bids in the domain is" + totalBids);

			// Get the total number of bids in the preference ranking.
			BidRanking bidRanking = userModel.getBidRanking();
			System.out.println("The number of bids in the ranking is:" + bidRanking.getSize());

			// Get the elicitation cost.
			double elicitationCost = user.getElicitationCost();
			System.out.println("The elicitation costs are:"+elicitationCost);

			// Get the bid with the lowest utility.
			// two ways, first is to use BidRanking object
			System.out.println("The bid with the lowest utility is"+bidRanking.getLowUtility());
			// or getting all the ranked bids using the method getBidOrder()
			List bidList = bidRanking.getBidOrder();
			System.out.println("The bid with the lowest utility is"+bidList.get(1));

			// Bid with highest utility
			System.out.println("The highest bid is:"+bidRanking.getMaximalBid());

			// 5th bid in ranking list
			System.out.println("The 5th bid in the ranking is:"+bidList.get(5));

			ExperimentalUserModel e = ( ExperimentalUserModel ) userModel ;
			UncertainAdditiveUtilitySpace realUSpace = e.getRealUtilitySpace();
			System.out.println(realUSpace);
		}

		// An object which implements the UtilitySpace interface contains the domain and the agent's
		// preference profile, i.e. the issues, values, weights and evaluations. Note that an agent
		// knows only his own weights and evaluations, and not that of the opponent.

		AbstractUtilitySpace utilitySpace = info.getUtilitySpace();  // get's util space
		AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;  // converts to additive util

		List<Issue> issues = additiveUtilitySpace.getDomain().getIssues();  // gets list of issues

		for (Issue issue : issues) {
			int issueNumber = issue.getNumber();
			System.out.println(">> " + issue.getName() + " weight: " + additiveUtilitySpace.getWeight(issueNumber));

			// Assuming that issues are discrete only
			IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
			EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);

			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
				System.out.println(valueDiscrete.getValue());
				System.out.println("Evaluation(getValue): " + evaluatorDiscrete.getValue(valueDiscrete));
				try {
					System.out.println("Evaluation(getEvaluation): " + evaluatorDiscrete.getEvaluation(valueDiscrete));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Makes a random offer above the minimum utility target
	 * Accepts everything above the reservation value at the very end of the negotiation; or breaks off otherwise.
	 */
	@Override
	public Action chooseAction(List<Class<? extends Action>> possibleActions)
	{
		// Check for acceptance if we have received an offer
		if (lastOffer != null)
			if (timeline.getTime() >= 0.99)
				if (getUtility(lastOffer) >= utilitySpace.getReservationValue())
					return new Accept(getPartyId(), lastOffer);
				else
					return new EndNegotiation(getPartyId());

		// Otherwise, send out a random offer above the target utility
		return new Offer(getPartyId(), generateRandomBidAboveTarget());
	}

	private Bid generateRandomBidAboveTarget()
	{
		Bid randomBid;
		double util;
		int i = 0;
		// try 100 times to find a bid under the target utility
		do
		{
			randomBid = generateRandomBid();
			util = utilitySpace.getUtility(randomBid);
		}
		while (util < MINIMUM_TARGET && i++ < 100);
		return randomBid;
	}

	/**
	 * Remembers the offers received by the opponent.
	 */
	@Override
	public void receiveMessage(AgentID sender, Action action)
	{
		if (action instanceof Offer)
		{
			lastOffer = ((Offer) action).getBid();
		}
	}

	@Override
	public String getDescription()
	{
		return "Test Agent";
	}

	/**
	 * This stub can be expanded to deal with preference uncertainty in a more sophisticated way than the default behavior.
	 * Returns an estimate of the utility space given uncertain preferences specified by the user model.
	 * By default, the utility space is estimated with a simple counting heuristic so that any agent can
	 * deal with preference uncertainty.
	 *
	 * This method can be overridden by the agent to provide better estimates.
	 */
	@Override
	public AbstractUtilitySpace estimateUtilitySpace()
	{
		return super.estimateUtilitySpace();
	}



}
