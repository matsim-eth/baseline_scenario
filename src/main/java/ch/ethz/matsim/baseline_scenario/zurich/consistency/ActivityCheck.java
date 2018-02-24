package ch.ethz.matsim.baseline_scenario.zurich.consistency;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

public class ActivityCheck implements PlanConsistencyCheck {
	final private Network network;
	final private ActivityFacilities facilities;

	public ActivityCheck(Network network, ActivityFacilities facilities) {
		this.network = network;
		this.facilities = facilities;
	}

	@Override
	public void run(List<PlanElement> plan) {
		runSpatial(plan);
		runTemporal(plan);
	}

	public void runTemporal(List<PlanElement> plan) {
		double time = 0.0;

		for (int i = 0; i < plan.size(); i += 2) {
			Activity activity = (Activity) plan.get(i);

			if (activity.getType().equals("outside")) {
				// Because legs may take longer than expected, we cannot guarantee that they are
				// time-consistent, or we would need to modify activities further down the plan
				continue;
			}

			double startTime = activity.getStartTime();
			double endTime = activity.getEndTime();

			if (i > 0) {
				if (!Double.isFinite(startTime)) {
					throw new IllegalStateException("Start time of activity is not finite");
				}

				if (startTime < time) {
					throw new IllegalStateException("Start of activity is in the past");
				}

				time = startTime;
			}

			if (i < plan.size() - 1) {
				if (!Double.isFinite(endTime)) {
					throw new IllegalStateException("End time of activity is not finite");
				}

				if (endTime < time) {
					throw new IllegalStateException("End of activity is in the past");
				}

				time = endTime;
			}
		}
	}

	public void runSpatial(List<PlanElement> plan) {
		for (int i = 0; i < plan.size(); i += 2) {
			Activity activity = (Activity) plan.get(i);

			Coord coord = activity.getCoord();
			Id<Link> linkId = activity.getLinkId();
			Id<ActivityFacility> facilityId = activity.getFacilityId();

			if (coord == null) {
				throw new IllegalStateException("Every activity must have a coordinate");
			}

			if (linkId == null) {
				throw new IllegalStateException("Every activity must have a link ID");
			}

			if (facilityId == null) {
				throw new IllegalStateException("Every activity must have a facility ID");
			}

			Link link = network.getLinks().get(linkId);

			if (link == null) {
				throw new IllegalStateException("Every actiivty must have a existing link attached");
			}

			ActivityFacility facility = facilities.getFacilities().get(facilityId);

			if (facility == null) {
				throw new IllegalStateException("Every activity must have an existing facility");
			}

			if (!facility.getLinkId().equals(linkId)) {
				throw new IllegalStateException("Link ID and Facility ID must be consistent for every activity");
			}

			if (!facility.getCoord().equals(coord)) {
				throw new IllegalStateException("Facility coordinate and activity coordinate must be consistent");
			}

			if (!facility.getLinkId().equals(activity.getLinkId())) {
				throw new IllegalStateException("Activity must have the same link ID as its facility");
			}
		}
	}
}
