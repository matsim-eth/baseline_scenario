package ch.ethz.matsim.baseline_scenario.zurich.router.parallel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.vehicles.Vehicle;

public interface ParallelLeastCostPathCalculator {
	CompletableFuture<Path> calcLeastCostPath(Node fromNode, Node toNode, double starttime, final Person person,
			final Vehicle vehicle, Executor executor);
}
