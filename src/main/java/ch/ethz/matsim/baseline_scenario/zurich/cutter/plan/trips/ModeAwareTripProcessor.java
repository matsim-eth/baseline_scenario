package ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.trips;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;

public class ModeAwareTripProcessor implements TripProcessor {
	final private MainModeIdentifier mainModeIdentifier;
	final private Map<String, TripProcessor> processors;

	public ModeAwareTripProcessor(MainModeIdentifier mainModeIdentifier, Map<String, TripProcessor> processors) {
		this.mainModeIdentifier = mainModeIdentifier;
		this.processors = processors;
	}

	@Override
	public List<PlanElement> process(Activity firstActivity, List<PlanElement> trip, Activity secondActivity) {
		String mainMode = mainModeIdentifier.identifyMainMode(trip);
		return processors.get(mainMode).process(firstActivity, trip, secondActivity);
	}
}
