package ch.ethz.matsim.baseline_scenario.zurich.consistency;

import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;

public interface PlanConsistencyCheck {
	void run(List<PlanElement> plan);
}
