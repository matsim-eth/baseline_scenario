package ch.ethz.matsim.baseline_scenario.mode_choice;

import ch.ethz.matsim.mode_choice.alternatives.ChainAlternatives;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ASTRAChainAlternatives implements ChainAlternatives {
	final private static Logger logger = Logger.getLogger(ASTRAChainAlternatives.class);

	final private StageActivityTypes stageActivityTypes;
	final private MainModeIdentifier mainModeIdentifier;
	final private Collection<String> borderModes;

	final private Id<Link> outsideLocation = Id.createLinkId("outside");

	public ASTRAChainAlternatives(StageActivityTypes stageActivityTypes, MainModeIdentifier mainModeIdentifier,
                                  Collection<String> borderModes) {
		this.stageActivityTypes = stageActivityTypes;
		this.mainModeIdentifier = mainModeIdentifier;
		this.borderModes = borderModes;
	}

	private Id<Link> getHomeLocation(Plan plan) {
		for (Activity activity : TripStructureUtils.getActivities(plan, stageActivityTypes)) {
			if (activity.getType().contains("home")) {
				return activity.getLinkId();
			}
		}

		return outsideLocation;
	}

	private boolean isOutside(Id<Link> linkId) {
		return linkId.toString().contains("outside");
	}

	private boolean isOutside(Activity activity) {
		return activity.getType().contains("outside") || isOutside(activity.getLinkId());
	}

	private Id<Link> normalizeLinkId(Activity activity) {
		if (activity.getLinkId().toString().contains("outside") || activity.getType().contains("outside")) {
			return outsideLocation;
		} else {
			return activity.getLinkId();
		}
	}

	private Id<Link> getVehicleLocation(List<Trip> trips, Id<Link> homeLocation, String mode, List<String> chain) {
		int index = chain.lastIndexOf(mode);

		if (index > -1) {
			return normalizeLinkId(trips.get(index).getDestinationActivity());
		} else {
			return homeLocation;
		}
	}

	@Override
	public List<List<String>> getTripChainAlternatives(Plan plan, List<String> chainModes, List<String> nonChainModes) {
		List<List<String>> alternatives = new LinkedList<>();
		List<List<String>> newAlternatives = new LinkedList<>();

		Id<Link> homeLocation = getHomeLocation(plan);

		alternatives.add(new LinkedList<>());
		List<Trip> trips = TripStructureUtils.getTrips(plan, stageActivityTypes);

		for (Trip trip : trips) {
			String mainMode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
			boolean outside = isOutside(trip.getOriginActivity()) || isOutside(trip.getDestinationActivity());

			if (mainMode.equals("outside")) {
				for (List<String> source : alternatives) {
					List<String> current = new LinkedList<>(source);
					current.add("outside");
					newAlternatives.add(current);
				}
			} else if (outside && !borderModes.contains(mainMode)) {
				for (List<String> source : alternatives) {
					List<String> current = new LinkedList<>(source);
					current.add(mainMode);
					newAlternatives.add(current);
				}
			} else {
				for (List<String> source : alternatives) {
					for (String nonChainMode : nonChainModes) {
						if (nonChainMode.equals("outside")) {
							continue;
						}

						if (outside && !borderModes.contains(nonChainMode)) {
							continue;
						}

						List<String> current = new LinkedList<>(source);
						current.add(nonChainMode);
						newAlternatives.add(current);
					}

					for (String chainMode : chainModes) {
						if (chainMode.equals("outside")) {
							continue;
						}

						if (outside && !borderModes.contains(chainMode)) {
							continue;
						}

						Id<Link> lastLocation = getVehicleLocation(trips, homeLocation, chainMode, source);

						if (normalizeLinkId(trip.getOriginActivity()).equals(lastLocation)) {
							List<String> current = new LinkedList<>(source);
							current.add(chainMode);
							newAlternatives.add(current);
						}
					}
				}
			}

			alternatives = new LinkedList<>(newAlternatives);
			newAlternatives.clear();
		}

		Iterator<List<String>> iterator = alternatives.iterator();

		while (iterator.hasNext()) {
			List<String> chain = iterator.next();

			for (String mode : chainModes) {
				if (mode.equals("outside")) {
					continue;
				}

				Id<Link> finalLocation = getVehicleLocation(trips, homeLocation, mode, chain);

				if (!finalLocation.equals(homeLocation)) {
					iterator.remove();
					break;
				}
			}
		}

		if (alternatives.size() == 0) {
			logger.warn("No feasible trip chain for person '" + plan.getPerson().getId()
					+ "'. Falling back to original plan.");
			List<String> original = new LinkedList<>();

			for (Trip trip : trips) {
				original.add(mainModeIdentifier.identifyMainMode(trip.getTripElements()));
			}

			alternatives.add(original);
		}

		return alternatives;
	}

}
