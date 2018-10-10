package ch.ethz.matsim.baseline_scenario.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

/**
 * TODO: Maybe fix this in a more sensible way than deleting them!
 */
public class RemoveInvalidPlans {

	private Collection<String> validFirstActivityTypes;
	private Collection<String> validLastActivityTypes;

	public RemoveInvalidPlans(Collection<String> validFirstActivityTypes, Collection<String> validLastActivityTypes) {
		this.validFirstActivityTypes = validFirstActivityTypes;
		this.validLastActivityTypes = validLastActivityTypes;
	}

	public void apply(Population population) {
		Iterator<? extends Person> personIterator = population.getPersons().values().iterator();
		
		while (personIterator.hasNext()) {
			Person person = personIterator.next();
			boolean remove = false;
			
			for (Plan plan : person.getPlans()) {
				List<String> activityTypes = new LinkedList<>();
				
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Activity) {
						Activity activity = (Activity) element;
						activityTypes.add(activity.getType());

						if (activity.getEndTime() - activity.getStartTime() <= 0.0) {
							remove = true;
						}
					}
				}

				for (String validFirstActivityType : validFirstActivityTypes) {
					if (!activityTypes.get(0).equals(validFirstActivityType)) {
						remove = true;
					}
				}

				for (String validLastActivityType : validLastActivityTypes) {
					if (!activityTypes.get(activityTypes.size() - 1).equals(validLastActivityType)) {
						remove = true;
					}
				}

			}
			
			if (remove) {
				personIterator.remove();
				population.getPersonAttributes().removeAllAttributes(person.getId().toString());
			}
		}
	}
}
