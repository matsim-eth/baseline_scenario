package ch.ethz.matsim.baseline_scenario.zurich.cutter.population;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import org.matsim.api.core.v01.population.Population;

public interface ParallelPopulationCutter {
	void run(Population population, Executor executor) throws InterruptedException, ExecutionException;
}
