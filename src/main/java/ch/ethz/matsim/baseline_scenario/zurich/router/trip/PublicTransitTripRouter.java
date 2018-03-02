package ch.ethz.matsim.baseline_scenario.zurich.router.trip;

import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import ch.ethz.matsim.baseline_scenario.transit.routing.BaselineTransitRoutingModule;
import ch.ethz.matsim.baseline_scenario.transit.routing.EnrichedTransitRouter;
import ch.ethz.matsim.baseline_scenario.zurich.utils.ActivityWithFacility;

public class PublicTransitTripRouter implements TripRouter {
	final private TripRouterWithRoutingModule delegate;

	public PublicTransitTripRouter(Network network, TransitSchedule transitSchedule,
			EnrichedTransitRouter transitRouter, ModeRoutingParams walkParams) {
		this.delegate = new TripRouterWithRoutingModule(
				new BaselineTransitRoutingModule(transitRouter, transitSchedule));
	}

	@Override
	public List<PlanElement> route(ActivityWithFacility originActivity, List<PlanElement> trip,
			ActivityWithFacility destinationActivity) {
		return delegate.route(originActivity, trip, destinationActivity);
	}

}
