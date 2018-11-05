package ch.ethz.matsim.baseline_scenario.location_assignment;

import java.util.Iterator;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;

public class ZurichPopulationCleaner {
	public void run(Population population, StageActivityTypes stageActivityTypes,
			MainModeIdentifier mainModeIdentifier) {
		Iterator<? extends Person> personIterator = population.getPersons().values().iterator();

		while (personIterator.hasNext()) {
			Person person = personIterator.next();
			Plan plan = person.getSelectedPlan();

			for (Activity activity : TripStructureUtils.getActivities(plan, stageActivityTypes)) {
				activity.setType(activity.getType().replaceAll("_([0-9]+)$", ""));
			}

			new TripsToLegsAlgorithm(stageActivityTypes, mainModeIdentifier).run(plan);

			if (TripStructureUtils.getLegs(plan).stream().map(Leg::getMode).filter(m -> m.equals("outside"))
					.count() > 0) {
				personIterator.remove();
				continue;
			}

			if (TripStructureUtils.getActivities(plan, stageActivityTypes).stream().map(Activity::getType)
					.filter(t -> t.equals("outside")).count() > 0) {
				personIterator.remove();
				continue;
			}
		}
	}
}
