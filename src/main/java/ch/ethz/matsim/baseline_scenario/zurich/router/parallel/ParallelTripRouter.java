package ch.ethz.matsim.baseline_scenario.zurich.router.parallel;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.matsim.baseline_scenario.zurich.utils.ActivityWithFacility;

public interface ParallelTripRouter {
	CompletableFuture<List<PlanElement>> route(ActivityWithFacility originActivity, List<PlanElement> trip,
			ActivityWithFacility destinationActivity, Executor executor);
}
