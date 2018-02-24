package ch.ethz.matsim.baseline_scenario.zurich.router.parallel;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.matsim.baseline_scenario.zurich.router.DefaultPopulationRouter;

public class DefaultParallelPopulationRouter implements ParallelPopulationRouter {
	final private Logger logger = Logger.getLogger(DefaultPopulationRouter.class);
	final private ParallelPlanRouter planRouter;

	public DefaultParallelPopulationRouter(ParallelPlanRouter planRouter) {
		this.planRouter = planRouter;
	}

	private void progress(int numberOfPersons, int currentlyProcessed) {
		logger.info(String.format("Routing population %d/%d (%.2f%%)", currentlyProcessed, numberOfPersons,
				100.0 * currentlyProcessed / numberOfPersons));
	}

	@Override
	public void run(Population population, Executor executor) throws InterruptedException, ExecutionException {
		int numberOfPersons = population.getPersons().values().size();
		AtomicInteger currentlyProcessed = new AtomicInteger(0);

		List<Future<?>> futures = new LinkedList<>();

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				List<PlanElement> oldElements = new LinkedList<>(plan.getPlanElements());
				plan.getPlanElements().clear();

				futures.add(planRouter.route(oldElements, executor).thenAccept(newElements -> {
					plan.getPlanElements().addAll(newElements);
					progress(numberOfPersons, currentlyProcessed.incrementAndGet());
				}));
			}
		}
		
		CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()])).get();
	}
}
