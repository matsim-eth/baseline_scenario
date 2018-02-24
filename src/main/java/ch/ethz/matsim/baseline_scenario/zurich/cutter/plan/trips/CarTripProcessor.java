package ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.trips;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;

import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.NetworkCrossingPoint;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.NetworkCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

public class CarTripProcessor implements TripProcessor {
	final private NetworkCrossingPointFinder crossingPointFinder;
	final private ScenarioExtent extent;

	public CarTripProcessor(NetworkCrossingPointFinder crossingPointFinder, ScenarioExtent extent) {
		this.crossingPointFinder = crossingPointFinder;
		this.extent = extent;
	}

	@Override
	public List<PlanElement> process(Activity firstActivity, List<PlanElement> trip, Activity secondActivity) {
		Leg leg = (Leg) trip.get(0);
		NetworkRoute route = (NetworkRoute) leg.getRoute();

		if (firstActivity.getType().equals("work_1") && secondActivity.getType().equals("home_1")) {
			if (route.getStartLinkId().toString().equals("234839")) {
				if (route.getEndLinkId().toString().equals("641520")) {
					System.err.println("HERE");
				}
			}
		}

		return process(route, leg.getDepartureTime(),
				!extent.isInside(firstActivity.getCoord()) && !extent.isInside(secondActivity.getCoord()));
	}

	public List<PlanElement> process(NetworkRoute route, double departureTime, boolean allOutside) {
		List<NetworkCrossingPoint> crossingPoints = crossingPointFinder.findCrossingPoints(route, departureTime);

		if (crossingPoints.size() == 0) {
			return Arrays.asList(PopulationUtils.createLeg(allOutside ? "outside" : "car"));
		} else {
			List<PlanElement> result = new LinkedList<>();

			result.add(PopulationUtils.createLeg(crossingPoints.get(0).isOutgoing ? "car" : "outside"));

			for (NetworkCrossingPoint point : crossingPoints) {
				Activity activity = PopulationUtils.createActivityFromLinkId("outside", point.link.getId());
				activity.setEndTime(point.leaveTime);
				result.add(activity);
				result.add(PopulationUtils.createLeg(point.isOutgoing ? "outside" : "car"));
			}

			return result;
		}
	}
}
