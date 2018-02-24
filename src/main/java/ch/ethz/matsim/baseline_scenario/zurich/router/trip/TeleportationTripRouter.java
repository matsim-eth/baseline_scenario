package ch.ethz.matsim.baseline_scenario.zurich.router.trip;

import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TeleportationRoutingModule;

import ch.ethz.matsim.baseline_scenario.zurich.utils.ActivityWithFacility;

public class TeleportationTripRouter implements TripRouter {
	final private TripRouterWithRoutingModule delegate;

	public TeleportationTripRouter(ModeRoutingParams modeParams) {
		delegate = new TripRouterWithRoutingModule(
				new TeleportationRoutingModule(modeParams.getMode(), PopulationUtils.getFactory(),
						modeParams.getTeleportedModeSpeed(), modeParams.getBeelineDistanceFactor()));
	}

	@Override
	public List<PlanElement> route(ActivityWithFacility originActivity, List<PlanElement> trip,
			ActivityWithFacility destinationActivity) {
		return delegate.route(originActivity, trip, destinationActivity);
	}
}
