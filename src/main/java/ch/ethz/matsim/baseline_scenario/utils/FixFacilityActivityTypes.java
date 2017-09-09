package ch.ethz.matsim.baseline_scenario.utils;

import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTimeImpl;

public class FixFacilityActivityTypes {
	public void run(ActivityFacilities facilities) {
        addNewActivityTypes(facilities, "remote_work", "work", 5, new OpeningTimeImpl(0, 24*60*60));
        addNewActivityTypes(facilities, "escort_kids", "education", 10, new OpeningTimeImpl(7*60*60, 18*60*60));
        addNewActivityTypes(facilities, "escort_kids", "leisure", 5, new OpeningTimeImpl(7*60*60, 22*60*60));
        addNewActivityTypes(facilities, "escort_other", "leisure", 5, new OpeningTimeImpl(5*60*60, 24*60*60));
        addNewActivityTypes(facilities, "escort_other", "shop", 5, new OpeningTimeImpl(5*60*60, 24*60*60));
        addNewActivityTypes(facilities, "remote_home", "home", 1, new OpeningTimeImpl(0, 24*60*60));
	
        for (ActivityFacility facility : facilities.getFacilities().values()) {
    		if (facility.getActivityOptions().containsKey("home")) {
    			facility.getActivityOptions().get("home").getOpeningTimes().clear();
    		}
    		
    		if (facility.getActivityOptions().containsKey("remote_home")) {
    			facility.getActivityOptions().get("remote_home").getOpeningTimes().clear();
    		}
        }
	}

	private void addNewActivityTypes(ActivityFacilities facilities, String newActivityType,
			String referenceActivityType, double capacity, OpeningTime openingTime) {
		for (ActivityFacility facility : facilities.getFacilitiesForActivityType(referenceActivityType).values()) {
			if (!facility.getActivityOptions().containsKey(newActivityType)) {
				ActivityOption newOption = new ActivityOptionImpl(newActivityType);
				newOption.setCapacity(capacity);
				newOption.addOpeningTime(openingTime);
				facility.addActivityOption(newOption);
			}
		}
	}
}
