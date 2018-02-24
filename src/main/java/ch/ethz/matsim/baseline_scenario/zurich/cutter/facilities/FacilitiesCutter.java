package ch.ethz.matsim.baseline_scenario.zurich.cutter.facilities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

public class FacilitiesCutter {
	final private static Logger log = Logger.getLogger(FacilitiesCutter.class);

	final private ScenarioExtent extent;
	final private Collection<Id<ActivityFacility>> usedFacilityIds = new HashSet<>();

	public FacilitiesCutter(ScenarioExtent extent, Collection<? extends Person> persons) {
		this.extent = extent;

		for (Person person : persons) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Activity) {
						Activity activity = (Activity) element;
						usedFacilityIds.add(activity.getFacilityId());
					}
				}
			}
		}
	}

	public void run(ActivityFacilities facilities) {
		log.info("Cutting facilities ...");
		int originalNumberOfFacilities = facilities.getFacilities().size();

		Iterator<? extends ActivityFacility> iterator = facilities.getFacilities().values().iterator();

		while (iterator.hasNext()) {
			ActivityFacility facility = iterator.next();

			if (!usedFacilityIds.contains(facility.getId()) && !extent.isInside(facility.getCoord())) {
				iterator.remove();
			}
		}

		int finalNumberOfFacilities = facilities.getFacilities().size();

		log.info("Number of facilities before: " + originalNumberOfFacilities);
		log.info("Number of facilities now: " + finalNumberOfFacilities);
	}
}
