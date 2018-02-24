package ch.ethz.matsim.baseline_scenario.zurich.router.parallel;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.matsim.api.core.v01.population.PlanElement;

public interface ParallelPlanRouter {
	CompletableFuture<List<PlanElement>> route(List<PlanElement> plan, Executor executor);
}
