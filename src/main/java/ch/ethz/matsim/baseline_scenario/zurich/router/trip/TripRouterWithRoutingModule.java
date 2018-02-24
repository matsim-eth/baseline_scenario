package ch.ethz.matsim.baseline_scenario.zurich.router.trip;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.RoutingModule;

import ch.ethz.matsim.baseline_scenario.zurich.utils.ActivityWithFacility;

public class TripRouterWithRoutingModule implements TripRouter {
	final protected RoutingModule routingModule;

	public TripRouterWithRoutingModule(RoutingModule routingModule) {
		this.routingModule = routingModule;
	}

	@Override
	public List<PlanElement> route(ActivityWithFacility originActivity, List<PlanElement> trip,
			ActivityWithFacility destinationActivity) {
		List<PlanElement> result = new LinkedList<>();
		result.addAll(routingModule.calcRoute(originActivity.getFacility(), destinationActivity.getFacility(),
				originActivity.getEndTime(), null));
		return result;
	}
}
