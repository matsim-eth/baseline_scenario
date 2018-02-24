package ch.ethz.matsim.baseline_scenario.zurich.router.trip;

import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.matsim.baseline_scenario.zurich.utils.ActivityWithFacility;

public interface TripRouter {
	List<PlanElement> route(ActivityWithFacility originActivity, List<PlanElement> trip,
			ActivityWithFacility destinationActivity);
}
