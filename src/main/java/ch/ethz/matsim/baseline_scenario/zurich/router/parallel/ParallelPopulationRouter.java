package ch.ethz.matsim.baseline_scenario.zurich.router.parallel;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import org.matsim.api.core.v01.population.Population;

public interface ParallelPopulationRouter {
	void run(Population population, Executor executor) throws InterruptedException, ExecutionException;
}
