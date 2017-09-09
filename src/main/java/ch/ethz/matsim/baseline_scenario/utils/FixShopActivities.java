package ch.ethz.matsim.baseline_scenario.utils;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

public class FixShopActivities {
	public void apply(Population population) {
		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Activity) {
						Activity activity = (Activity) planElement;
						
						if (activity.getType().equals("shopping")) {
							activity.setType("shop");
						}
					}
				}
			}
		}
	}
}
