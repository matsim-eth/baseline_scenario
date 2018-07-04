package ch.ethz.matsim.baseline_scenario.zurich.cutter.population;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;

import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.PlanCutter;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.MergeOutsideActivities;

public class DefaultParallelPopulationCutter implements ParallelPopulationCutter {
	final private Logger logger = Logger.getLogger(DefaultParallelPopulationCutter.class);
	final private PlanCutter planCutter;
	final private MergeOutsideActivities mergeOutsideActivities;

	public DefaultParallelPopulationCutter(PlanCutter planCutter, MergeOutsideActivities mergeOutsideActivities) {
		this.planCutter = planCutter;
		this.mergeOutsideActivities = mergeOutsideActivities;
	}

	private void progress(int numberOfPersons, int currentlyProcessed) {
		double percentage = 100.0 * currentlyProcessed / numberOfPersons;
		logger.info(
				String.format("Cutting population %d/%d (%.2f%%)", currentlyProcessed, numberOfPersons, percentage));
	}

	public void run(Population population, Executor executor) throws InterruptedException, ExecutionException {
		int numberOfPersons = population.getPersons().size();
		AtomicInteger currentlyProcessed = new AtomicInteger(0);

		List<CompletableFuture<?>> futures = new LinkedList<>();

		for (Person person : population.getPersons().values()) {
			futures.add(CompletableFuture.runAsync(() -> {
				List<PlanElement> updatedPlan = planCutter.processPlan(person.getSelectedPlan().getPlanElements());
				mergeOutsideActivities.run(updatedPlan);

				person.removePlan(person.getSelectedPlan());

				Plan newPlan = PopulationUtils.createPlan();
				updatedPlan.forEach(element -> {
					if (element instanceof Activity) {
						newPlan.addActivity((Activity) element);
					} else {
						newPlan.addLeg((Leg) element);
					}
				});

				person.addPlan(newPlan);
				person.setSelectedPlan(newPlan);

				progress(numberOfPersons, currentlyProcessed.incrementAndGet());
			}, executor));
		}

		//CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()])).get();
	}
}
