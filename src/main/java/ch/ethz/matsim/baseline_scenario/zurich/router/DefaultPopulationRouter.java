package ch.ethz.matsim.baseline_scenario.zurich.router;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

public class DefaultPopulationRouter implements PopulationRouter {
	final private Logger logger = Logger.getLogger(DefaultPopulationRouter.class);
	final private PlanRouter planRouter;

	public DefaultPopulationRouter(PlanRouter planRouter) {
		this.planRouter = planRouter;
	}

	@Override
	public void run(Population population) {
		int numberOfPersons = population.getPersons().values().size();
		int currentlyProcessed = 0;

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				List<PlanElement> oldElements = new LinkedList<>(plan.getPlanElements());
				plan.getPlanElements().clear();
				plan.getPlanElements().addAll(planRouter.route(oldElements));
			}

			currentlyProcessed++;
			logger.info(String.format("Routing population %d/%d (%.2f%%)", currentlyProcessed, numberOfPersons,
					100.0 * currentlyProcessed / numberOfPersons));
		}
	}
}
