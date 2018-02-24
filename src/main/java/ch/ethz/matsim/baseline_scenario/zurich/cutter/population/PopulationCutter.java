package ch.ethz.matsim.baseline_scenario.zurich.cutter.population;

import org.matsim.api.core.v01.population.Population;

public interface PopulationCutter {
	void run(Population population);
}
