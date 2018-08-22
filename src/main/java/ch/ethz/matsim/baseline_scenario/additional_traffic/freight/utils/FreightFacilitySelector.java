package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils;

import ch.ethz.matsim.baseline_scenario.additional_traffic.AdditionalTrafficType;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.*;

import java.util.*;

public class FreightFacilitySelector {
    private final Map<Integer, Set<ActivityFacility>> zone2facilities;
    private final ActivityFacilitiesFactory activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();
    private final Random random;

    public FreightFacilitySelector(Map<Integer, Set<ActivityFacility>> zone2facilities, Random random) {
        this.zone2facilities = zone2facilities;
        this.random = random;
    }

    public ActivityFacility getFreightFacility(int zoneId) {
        Set<ActivityFacility> facilityList = zone2facilities.get(zoneId);
        Optional<ActivityFacility> facility = Optional.empty();
        while (!facility.isPresent()) {
            facility = facilityList.stream()
                    .skip(random.nextInt(facilityList.size() - 1))
                    .findFirst();
        }
        Id<ActivityFacility> facilityId = Id.create(AdditionalTrafficType.FREIGHT.toString()
                + "_" + (int)facility.get().getCoord().getX() + "_" + (int)facility.get().getCoord().getY(), ActivityFacility.class);
        ActivityFacility freightFacility = activityFacilitiesFactory.createActivityFacility(facilityId,
                facility.get().getCoord(), facility.get().getLinkId());

        // add freight activity to facility
        freightFacility.addActivityOption(new ActivityOptionImpl(AdditionalTrafficType.FREIGHT.toString()));
        OpeningTime openingTime = new OpeningTimeImpl(0.0 * 3600.0, 24.0 * 3600.0);
        freightFacility.getActivityOptions().get(AdditionalTrafficType.FREIGHT.toString()).addOpeningTime(openingTime);

        return freightFacility;
    }
}
