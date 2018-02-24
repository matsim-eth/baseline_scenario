package ch.ethz.matsim.baseline_scenario.zurich.router.trip;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;

import ch.ethz.matsim.baseline_scenario.zurich.utils.ActivityWithFacility;

public class ModeAwareTripRouter implements TripRouter {
	final private MainModeIdentifier mainModeIdentifier;
	final private Map<String, TripRouter> tripRouters;

	public ModeAwareTripRouter(Map<String, TripRouter> tripRouters, MainModeIdentifier mainModeIdentifier) {
		this.mainModeIdentifier = mainModeIdentifier;
		this.tripRouters = tripRouters;
	}

	@Override
	public List<PlanElement> route(ActivityWithFacility originActivity, List<PlanElement> trip,
			ActivityWithFacility destinationActivity) {
		String mainMode = mainModeIdentifier.identifyMainMode(trip);
		return tripRouters.get(mainMode).route(originActivity, trip, destinationActivity);
	}
}
