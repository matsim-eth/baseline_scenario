package ch.ethz.matsim.baseline_scenario.mode_choice;

import ch.ethz.matsim.mode_choice.DefaultModeChoiceTrip;
import ch.ethz.matsim.mode_choice.ModeChoiceModel;
import ch.ethz.matsim.mode_choice.ModeChoiceTrip;
import ch.ethz.matsim.mode_choice.alternatives.ChainAlternatives;
import ch.ethz.matsim.mode_choice.mnl.ModeChoiceAlternative;
//import ch.ethz.matsim.projects.astra.mode_choice.modes.private_av.PravEmptyRideUtilityEstimator;
import ch.ethz.matsim.baseline_scenario.scoring.ASTRAScoringParameters;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.PtConstants;

import java.util.*;
import java.util.stream.Collectors;

/**
 * New mode choice model, based on the original MNL implementation in the
 * mode_choice package. It includes some comments how the interfaces / structure
 * of the package can be improved and implements a simple straight-forward
 * "total chain utility" maximization scheme without taking the detour of
 * computing likelihoods in between.
 */
public class ASTRAModeChoiceModel implements ModeChoiceModel {
	final private ChainAlternatives chainAlternatives;
	final private Network network;
	final private Random random;
	final private ASTRAScoringParameters parameters;
	final private MainModeIdentifier mainModeIdentifier;

	final private List<String> modes = new LinkedList<>();
	final private Map<String, ModeChoiceAlternative> alternatives = new HashMap<>();

	private List<String> chainModes = new LinkedList<>();
	private List<String> nonChainModes = new LinkedList<>();

	final private Network roadNetwork;

	public ASTRAModeChoiceModel(ChainAlternatives chainAlternatives, Network network, Random random,
                                ASTRAScoringParameters parameters, Network roadNetwork,
                                MainModeIdentifier mainModeIdentifier) {
		this.chainAlternatives = chainAlternatives;
		this.network = network;
		this.random = random;
		this.parameters = parameters;
		this.roadNetwork = roadNetwork;
		this.mainModeIdentifier = mainModeIdentifier;
	}

	@Override
	/**
	 * Return the based chain of modes for a given plan
	 */
	public List<String> chooseModes(Plan plan) {
		/*
		 * REMARK: Actually, it doesn't make sense to pass chainModes and nonChainModes
		 * from here. The ChainAlternatives should know that by itself (via constructor
		 * etc)
		 */

		/*
		 * REMARK: In the original MNL implementation here carAvailability and license
		 * is checked. This also doesn't make sense. This should come from the utility
		 * computation. If a person without a license "scores" a car trip, it should
		 * return -Inf as utility! (Here, in ASTRA, it is implmented like this now!)
		 */

		// Get all possible chains
		List<List<String>> chains = chainAlternatives.getTripChainAlternatives(plan, chainModes, nonChainModes);
		Collections.shuffle(chains); // In case multiple chains will produce the same utility, see further down

		// Remark: This NEEDS to be passed via constructor instead of being created
		// here!
		StageActivityTypes stageActivityTypes = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);

		// Now figure out all the trips regardless of mode:
		List<Trip> originalTrips = TripStructureUtils.getTrips(plan, stageActivityTypes);
		List<ModeChoiceTrip> trips = originalTrips.stream().map(trip -> {
			return new DefaultModeChoiceTrip(network.getLinks().get(trip.getOriginActivity().getLinkId()),
					network.getLinks().get(trip.getDestinationActivity().getLinkId()),
					trip.getOriginActivity().getEndTime(), plan.getPerson());
		}).collect(Collectors.toList());

		// Empty plan, do nothing
		if (trips.size() == 0) {
			return Collections.emptyList();
		}

		// Only one alternative
		if (chains.size() == 1) {
			return chains.get(0);
		}

		List<String> backupChain = originalTrips.stream()
				.map(t -> mainModeIdentifier.identifyMainMode(t.getTripElements())).collect(Collectors.toList());
		double backupChainScore = Double.NEGATIVE_INFINITY;

		// Now, go through all the trip chains and do something with them.
		// Here, we just compute the total utility of all chains and choose the one
		// with the highest utility!

		// Possible alternatives:
		// - Use a Naive-Bayes total probability (like in the original implementation)
		// - Use a "total utility" approach with added variance by a Gumbel distribution
		// (This would mean adding N iid standard Gumbel samples to the utility, with
		// N being the number of trips in the chain)
		// - Others?

		/*
		 * REMARK: We need to figure out whether it makes sense to let ChainAlternatives
		 * not only return the mode of each trip in a chain, but also the available
		 * alternatives. However, one could argue that the ChainAlternatives should just
		 * do the construction part (and no filtering), while the utility computation
		 * takes such constraints into account (e.g. if there is a -Inf utility for car
		 * because the agent does not have a license, this trip would have a zero
		 * probability contribution or a -Inf contribution for the total utility of a
		 * chain, which, in both cases, is perfectly valid and desired behaviour. On the
		 * other hand, the utility computation doesn't know about previous trips, so
		 * actually it somehow would need to be informed that car is NOT available,
		 * because it has not been picked up from home etc... In this case the
		 * ChainAlternatives must provide a choice and the alternatives. Maybe this
		 * could also be made flexibly. Clearly, for the naive Bayes approach it would
		 * be useful to know, for the approach here (maximization of the chain utility)
		 * it does not matter at all (with the currect construction-scheme that takes
		 * chain-based modes into account ...)
		 */

		// UPDATE: We now add a Gumbel sample to the utility

//		PravEmptyRideUtilityEstimator pravEmptyRideUtilityEstimator = new PravEmptyRideUtilityEstimator(parameters,
//				roadNetwork, pravTravelTime);

		// As said, only compute total utility for now, simple approach
		List<Double> utilities = chains.stream().map(chain -> {
			Iterator<ModeChoiceTrip> tripIterator = trips.iterator();
			Iterator<String> choiceIterator = chain.iterator();

			double chainUtility = 0.0;

			boolean carIncluded = false;
			boolean privateAVIncluded = false;
			
			// Use OR here to make sure there is an exception if they are for some reason
			// not the same size
			while (tripIterator.hasNext() || choiceIterator.hasNext()) {
				String tripMode = choiceIterator.next();
				ModeChoiceTrip trip = tripIterator.next();

				if (alternatives.get(tripMode).isFeasible(trip)) {
					chainUtility += alternatives.get(tripMode).estimateUtility(trip);
					// chainUtility += - Math.log(-Math.log(random.nextDouble())); // Gumbel sample

					carIncluded &= tripMode.equals("car");
					privateAVIncluded &= tripMode.equals("prav");

					/*
					 * if (Math.abs(chainUtility) > 700.0) { break; }
					 */
				} else {
					chainUtility = Double.NEGATIVE_INFINITY;
					break;
				}
			}

//			// if (Math.abs(chainUtility) <= 700.0) {
//			if (chainUtility != Double.NEGATIVE_INFINITY) {
//				double pravUtility = pravEmptyRideUtilityEstimator.estimate(originalTrips, chain);
//				chainUtility += pravUtility;
//			}
//			// }

			if (carIncluded && privateAVIncluded) {
				// chainUtility += parameters.carAndPravPenalty;
				chainUtility = Double.NEGATIVE_INFINITY;
			}

			return chainUtility;
		}).collect(Collectors.toList());

		Iterator<List<String>> chainIterator = chains.iterator();
		Iterator<Double> utilityIterator = utilities.iterator();

		// Remove the ones that exceed the range

		while (chainIterator.hasNext() && utilityIterator.hasNext()) {
			double utility = utilityIterator.next();
			List<String> chain = chainIterator.next();

			if (utility < -700.0 || utility > 700.0) {
				chainIterator.remove();
				utilityIterator.remove();
			}

			if (utility != Double.NEGATIVE_INFINITY && utility > backupChainScore) {
				// In any case, this should make sure we have a feasible trip, e.g. everything
				// done by walking
				backupChainScore = utility;
				backupChain = chain;
			}
		}

		if (chains.size() == 0) {
			return backupChain;
		}

		// UPDATE: We use probabilities now, not gumbel-max!

		List<Double> exponentials = utilities.stream().map(utility -> {
			return Math.exp(Math.min(700.0, Math.max(-700.0, utility)));
		}).collect(Collectors.toList());

		double exponentialsSum = exponentials.stream().mapToDouble(d -> d).sum();

		List<Double> probabilities = exponentials.stream().map(exp -> {
			return exp / exponentialsSum;
		}).collect(Collectors.toList());

		List<Double> cdf = new LinkedList<>();

		double cumulativeSum = 0.0;

		for (int i = 0; i < probabilities.size(); i++) {
			cumulativeSum += probabilities.get(i);
			cdf.add(cumulativeSum);
		}

		// Select by probability

		double selector = random.nextDouble();
		int index = 0;

		while (index < cdf.size()) {
			if (selector < cdf.get(index)) {
				return chains.get(index);
			}

			index++;
		}

		return chains.get(cdf.size() - 1);

		/*
		 * // Now find the chain with the maximum utility. // We already did some
		 * shuffling before, so if two chains have the same utility, // each of them
		 * will be chosen eventually.
		 * 
		 * int maximumIndex = -1; double maximumUtility = Double.NEGATIVE_INFINITY;
		 * 
		 * for (int i = 0; i < utilities.size(); i++) { if (utilities.get(i) >
		 * maximumUtility) { maximumIndex = i; maximumUtility = utilities.get(i); } }
		 * 
		 * if (maximumIndex == -1) { System.out.println("problem"); ; }
		 * 
		 * return chains.get(maximumIndex);
		 */
	}

	/**
	 * Add mode choice alternatives.
	 * 
	 * Could also be done via constructor... See comments up above on chain-based
	 * and non-chain-based modes. This distinction should not be made here, but in
	 * the ChainAlternatives class.
	 */
	public void addModeAlternative(String mode, ModeChoiceAlternative alternative) {
		if (modes.contains(mode)) {
			throw new IllegalArgumentException(String.format("Alternative '%s' already exists", mode));
		}

		alternatives.put(mode, alternative);
		modes.add(mode);

		if (alternative.isChainMode()) {
			chainModes.add(mode);
		} else {
			nonChainModes.add(mode);
		}
	}

	@Override
	public String chooseMode(ModeChoiceTrip trip) {
		/* REMARK: Remove this in the mode_choice package. Useless! */
		throw new IllegalStateException();
	}
}
