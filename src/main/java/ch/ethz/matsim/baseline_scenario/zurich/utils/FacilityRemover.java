package ch.ethz.matsim.baseline_scenario.zurich.utils;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

public class FacilityRemover {
	final private Population population;
	final private ScenarioExtent extent;
	
	public FacilityRemover(Population population, ScenarioExtent extent) {
		this.population = population;
		this.extent = extent;
	}
	
	public void run(ActivityFacilities facilities) {
		Set<Id<ActivityFacility>> outside = new HashSet<>();
		Set<Id<ActivityFacility>> unused = new HashSet<>(facilities.getFacilities().keySet());
		
		for (ActivityFacility facility : facilities.getFacilities().values()) {
			if (!extent.isInside(facility.getCoord())) {
				outside.add(facility.getId());
			}
		}
		
		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Activity) {
						Activity activity = (Activity) element;
						unused.remove(activity.getFacilityId());
					}
				}
			}
		}
		
		outside.retainAll(unused);
		facilities.getFacilities().keySet().removeAll(outside);		
		outside.forEach(id -> facilities.getFacilityAttributes().removeAllAttributes(id.toString()));
	}
}
