package ch.ethz.matsim.baseline_scenario.zurich.router.parallel;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.vehicles.Vehicle;

public class DefaultParallelLeastCostPathCalculator implements ParallelLeastCostPathCalculator {
	final private BlockingQueue<LeastCostPathCalculator> calculators = new LinkedBlockingQueue<>();
	final private ExecutorService executor;

	public DefaultParallelLeastCostPathCalculator(ExecutorService executor,
			Collection<LeastCostPathCalculator> calculators) {
		this.calculators.addAll(calculators);
		this.executor = executor;
	}

	@Override
	public CompletableFuture<Path> calcLeastCostPath(Node fromNode, Node toNode, double starttime, Person person,
			Vehicle vehicle, Executor executor) {

		return CompletableFuture.supplyAsync(() -> {
			try {
				LeastCostPathCalculator calculator = calculators.take();
				Path path = calculator.calcLeastCostPath(fromNode, toNode, starttime, person, vehicle);
				calculators.put(calculator);
				return path;
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}
}
