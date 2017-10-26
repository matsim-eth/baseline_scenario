package ch.ethz.matsim.baseline_scenario.utils;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

public class UnselectedPlanRemoval {
	public void run(Population population) {
		for (Person person : population.getPersons().values()) {
			Set<Plan> remove = new HashSet<>(person.getPlans());
			remove.remove(person.getSelectedPlan());
			remove.forEach(p -> person.removePlan(p));
		}
	}
}
