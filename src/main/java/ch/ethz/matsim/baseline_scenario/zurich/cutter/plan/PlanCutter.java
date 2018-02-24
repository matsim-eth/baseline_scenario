package ch.ethz.matsim.baseline_scenario.zurich.cutter.plan;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;

import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.trips.TripProcessor;
import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

public class PlanCutter {
	final private ScenarioExtent extent;
	final private TripProcessor tripProcessor;
	final private StageActivityTypes stageActivityTypes;

	public PlanCutter(TripProcessor tripProcessor, ScenarioExtent extent, StageActivityTypes stageActivityTypes) {
		this.extent = extent;
		this.tripProcessor = tripProcessor;
		this.stageActivityTypes = stageActivityTypes;
	}

	private void addActivity(List<PlanElement> plan, Activity activity) {
		if (extent.isInside(activity.getCoord())) {
			plan.add(activity);
		} else {
			Activity virtualActivity = PopulationUtils.createActivityFromCoord("outside", activity.getCoord());
			virtualActivity.setEndTime(activity.getEndTime());
			virtualActivity.getAttributes().putAttribute("originalType", activity.getType());

			plan.add(virtualActivity);
		}
	}

	public List<PlanElement> processPlan(List<PlanElement> elements) {
		List<PlanElement> result = new LinkedList<>();

		if (elements.size() > 0) {
			addActivity(result, (Activity) elements.get(0));

			for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(elements, stageActivityTypes)) {
				result.addAll(tripProcessor.process(trip.getOriginActivity(), trip.getTripElements(),
						trip.getDestinationActivity()));
				addActivity(result, trip.getDestinationActivity());
			}
		}

		return result;
	}
}
