package ch.ethz.matsim.baseline_scenario.zurich.cutter.population;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;

import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.PlanCutter;

public class DefaultPopulationCutter implements PopulationCutter {
	final private Logger logger = Logger.getLogger(DefaultPopulationCutter.class);
	final private PlanCutter planCutter;

	public DefaultPopulationCutter(PlanCutter planCutter) {
		this.planCutter = planCutter;
	}

	private void progress(int numberOfPersons, int currentlyProcessed) {
		double percentage = 100.0 * currentlyProcessed / numberOfPersons;
		logger.info(
				String.format("Cutting population %d/%d (%.2f%%)", currentlyProcessed, numberOfPersons, percentage));
	}

	@Override
	public void run(Population population) {
		int numberOfPersons = population.getPersons().size();
		int currentlyProcessed = 0;

		for (Person person : population.getPersons().values()) {
			List<PlanElement> updatedPlan = planCutter.processPlan(person.getSelectedPlan().getPlanElements());
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

			progress(numberOfPersons, ++currentlyProcessed);
		}
	}
}
