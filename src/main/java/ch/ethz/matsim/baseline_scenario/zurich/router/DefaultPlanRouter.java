package ch.ethz.matsim.baseline_scenario.zurich.router;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import ch.ethz.matsim.baseline_scenario.zurich.router.trip.TripRouter;
import ch.ethz.matsim.baseline_scenario.zurich.utils.ActivityWithFacility;
import ch.ethz.matsim.baseline_scenario.zurich.utils.DefaultActivityWithFacility;

public class DefaultPlanRouter implements PlanRouter {
	final private StageActivityTypes stageActivityTypes;
	final private TripRouter tripRouter;
	final private ActivityFacilities activityFacilities;

	public DefaultPlanRouter(TripRouter tripRouter, StageActivityTypes stageActivityTypes,
			ActivityFacilities activityFacilities) {
		this.stageActivityTypes = stageActivityTypes;
		this.tripRouter = tripRouter;
		this.activityFacilities = activityFacilities;
	}

	@Override
	public List<PlanElement> route(List<PlanElement> plan) {
		List<PlanElement> result = new LinkedList<>();

		for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan, stageActivityTypes)) {
			if (result.size() == 0) {
				result.add(trip.getOriginActivity());
			}

			ActivityFacility originFacility = activityFacilities.getFacilities()
					.get(trip.getOriginActivity().getFacilityId());
			ActivityFacility destinationFacility = activityFacilities.getFacilities()
					.get(trip.getDestinationActivity().getFacilityId());

			ActivityWithFacility originActivity = new DefaultActivityWithFacility(trip.getOriginActivity(),
					originFacility);
			ActivityWithFacility destinationActivity = new DefaultActivityWithFacility(trip.getDestinationActivity(),
					destinationFacility);

			result.addAll(tripRouter.route(originActivity, trip.getTripElements(),
					(ActivityWithFacility) destinationActivity));
			result.add(trip.getDestinationActivity());
		}

		return result;
	}

}
