package ch.ethz.matsim.baseline_scenario.zurich.router.parallel;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.matsim.baseline_scenario.zurich.router.trip.TripRouter;
import ch.ethz.matsim.baseline_scenario.zurich.utils.ActivityWithFacility;

public class DefaultParallelTripRouter implements ParallelTripRouter {
	final private BlockingQueue<TripRouter> queue;

	public DefaultParallelTripRouter(List<TripRouter> instances) {
		this.queue = new ArrayBlockingQueue<>(instances.size());
		this.queue.addAll(instances);
	}

	private List<PlanElement> routeInParallel(ActivityWithFacility originActivity, List<PlanElement> trip,
			ActivityWithFacility destinationActivity) {
		try {
			TripRouter router = queue.take();
			List<PlanElement> result = router.route(originActivity, trip, destinationActivity);
			queue.put(router);
			return result;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public CompletableFuture<List<PlanElement>> route(ActivityWithFacility originActivity, List<PlanElement> trip,
			ActivityWithFacility destinationActivity, Executor executor) {
		return CompletableFuture.supplyAsync(() -> {
			return routeInParallel(originActivity, trip, destinationActivity);
		}, executor);
	}
}
