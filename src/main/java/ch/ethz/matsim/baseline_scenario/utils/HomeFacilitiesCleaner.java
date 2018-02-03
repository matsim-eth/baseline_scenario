package ch.ethz.matsim.baseline_scenario.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.households.Household;

public class HomeFacilitiesCleaner {
	final private Collection<Id<Household>> householdIds;

	public HomeFacilitiesCleaner(Collection<Id<Household>> householdIds) {
		this.householdIds = householdIds;
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

				if (!oneExists) {
					iterator.remove();
				}
			}
		}
	}
}
