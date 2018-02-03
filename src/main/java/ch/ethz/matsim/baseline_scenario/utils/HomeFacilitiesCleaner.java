package ch.ethz.matsim.baseline_scenario.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.households.Household;

public class HomeFacilitiesCleaner {
	final private Collection<Id<Household>> householdIds;
	final private Collection<Id<ActivityFacility>> usedFacilityIds = new HashSet<>();

	public HomeFacilitiesCleaner(Collection<Id<Household>> householdIds, Collection<? extends Person> persons) {
		this.householdIds = householdIds;

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
		Iterator<? extends ActivityFacility> iterator = facilities.getFacilities().values().iterator();

		while (iterator.hasNext()) {
			ActivityFacilityImpl facility = (ActivityFacilityImpl) iterator.next();
			String description = facility.getDesc();

			if (description != null && description.contains("Home for household(s)")) {
				description = description.replace("Home for household(s)", "");

				String[] rawIds = description.split(",");
				List<Id<Household>> ids = Arrays.asList(rawIds).stream().map(s -> Id.create(s.trim(), Household.class))
						.collect(Collectors.toList());

				boolean oneExists = false;

				for (Id<Household> householdId : ids) {
					if (householdIds.contains(householdId)) {
						oneExists = true;
						break;
					}
				}

				// A facility might still be used as a remote_home. This is not denoted in the
				// facility description.
				if (!oneExists && !usedFacilityIds.contains(facility.getId())) {
					iterator.remove();
				}
			}
		}
	}
}
