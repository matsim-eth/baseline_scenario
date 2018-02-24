package ch.ethz.matsim.baseline_scenario.zurich.router;

import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;

public interface PlanRouter {
	List<PlanElement> route(List<PlanElement> plan);
}
