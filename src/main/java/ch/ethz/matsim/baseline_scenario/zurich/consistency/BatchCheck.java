package ch.ethz.matsim.baseline_scenario.zurich.consistency;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;

public class BatchCheck implements PlanConsistencyCheck {
	final private List<PlanConsistencyCheck> checks = new LinkedList<>();

	public BatchCheck(List<PlanConsistencyCheck> checks) {
		checks.addAll(checks);
	}

	public BatchCheck(PlanConsistencyCheck... checks) {
		for (PlanConsistencyCheck check : checks) {
			this.checks.add(check);
		}
	}

	@Override
	public void run(List<PlanElement> plan) {
		for (PlanConsistencyCheck check : checks) {
			check.run(plan);
		}
	}

}
