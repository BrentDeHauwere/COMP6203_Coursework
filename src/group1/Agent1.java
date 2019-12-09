package group1;

import java.util.*;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.Domain;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.SortedOutcomeSpace;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.utility.AbstractUtilitySpace;


public class Agent1 extends AbstractNegotiationParty
{
	/**
	 * § OPPONENT TRACKING VARIABLES
	 */
	private Bid lastReceivedBid = null;
	private double lastReceivedBidUtility;
	private OpponentModel opponentModel;

	/**
	 * § SETTINGS (CONSTANTS)
	 */
	// The percentage of the time during which the agent will only offer the maximum utility.
	private final double HARDHEADED_PERCENTAGE = 0.2;

	// The buffer size of the bestGeneratedBids list.
	// The maximum amount of bids the agent will save.
	private final int MAX_BEST_BIDS_BUFFER_SIZE = 100;

	// The number of bids to generate each round to populate the bestGeneratedBids array.
	private final int AMOUNT_OF_BIDS_TO_GENERATE = 100;

	// The frequency for recalculating the nash product with the best saved bids from the opponent.
	private final int FREQUENCY_RECALCULATE_NASH_PRODUCT = 10;

	private final double KA_MINIMUM_STARTING_TARGET_UTILITY = 0.85;

	/**
	 * § BIDDING VARIABLES
	 */
	// The amount of offers made by the agent.
	private int counterOffersMade;

	// The best bids generated by us; 'best' defined by the Nash product.
	private List<BidDetails> bestGeneratedBids;

	// The sorted outcome space, used to choose a bud close to a given utility.
	private SortedOutcomeSpace sortedOutcomeSpace;

	// Used to generate random numbers in the agent implementation.
	private Random random;

	/**
	 * § METHODS
	 */

	/**
	 * Initializes a new instance of the agent.
	 * + Prints the domain.
	 */
	@Override
	public void init(NegotiationInfo info)
	{
		super.init(info);

		// TODO: why needed? possible with uncertain domain? ## Yes it is as we override utility space
		this.sortedOutcomeSpace = new SortedOutcomeSpace(this.utilitySpace);
		this.random = new Random();
		this.counterOffersMade = 0;

		this.bestGeneratedBids = new ArrayList<BidDetails>();


		// TODO: We need to set up variables for when there is uncertainty
//		if (hasPreferenceUncertainty()){
////			System.out.println("Preference uncertainty is enabled.");
////			AbstractUtilitySpace passedUtilitySpace = info.getUtilitySpace();
////			AbstractUtilitySpace estimatedUtilitySpace = estimateUtilitySpace();
////			estimatedUtilitySpace.setReservationValue(passedUtilitySpace.getReservationValue());
////			estimatedUtilitySpace.setDiscount(passedUtilitySpace.getDiscountFactor());
////			info.setUtilSpace(estimatedUtilitySpace);
////			System.out.println("The elicitation costs are:"+user.getElicitationCost());
//			// AbstractUtilitySpace maxUtilityBid = estimatedUtilitySpace.getMaxUtilityBid(); Ask question in lab
//			// TODO: ?? Question on how to use preference elicitation to get util of specific bid
//		}

		// this.sortedOutcomeSpace = new SortedOutcomeSpace(this.utilitySpace); // changed to use estimation
		// Maybe look at taking sorted outcome space out and only sort using the bidranking we have and offer the best of those

//		BidDetails maxBidDetailsPossible = sortedOutcomeSpace.getMaxBidPossible(); // TODO: ? Do we use this ?
//		Bid maxBidPossible = maxBidDetailsPossible.getBid(); // TODO: Tester
//		BidDetails minBidPossible = sortedOutcomeSpace.getMaxBidPossible(); //TODO: Tester
//		BidDetails highBid = sortedOutcomeSpace.getBidNearUtility(1); // TODO: Or this??
//		Bid nearOneUtilbid = highBid.getBid(); // TODO: Tester

	}

	/**
	 * Indicates whether the agent is still within the time frame where he offers his maximum utility bid.
	 */
	private boolean isWithinMaxUtilityBidRange()
	{
		return getTimeLine().getTime() < this.HARDHEADED_PERCENTAGE;
	}

	/**
	 * Choose an action from the possibleActions list (ACCEPT / COUNTEROFFER / END)
	 */
	@Override
	public Action chooseAction(List<Class<? extends Action>> possibleActions)  // depends on lastReceivedBid which might be null
	{
		this.counterOffersMade++;

		// Hardheadedness: for first X% of the time, offer maximum utility bid.
		if (isWithinMaxUtilityBidRange()) {
			Bid maxUtilityBid = null; // TODO: replace by elicitation
			try {
				maxUtilityBid = this.utilitySpace.getMaxUtilityBid();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return new Offer(getPartyId(), maxUtilityBid);
		}

		// Otherwise, create bid above target utility
		// If the last received bid meets the criteria of or newly generated bid, ACCEPT the offered bid
		// If not, OFFER our newly generated bid
		Bid bid = generateBidAboveTarget();

		if (isLastReceivedBidPreferred(bid)) {
			return new Accept(getPartyId(), lastReceivedBid);
		} else {
			return new Offer(getPartyId(),bid);
		}
	}

	/**
	 * A bid will be accepted when:
	 * - Utility of the offered bid > utility of our generated bid
	 * - Utility of the offered bid > our target utility
	 */
	private boolean isLastReceivedBidPreferred(Bid generatedBid)
	{


		boolean condition1 = this.getUtility(this.lastReceivedBid) >= getUtility(generatedBid);  // LastRecievedbid might be null
		boolean condition2 = this.getUtility(this.lastReceivedBid) >= this.getTargetUtility();


		return condition1 || condition2;
	}

	/**
	 * Generate a bid, based on our target utility and the estimated opponent utility from the frequency model.
	 */
	private Bid generateBidAboveTarget()
	{
		double targetUtility = this.getTargetUtility();

		Bid randomBid;
		Bid bestBid = this.generateRandomBidAboveTarget(targetUtility);  // Check if not null

		double nashProduct;
		double bestNashProduct = Double.MIN_VALUE;

		// Recalculate the nash product every X rounds
		if (this.counterOffersMade % this.FREQUENCY_RECALCULATE_NASH_PRODUCT == 0)
			this.updateNashProduct();


		// TODO: mistake in original code: for (int i = 0; i < this.AMOUNT_OF_BIDS_TO_GENERATE; i++) {?
		// Generate random bids and keep the one with the best nash product.
		for (int i = 0; i < this.AMOUNT_OF_BIDS_TO_GENERATE; i++) {
			randomBid = this.generateRandomBidAboveTarget(targetUtility);
			nashProduct = this.calculateNashProduct(randomBid);

			if (nashProduct > bestNashProduct) {
				bestBid = randomBid;
				bestNashProduct = nashProduct;
			}
		}

		// If bestGeneratedBids not full, add above generated bid to list
		if (this.bestGeneratedBids.size() < MAX_BEST_BIDS_BUFFER_SIZE) {
			this.bestGeneratedBids.add(new BidDetails(bestBid, bestNashProduct));

			// If adding this new element to the list caused the list to be full,
			// sort the list by utility value
			// Note: as a consequence, only performed once
			if (this.bestGeneratedBids.size() < MAX_BEST_BIDS_BUFFER_SIZE)
				this.sortBestGeneratedBidsByUtility();

		}
		// If bestGeneratedBids full, keep the bid with the lowest utility in the list,
		// or the newly generated bid with the best nash product
		else {
			double utilityWorstBid = this.bestGeneratedBids.get(this.MAX_BEST_BIDS_BUFFER_SIZE - 1).getMyUndiscountedUtil();

			// TODO: comparing nash product with a utility? do we not save the we save the nash products?
			if (bestNashProduct > utilityWorstBid) {
				this.bestGeneratedBids.remove(0);
				this.bestGeneratedBids.add(new BidDetails(bestBid, bestNashProduct));
				this.sortBestGeneratedBidsByUtility();
			}
		}

		// If list is full, offer one of the top five bids
		// Otherwise, offer the newly generated bid with the highest nash product
		if (this.bestGeneratedBids.size() == this.MAX_BEST_BIDS_BUFFER_SIZE) {
			// TODO: taking the last one in the list?
			int index = this.MAX_BEST_BIDS_BUFFER_SIZE - random.nextInt(5) - 1;
			bestBid = this.bestGeneratedBids.get(index).getBid();
		}

		return bestBid;
	}

	/**
	 * Sort bestGeneratedBids by utility values.
	 */
	private void sortBestGeneratedBidsByUtility()
	{
		Collections.sort(this.bestGeneratedBids, new Comparator<BidDetails>() {
			@Override
			public int compare(BidDetails o1, BidDetails o2) {
				if (o1.getMyUndiscountedUtil() < o2.getMyUndiscountedUtil())
					return -1;
				else if (o1.getMyUndiscountedUtil() == o2.getMyUndiscountedUtil())
					return 0;
				else
					return 1;
			}
		});
	}

	/**
	 * Create random bid with utility > reservation value.
	 */
	private Bid generateRandomBidAboveTarget(double targetUtility)
	{
		Bid bid;

		do {
			double random = this.random.nextDouble();
			double utilityRange = targetUtility + random * (1.0 - targetUtility);  // Enrico said change
			bid = sortedOutcomeSpace.getBidNearUtility(utilityRange).getBid(); // Change not efficient and may time out
		} while (this.getUtility(bid) <= targetUtility);
		// TODO: this.getUtility(bid) allowed? What does utilityRange formula mean?
		// TODO: elicit reservation value? Is that available?

		return bid;
	}

	/**
	 * Update Nash products in the bestGeneratedBids list.
	 */
	private void updateNashProduct()
	{
		for (BidDetails bidDetails: this.bestGeneratedBids){
			bidDetails.setMyUndiscountedUtil(this.calculateNashProduct(bidDetails.getBid()));
		}
	}

	/**
	 * Calculate the Nash product by using our utility of the bid * opponent utility.
	 */
	private double calculateNashProduct(Bid bid)
	{
		return this.getUtility(bid) * opponentModel.getOpponentUtility(bid);
	}

	private double getTargetUtility() { // getMinAcceptableUtility
		double timeRemaining = 1 - this.getTimeLine().getTime();

		return Math.log10(timeRemaining) / this.getConcedingFactor() + KA_MINIMUM_STARTING_TARGET_UTILITY;
	}

	/**
	 * Calculate the conceding factor.
	 * If an opponent is hard headed, determined by the last 10 rounds, concede slightly faster.
	 * TODO: Giving in to opponent?
	 */
	private double getConcedingFactor()
	{
		double hardheadedness = OpponentModel.hardheaded(10);

		if (this.timeline.getTime() < 0.9)
			return 13;
		else if (hardheadedness <= 0.6)
			return 10;
		else
			return 7;
	}

	/**
	 * Remembers the offers received by the opponent.
	 */
	@Override
	public void receiveMessage(AgentID sender, Action action)
	{
		super.receiveMessage(sender, action);

		// If opponent makes an offer
		if (action instanceof Offer)
		{
			// Replace the last received bid
			this.lastReceivedBid = ((Offer) action).getBid();
			this.lastReceivedBidUtility = getUtility(lastReceivedBid);

			// If it is the first offer from the opponent, initialise model
			if (this.opponentModel == null)
				opponentModel = new OpponentModel(this.generateRandomBid()); // TODO: why generateRandomBid?

			// Store the bid and utility in the opponent history
			opponentModel.addBid(this.lastReceivedBid);
			opponentModel.addUtilityHistory(this.lastReceivedBidUtility); // TODO: in original code, why not done if it is a new opponent?
		}
	}

	/**
	 * The description of the agent.
	 */
	@Override
	public String getDescription()
	{
		return "Group 1 (2019) — COMP6203 Intelligent Agents";
	}

	/**
	 * This stub can be expanded to deal with preference uncertainty in a more sophisticated way than the default behavior.
	 */
	@Override
	public AbstractUtilitySpace estimateUtilitySpace()
	{
		Domain domain = getDomain();
		BidRanking bidRanking = userModel.getBidRanking(); // changed to userModel.getBidRanking
		LinearProgrammingEstimation linearProgrammingEstimation = new LinearProgrammingEstimation(domain, bidRanking);
		AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory;

		try {
			additiveUtilitySpaceFactory = linearProgrammingEstimation.Estimation();
		} catch (Exception e){
			e.printStackTrace();
			additiveUtilitySpaceFactory = new AdditiveUtilitySpaceFactory(domain);
			additiveUtilitySpaceFactory.estimateUsingBidRanks(bidRanking);
		}
		return additiveUtilitySpaceFactory.getUtilitySpace();
	}


	/**
	 * § SUPPLEMENTARY DEBUGGING CODE
	 */

	/**
	 * Initializes a new instance of the agent.
	 * + Prints the domain.
	 */
	/*
	@Override
	public void init(NegotiationInfo info)
	{
		super.init(info);

		// TO DO: why needed? possible with uncertain domain?
		this.sortedOutcomeSpace = new SortedOutcomeSpace(this.utilitySpace);
		this.random = new Random();

		// Print the domain
		// TO DO: replace with uncertainUtilitySpace
		if (false) {
			// UtilitySpace contains DOMAIN and PREFERENCE PROFILE
			// We use implementation called AdditiveUtilitySpace
			AbstractUtilitySpace utilitySpace = info.getUtilitySpace();
			AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;

			// All ISSUES in the domain
			List<Issue> issues = additiveUtilitySpace.getDomain().getIssues();
			for (Issue issue: issues) {
				// WEIGHTS of each issue
				int issueNumber = issue.getNumber();
				String issueName = issue.getName();
				double issueWeight = additiveUtilitySpace.getWeight(issueNumber);

				// Assuming DISCRETE issues, get EVALUATION FUNCTION of each issue
				IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
				EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);

				// For every possible VALUE of the issue, get EVALUATION
				for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
					String value = valueDiscrete.getValue(); // NAME of the value (256GB)
					Integer valueByEvaluationFunction = evaluatorDiscrete.getValue(valueDiscrete); // value (6)
					try {
						Double evaluationOfIssueValue = evaluatorDiscrete.getEvaluation(valueDiscrete); // evaluation (0.6)
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	*/
}
