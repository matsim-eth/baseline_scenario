package ch.ethz.matsim.baseline_scenario.zurich.consistency;

import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

public class ChainStructureCheck implements PlanConsistencyCheck {
	final private ScenarioExtent extent;
	final private Network network;

	public ChainStructureCheck(ScenarioExtent extent, Network network) {
		this.extent = extent;
		this.network = network;
	}

	@Override
	public void run(List<PlanElement> plan) {
		if (!(plan.get(0) instanceof Activity)) {
			throw new IllegalStateException();
		}

		if (!(plan.get(plan.size() - 1) instanceof Activity)) {
			throw new IllegalStateException();
		}

		for (int i = 0; i < plan.size(); i++) {
			if (i % 2 == 0 && !(plan.get(i) instanceof Activity)) {
				throw new IllegalStateException();
			}

			if (i % 2 != 0 && !(plan.get(i) instanceof Leg)) {
				throw new IllegalStateException();
			}
		}
	}
}
